package org.gitee.orryx.core.editor

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 编辑器 WebSocket 客户端。
 *
 * Socket 已连接与服务端已注册是两个独立状态。每次连接尝试分配 generation，旧连接的回调不会再
 * 修改新连接状态；配置热重载时 license 变化会立即切换连接，普通断线则延迟重连。
 */
object EditorClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val stateLock = Any()
    private val generation = AtomicLong()
    private val connected = AtomicBoolean(false)
    private val registered = AtomicBoolean(false)
    private val reconnectScheduled = AtomicBoolean(false)
    private val stopping = AtomicBoolean(false)
    private val registrationRejected = AtomicBoolean(false)
    private val logSubscribed = AtomicBoolean(false)
    private val pendingTokenRegistrations = ConcurrentHashMap<String, CompletableFuture<Boolean>>()

    @Volatile
    private var client: WebSocketClient? = null

    @Volatile
    private var activeLicense: String? = null

    @Volatile
    private var reconnectJob: Job? = null

    @Volatile
    private var logKeywordFilter: String? = null

    private const val RECONNECT_INTERVAL = 30_000L
    private const val SERVER_URL = "wss://orryx.mcwar.cn/ws/server"
    private const val EDITOR_URL = "https://orryx.mcwar.cn"
    private const val TOKEN_EXPIRES_SECONDS = 300

    internal fun getEditorUrl(): String = EDITOR_URL
    internal fun getTokenExpires(): Int = TOKEN_EXPIRES_SECONDS

    private fun isEnabled(): Boolean = Orryx.config.getBoolean("Editor.Enable", false)

    private fun getLicense(): String? {
        return Orryx.config.getString("Editor.License")?.trim()?.takeIf { it.isNotEmpty() }
    }

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
        refreshConnection()
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        disconnect()
    }

    @Reload(99)
    private fun onReload() {
        refreshConnection()
    }

    private fun refreshConnection() {
        val license = getLicense().takeIf { isEnabled() }
        if (license == null) {
            disconnect()
            return
        }
        val currentClient = client
        if (activeLicense == license && currentClient != null && !registrationRejected.get()) return
        startConnection(license, reconnect = activeLicense == license)
    }

    private fun startConnection(license: String, reconnect: Boolean) {
        val attemptGeneration: Long
        val nextClient: WebSocketClient
        val previousClient: WebSocketClient?
        synchronized(stateLock) {
            stopping.set(false)
            registrationRejected.set(false)
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectScheduled.set(false)
            connected.set(false)
            registered.set(false)
            logSubscribed.set(false)
            logKeywordFilter = null
            pendingTokenRegistrations.values.forEach { it.complete(false) }
            pendingTokenRegistrations.clear()

            attemptGeneration = generation.incrementAndGet()
            activeLicense = license
            previousClient = client
            nextClient = createClient(SERVER_URL, license, attemptGeneration, reconnect)
            client = nextClient
        }
        closeQuietly(previousClient)
        try {
            nextClient.connect()
        } catch (e: Exception) {
            if (isCurrent(attemptGeneration, nextClient)) {
                synchronized(stateLock) {
                    if (client === nextClient) client = null
                }
                consoleMessage("&c[Editor] 连接中心服务器失败: ${sanitizeLogMessage(e.message)}")
                scheduleReconnect(attemptGeneration)
            }
        }
    }

    private fun disconnect() {
        val previousClient: WebSocketClient?
        synchronized(stateLock) {
            stopping.set(true)
            generation.incrementAndGet()
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectScheduled.set(false)
            connected.set(false)
            registered.set(false)
            logSubscribed.set(false)
            logKeywordFilter = null
            activeLicense = null
            pendingTokenRegistrations.values.forEach { it.complete(false) }
            pendingTokenRegistrations.clear()
            previousClient = client
            client = null
        }
        closeQuietly(previousClient)
    }

    private fun scheduleReconnect(expectedGeneration: Long) {
        val license = activeLicense ?: return
        if (stopping.get() || registrationRejected.get() || connected.get() || generation.get() != expectedGeneration) return
        if (!reconnectScheduled.compareAndSet(false, true)) return
        consoleMessage("&e[Editor] 将在 ${RECONNECT_INTERVAL / 1000}s 后尝试重连...")
        val job = OrryxAPI.ioScope.launch {
            delay(RECONNECT_INTERVAL)
            reconnectScheduled.set(false)
            synchronized(stateLock) {
                reconnectJob = null
            }
            if (stopping.get() || connected.get()) return@launch

            val configuredLicense = getLicense().takeIf { isEnabled() }
            if (configuredLicense == null) {
                disconnect()
                return@launch
            }
            startConnection(configuredLicense, reconnect = true)
        }
        synchronized(stateLock) {
            if (reconnectScheduled.get() && activeLicense == license && generation.get() == expectedGeneration) {
                reconnectJob = job
            } else {
                job.cancel()
            }
        }
    }

    private fun createClient(
        url: String,
        license: String,
        attemptGeneration: Long,
        reconnect: Boolean,
    ): WebSocketClient {
        return object : WebSocketClient(URI(url)) {
            override fun onOpen(handshake: ServerHandshake) {
                if (!isCurrent(attemptGeneration, this)) {
                    closeQuietly(this)
                    return
                }
                connected.set(true)
                registered.set(false)
                reconnectScheduled.set(false)
                if (reconnect) {
                    consoleMessage("&e┣&7[Editor] 重连成功，正在注册服务器")
                } else {
                    consoleMessage("&e┣&7[Editor] 已连接中心服务器，正在注册服务器")
                }
                sendMessageForGeneration(attemptGeneration, "server.register", "reg_init", buildJsonObject {
                    put("license", license)
                    put("serverName", getServerName())
                })
            }

            override fun onMessage(message: String) {
                if (!isCurrent(attemptGeneration, this)) return
                try {
                    val msg = json.parseToJsonElement(message).jsonObject
                    val type = msg["type"]?.jsonPrimitive?.contentOrNull ?: return
                    val id = msg["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val data = msg["data"] as? JsonObject

                    debug { "&e┣&7[Editor] 收到消息: type=$type, id=$id" }
                    if (type != "server.register.result" && !registered.get()) {
                        sendError(id, "Editor 服务器尚未完成注册")
                        return
                    }
                    when (type) {
                        "server.register.result" -> {
                            if (id == "reg_init" && !registered.get() && !registrationRejected.get()) {
                                handleRegisterResult(data)
                            }
                        }
                        "token.register.result" -> {
                            val success = data?.get("success")?.jsonPrimitive?.booleanOrNull ?: false
                            pendingTokenRegistrations.remove(id)?.complete(success)
                            debug { "&e┣&7[Editor] Token 注册结果: $success" }
                        }
                        "file.list" -> FileHandler.handleList(id, data)
                        "file.read" -> FileHandler.handleRead(id, data)
                        "file.write" -> FileHandler.handleWrite(id, data)
                        "file.create" -> FileHandler.handleCreate(id, data)
                        "file.delete" -> FileHandler.handleDelete(id, data)
                        "file.rename" -> FileHandler.handleRename(id, data)
                        "reload" -> ReloadHandler.handle(id, data)
                        "actions.schema" -> sendMessage("actions.schema.result", id, ActionsSchemaGenerator.generateSchema())
                        "log.subscribe" -> {
                            logSubscribed.set(true)
                            logKeywordFilter = (data?.get("filters") as? JsonObject)
                                ?.get("keyword")?.jsonPrimitive?.contentOrNull
                            debug { "&e┣&7[Editor] 日志订阅已开启" }
                        }
                        "log.unsubscribe" -> {
                            logSubscribed.set(false)
                            logKeywordFilter = null
                            debug { "&e┣&7[Editor] 日志订阅已关闭" }
                        }
                        "auth" -> {
                            val token = data?.get("token")?.jsonPrimitive?.contentOrNull
                            debug { "&e┣&7[Editor] 浏览器已认证, token=${redactToken(token)}" }
                        }
                        else -> debug { "&e┣&7[Editor] 未知消息类型: $type" }
                    }
                } catch (e: Exception) {
                    consoleMessage("&c[Editor] 消息处理异常: ${sanitizeLogMessage(e.message)}")
                }
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                if (!isCurrent(attemptGeneration, this)) return
                synchronized(stateLock) {
                    if (client === this) client = null
                    connected.set(false)
                    registered.set(false)
                    logSubscribed.set(false)
                    logKeywordFilter = null
                    pendingTokenRegistrations.values.forEach { it.complete(false) }
                    pendingTokenRegistrations.clear()
                }
                if (!stopping.get()) {
                    consoleMessage("&e[Editor] 连接断开: ${sanitizeLogMessage(reason)}")
                    scheduleReconnect(attemptGeneration)
                } else {
                    debug { "&e┣&7[Editor] 连接已关闭" }
                }
            }

            override fun onError(ex: Exception) {
                if (!isCurrent(attemptGeneration, this)) return
                consoleMessage("&c[Editor] WebSocket 错误: ${sanitizeLogMessage(ex.message)}")
            }

            private fun handleRegisterResult(data: JsonObject?) {
                val success = data?.get("success")?.jsonPrimitive?.booleanOrNull ?: false
                val resultMessage = data?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
                registered.set(success)
                if (success) {
                    registrationRejected.set(false)
                    consoleMessage("&e┣&7[Editor] 服务器注册成功: &a${sanitizeLogMessage(resultMessage)}")
                } else {
                    registrationRejected.set(true)
                    consoleMessage("&c[Editor] 服务器注册失败，已停止自动重试: ${sanitizeLogMessage(resultMessage)}")
                    close()
                }
            }
        }
    }

    /** 发送消息到当前连接；返回 false 表示没有可用连接或发送失败。 */
    fun sendMessage(type: String, id: String, data: JsonObject): Boolean {
        return sendMessageForGeneration(generation.get(), type, id, data)
    }

    private fun sendMessageForGeneration(
        expectedGeneration: Long,
        type: String,
        id: String,
        data: JsonObject,
    ): Boolean {
        val target = client ?: return false
        if (!connected.get() || generation.get() != expectedGeneration) return false
        val msg = buildJsonObject {
            put("type", type)
            put("id", id)
            put("data", data)
        }
        return try {
            target.send(json.encodeToString(JsonObject.serializer(), msg))
            true
        } catch (e: Exception) {
            consoleMessage("&c[Editor] 发送消息失败: ${sanitizeLogMessage(e.message)}")
            false
        }
    }

    fun sendError(id: String, message: String) {
        sendMessage("error", id, buildJsonObject { put("message", message) })
    }

    fun pushLog(level: String, message: String, source: String? = null) {
        if (!registered.get() || !logSubscribed.get()) return
        val keyword = logKeywordFilter
        if (keyword != null && !message.contains(keyword, ignoreCase = true)) return
        sendMessage("log.entry", "", buildJsonObject {
            put("level", level)
            put("message", sanitizeLogMessage(message))
            put("timestamp", System.currentTimeMillis())
            source?.let { put("source", it) }
        })
    }

    fun registerToken(token: String, playerName: String): CompletableFuture<Boolean> {
        if (!registered.get()) return CompletableFuture.completedFuture(false)
        val id = "tok_${generation.get()}_${System.nanoTime()}"
        val future = CompletableFuture<Boolean>()
        pendingTokenRegistrations[id] = future
        val timeoutTask = SimpleTimeoutTask.createSimpleTask(100L) {
            if (pendingTokenRegistrations.remove(id, future)) future.complete(false)
        }
        future.whenComplete { _, _ ->
            SimpleTimeoutTask.cancel(timeoutTask, false)
            pendingTokenRegistrations.remove(id, future)
        }
        val sent = sendMessage("token.register", id, buildJsonObject {
            put("token", token)
            put("playerName", playerName)
            put("expiresIn", TOKEN_EXPIRES_SECONDS * 1000L)
        })
        if (!sent && pendingTokenRegistrations.remove(id, future)) future.complete(false)
        return future
    }

    fun isConnected(): Boolean = connected.get()

    fun isRegistered(): Boolean = registered.get()

    internal fun currentGeneration(): Long = generation.get()

    internal fun redactToken(token: String?): String {
        if (token.isNullOrEmpty()) return "<missing>"
        if (token.length <= 8) return "***"
        return "${token.take(4)}***${token.takeLast(4)}"
    }

    internal fun sanitizeLogMessage(message: String?): String {
        var sanitized = message.orEmpty()
        activeLicense?.takeIf { it.isNotEmpty() }?.let { sanitized = sanitized.replace(it, "***") }
        sanitized = URL_TOKEN_REGEX.replace(sanitized) { "${it.groupValues[1]}***" }
        sanitized = JSON_SECRET_REGEX.replace(sanitized) { "${it.groupValues[1]}${it.groupValues[2]}***" }
        sanitized = BEARER_REGEX.replace(sanitized, "Bearer ***")
        return sanitized
    }

    private fun isCurrent(expectedGeneration: Long, expectedClient: WebSocketClient): Boolean {
        return generation.get() == expectedGeneration && client === expectedClient
    }

    private fun closeQuietly(target: WebSocketClient?) {
        if (target == null) return
        try {
            target.close()
        } catch (_: Exception) {
        }
    }

    private val URL_TOKEN_REGEX = Regex("(?i)([?&]token=)[^&\\s]+")
    private val JSON_SECRET_REGEX = Regex("(?i)([\\\"']?(?:token|license)[\\\"']?)(\\s*[:=]\\s*[\\\"']?)[^\\s,\\\"'}]+")
    private val BEARER_REGEX = Regex("(?i)Bearer\\s+[^\\s,]+")
}
