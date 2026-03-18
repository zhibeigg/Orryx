package org.gitee.orryx.core.editor

import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.editor.handler.FileHandler
import org.gitee.orryx.core.editor.handler.ReloadHandler
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.module.wiki.ActionsSchemaGenerator
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.debug
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 编辑器 WebSocket 客户端
 * 连接中心服务器，接收浏览器转发的消息并响应
 *
 * license 存放在 plugins/Orryx/license.key
 * 存在 license.key 即视为启用编辑器
 */
object EditorClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var client: WebSocketClient? = null
    private val connected = AtomicBoolean(false)
    private val reconnecting = AtomicBoolean(false)
    private val closing = AtomicBoolean(false)
    private val logSubscribed = AtomicBoolean(false)

    @Volatile
    private var logKeywordFilter: String? = null

    private const val RECONNECT_INTERVAL = 30000L

    private const val SERVER_URL = "wss://orryx.mcwar.cn/ws/server"
    private const val EDITOR_URL = "https://orryx.mcwar.cn"
    private const val TOKEN_EXPIRES_SECONDS = 300

    internal fun getEditorUrl(): String = EDITOR_URL
    internal fun getTokenExpires(): Int = TOKEN_EXPIRES_SECONDS

    private fun isEnabled(): Boolean = Orryx.config.getBoolean("Editor.Enable", false)

    /**
     * 从 config.yml 读取 license
     * @return license 字符串，未配置或为空返回 null
     */
    private fun getLicense(): String? {
        return Orryx.config.getString("Editor.License")?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * 从 server.properties 读取属性
     */
    private fun readServerProperties(key: String): String? {
        return try {
            val file = File("server.properties")
            if (!file.exists()) return null
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.firstOrNull { it.startsWith("$key=") }?.substringAfter("=")?.trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getServerName(): String {
        return readServerProperties("server-name")
            ?: readServerProperties("motd")
            ?: "Minecraft Server"
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        if (!isEnabled()) return
        val license = getLicense() ?: return
        connect(license)
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        disconnect()
    }

    @Reload(99)
    private fun onReload() {
        val enabled = isEnabled()
        val license = getLicense()
        if (enabled && license != null && !connected.get()) {
            connect(license)
        } else if ((!enabled || license == null) && connected.get()) {
            disconnect()
        }
    }

    private fun connect(license: String) {
        if (connected.get()) return
        closing.set(false)
        try {
            client = createClient(SERVER_URL, license)
            client?.connect()
        } catch (e: Exception) {
            consoleMessage("&c[Editor] 连接中心服务器失败: ${e.message}")
            scheduleReconnect()
        }
    }

    private fun disconnect() {
        closing.set(true)
        reconnecting.set(false)
        logSubscribed.set(false)
        logKeywordFilter = null
        try {
            client?.close()
        } catch (_: Exception) {
        }
        client = null
        connected.set(false)
    }

    private fun scheduleReconnect() {
        if (reconnecting.get()) return
        reconnecting.set(true)
        consoleMessage("&e[Editor] 将在 ${RECONNECT_INTERVAL / 1000}s 后尝试重连...")
        OrryxAPI.ioScope.launch {
            Thread.sleep(RECONNECT_INTERVAL)
            reconnecting.set(false)
            if (connected.get() || !isEnabled()) return@launch
            val license = getLicense()
            if (license == null) {
                consoleMessage("&c[Editor] License 未配置，停止重连")
                return@launch
            }
            try {
                client = createClient(SERVER_URL, license)
                client?.connect()
            } catch (e: Exception) {
                consoleMessage("&c[Editor] 重连失败: ${e.message}")
                scheduleReconnect()
            }
        }
    }

    private fun createClient(url: String, license: String): WebSocketClient {
        return object : WebSocketClient(URI(url)) {
            override fun onOpen(handshake: ServerHandshake) {
                connected.set(true)
                reconnecting.set(false)
                consoleMessage("&e┣&7[Editor] 已连接中心服务器")
                sendMessage("server.register", "reg_init", buildJsonObject {
                    put("license", license)
                    put("serverName", getServerName())
                })
            }

            override fun onMessage(message: String) {
                try {
                    val msg = json.decodeFromJsonElement<JsonObject>(json.parseToJsonElement(message))
                    val type = msg["type"]?.jsonPrimitive?.content ?: return
                    val id = msg["id"]?.jsonPrimitive?.content ?: ""
                    val data = msg["data"]?.jsonObject

                    debug { "&e┣&7[Editor] 收到消息: type=$type, id=$id" }

                    when (type) {
                        "server.register.result" -> {
                            val success = data?.get("success")?.jsonPrimitive?.boolean ?: false
                            val resultMsg = data?.get("message")?.jsonPrimitive?.content ?: ""
                            if (success) {
                                consoleMessage("&e┣&7[Editor] 服务器注册成功: &a$resultMsg")
                            } else {
                                consoleMessage("&c[Editor] 服务器注册失败: $resultMsg")
                            }
                        }
                        "token.register.result" -> {
                            debug { "&e┣&7[Editor] Token 注册结果: ${data?.get("success")}" }
                        }
                        "file.list" -> FileHandler.handleList(id, data)
                        "file.read" -> FileHandler.handleRead(id, data)
                        "file.write" -> FileHandler.handleWrite(id, data)
                        "file.create" -> FileHandler.handleCreate(id, data)
                        "file.delete" -> FileHandler.handleDelete(id, data)
                        "file.rename" -> FileHandler.handleRename(id, data)
                        "reload" -> ReloadHandler.handle(id, data)
                        "actions.schema" -> {
                            sendMessage("actions.schema.result", id, ActionsSchemaGenerator.generateSchema())
                        }
                        "log.subscribe" -> {
                            logSubscribed.set(true)
                            logKeywordFilter = data?.get("filters")?.jsonObject?.get("keyword")?.jsonPrimitive?.content
                            debug { "&e┣&7[Editor] 日志订阅已开启" }
                        }
                        "log.unsubscribe" -> {
                            logSubscribed.set(false)
                            logKeywordFilter = null
                            debug { "&e┣&7[Editor] 日志订阅已关闭" }
                        }
                        "auth" -> {
                            val token = data?.get("token")?.jsonPrimitive?.content
                            debug { "&e┣&7[Editor] 浏览器已认证, token=$token" }
                        }
                        else -> debug { "&e┣&7[Editor] 未知消息类型: $type" }
                    }
                } catch (e: Exception) {
                    consoleMessage("&c[Editor] 消息处理异常: ${e.message}")
                }
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                connected.set(false)
                logSubscribed.set(false)
                logKeywordFilter = null
                if (!closing.get()) {
                    consoleMessage("&e[Editor] 连接断开: $reason")
                    scheduleReconnect()
                } else {
                    debug { "&e┣&7[Editor] 连接已关闭" }
                }
            }

            override fun onError(ex: Exception) {
                consoleMessage("&c[Editor] WebSocket 错误: ${ex.message}")
            }
        }
    }

    /**
     * 发送消息到中心服务器
     */
    fun sendMessage(type: String, id: String, data: JsonObject) {
        if (!connected.get()) return
        val msg = buildJsonObject {
            put("type", type)
            put("id", id)
            put("data", data)
        }
        try {
            client?.send(json.encodeToString(JsonObject.serializer(), msg))
        } catch (e: Exception) {
            consoleMessage("&c[Editor] 发送消息失败: ${e.message}")
        }
    }

    /**
     * 发送错误响应
     */
    fun sendError(id: String, message: String) {
        sendMessage("error", id, buildJsonObject {
            put("message", message)
        })
    }

    /**
     * 推送日志条目
     */
    fun pushLog(level: String, message: String, source: String? = null) {
        if (!logSubscribed.get()) return
        val keyword = logKeywordFilter
        if (keyword != null && !message.contains(keyword, ignoreCase = true)) return
        sendMessage("log.entry", "", buildJsonObject {
            put("level", level)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
            source?.let { put("source", it) }
        })
    }

    /**
     * 注册 Token 到中心服务器
     */
    fun registerToken(token: String, playerName: String) {
        sendMessage("token.register", "tok_${System.currentTimeMillis()}", buildJsonObject {
            put("token", token)
            put("playerName", playerName)
            put("expiresIn", TOKEN_EXPIRES_SECONDS * 1000L)
        })
    }

    fun isConnected(): Boolean = connected.get()
}
