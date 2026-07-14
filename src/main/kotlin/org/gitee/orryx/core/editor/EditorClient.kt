package org.gitee.orryx.core.editor

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.editor.handler.FileHandler
import org.gitee.orryx.core.editor.handler.ReloadHandler
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.debug
import org.java_websocket.WebSocketImpl
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.enums.Opcode
import org.java_websocket.exceptions.LimitExceededException
import org.java_websocket.extensions.DefaultExtension
import org.java_websocket.framing.Framedata
import org.java_websocket.handshake.ServerHandshake
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.pluginVersion
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private class BoundedEditorDraft(
    private val maxMessageBytes: Int,
    private val maxFragmentCount: Int,
) : Draft_6455(listOf(DefaultExtension()), maxMessageBytes) {

    private var fragmentedMessageBytes = 0L
    private var fragmentedFrameCount = 0
    private var fragmentedMessageOpen = false

    @Synchronized
    override fun processFrame(webSocketImpl: WebSocketImpl, frame: Framedata) {
        val opcode = frame.opcode
        val startsFragmentedMessage = !fragmentedMessageOpen && !frame.isFin &&
            (opcode == Opcode.TEXT || opcode == Opcode.BINARY)
        val continuesFragmentedMessage = fragmentedMessageOpen && opcode == Opcode.CONTINUOUS
        val finishesFragmentedMessage = continuesFragmentedMessage && frame.isFin

        try {
            when {
                startsFragmentedMessage -> {
                    fragmentedMessageOpen = true
                    fragmentedMessageBytes = frame.payloadData.remaining().toLong()
                    fragmentedFrameCount = 1
                    checkFragmentedMessageLimit()
                }
                continuesFragmentedMessage -> {
                    val payloadBytes = frame.payloadData.remaining().toLong()
                    if (payloadBytes > maxMessageBytes.toLong() - fragmentedMessageBytes) {
                        throw messageLimitExceeded()
                    }
                    fragmentedMessageBytes += payloadBytes
                    fragmentedFrameCount++
                    checkFragmentedMessageLimit()
                }
            }
            super.processFrame(webSocketImpl, frame)
        } catch (throwable: Throwable) {
            resetFragmentedMessage()
            throw throwable
        } finally {
            if (finishesFragmentedMessage) resetFragmentedMessage()
        }
    }

    @Synchronized
    override fun reset() {
        super.reset()
        resetFragmentedMessage()
    }

    override fun copyInstance(): Draft = BoundedEditorDraft(maxMessageBytes, maxFragmentCount)

    private fun checkFragmentedMessageLimit() {
        if (fragmentedMessageBytes > maxMessageBytes || fragmentedFrameCount > maxFragmentCount) {
            throw messageLimitExceeded()
        }
    }

    private fun messageLimitExceeded(): LimitExceededException {
        return LimitExceededException("Editor WebSocket 消息超过大小或分片数量限制", maxMessageBytes)
    }

    private fun resetFragmentedMessage() {
        fragmentedMessageBytes = 0L
        fragmentedFrameCount = 0
        fragmentedMessageOpen = false
    }
}

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
    private val preparationGeneration = AtomicLong()
    private val connected = AtomicBoolean(false)
    private val registered = AtomicBoolean(false)
    private val negotiatedProtocol = AtomicReference(EditorProtocol.PROTOCOL_V1)
    private val reconnectScheduled = AtomicBoolean(false)
    private val stopping = AtomicBoolean(false)
    private val registrationRejected = AtomicBoolean(false)
    private val logSubscribed = AtomicBoolean(false)
    private val pendingTokenRegistrations = ConcurrentHashMap<String, PendingTokenRegistration>()

    private data class PendingTokenRegistration(
        val generation: Long,
        val future: CompletableFuture<Boolean>,
    )

    private data class RegistrationContext(
        val request: ServerRegisterRequest,
    )

    private val identityStore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        EditorServerIdentityStore(getDataFolder().toPath())
    }

    @Volatile
    private var client: WebSocketClient? = null

    @Volatile
    private var activeLicense: String? = null

    @Volatile
    private var activeProtocolV2Enabled: Boolean? = null

    @Volatile
    private var reconnectJob: Job? = null

    @Volatile
    private var preparationJob: Job? = null

    @Volatile
    private var registrationTimeoutJob: Job? = null

    @Volatile
    private var sessionEpoch: Long? = null

    @Volatile
    private var workspaceId: String? = null

    @Volatile
    private var relayCapabilities: Set<String> = emptySet()

    @Volatile
    private var logKeywordFilter: String? = null

    private const val RECONNECT_INTERVAL = 30_000L
    private const val REGISTRATION_TIMEOUT = 15_000L
    private const val MAX_INCOMING_MESSAGE_BYTES = 8 * 1024 * 1024
    private const val MAX_INCOMING_MESSAGE_CHARS = MAX_INCOMING_MESSAGE_BYTES
    private const val MAX_INCOMING_MESSAGE_FRAGMENTS = 2_048
    private const val SERVER_URL = "wss://orryx.mcwar.cn/ws/server"
    private const val EDITOR_URL = "https://orryx.mcwar.cn"
    private const val TOKEN_EXPIRES_SECONDS = 300

    internal fun getEditorUrl(): String = EDITOR_URL
    internal fun getTokenExpires(): Int = TOKEN_EXPIRES_SECONDS

    private fun isEnabled(): Boolean = Orryx.config.getBoolean("Editor.Enable", false)

    private fun isProtocolV2Enabled(): Boolean = Orryx.config.getBoolean("Editor.ProtocolV2.Enable", false)

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
        val protocolV2Enabled = isProtocolV2Enabled()
        val currentClient = client
        if (
            activeLicense == license &&
            activeProtocolV2Enabled == protocolV2Enabled &&
            currentClient != null &&
            !registrationRejected.get()
        ) {
            return
        }
        prepareConnection(
            license = license,
            reconnect = activeLicense == license,
            protocolV2Enabled = protocolV2Enabled,
        )
    }

    private fun prepareConnection(license: String, reconnect: Boolean, protocolV2Enabled: Boolean) {
        stopping.set(false)
        val expectedPreparation = preparationGeneration.incrementAndGet()
        preparationJob?.cancel()
        preparationJob = OrryxAPI.ioScope.launch {
            try {
                val identity = identityStore.loadOrCreate()
                val serverName = getServerName()
                if (preparationGeneration.get() != expectedPreparation || stopping.get()) return@launch
                val configuredLicense = getLicense().takeIf { isEnabled() }
                if (configuredLicense != license || isProtocolV2Enabled() != protocolV2Enabled) return@launch
                startConnection(
                    identity = identity,
                    serverName = serverName,
                    license = license,
                    reconnect = reconnect,
                    expectedPreparation = expectedPreparation,
                    v2Enabled = protocolV2Enabled,
                )
            } catch (e: Exception) {
                if (preparationGeneration.get() == expectedPreparation) {
                    consoleMessage("&c[Editor] 初始化服务器身份失败: ${sanitizeLogMessage(e.message)}")
                }
            } finally {
                synchronized(stateLock) {
                    if (preparationGeneration.get() == expectedPreparation) preparationJob = null
                }
            }
        }
    }

    private fun startConnection(
        identity: EditorServerIdentity,
        serverName: String,
        license: String,
        reconnect: Boolean,
        expectedPreparation: Long,
        v2Enabled: Boolean,
    ) {
        val attemptGeneration: Long
        val nextClient: WebSocketClient
        val previousClient: WebSocketClient?
        val offeredProtocols = EditorProtocol.supportedProtocols(v2Enabled)
        val registrationContext = RegistrationContext(
            ServerRegisterRequest(
                license = license,
                serverName = serverName,
                serverId = identity.serverId,
                pluginVersion = pluginVersion,
                protocolVersions = offeredProtocols,
                preferredProtocol = EditorProtocol.preferredProtocol(v2Enabled),
                capabilities = if (v2Enabled) EditorProtocol.V2_CAPABILITIES else emptyList(),
                connectionNonce = UUID.randomUUID().toString(),
            ),
        )
        synchronized(stateLock) {
            if (preparationGeneration.get() != expectedPreparation || stopping.get()) return
            stopping.set(false)
            registrationRejected.set(false)
            reconnectJob?.cancel()
            reconnectJob = null
            registrationTimeoutJob?.cancel()
            registrationTimeoutJob = null
            reconnectScheduled.set(false)
            connected.set(false)
            registered.set(false)
            negotiatedProtocol.set(EditorProtocol.PROTOCOL_V1)
            sessionEpoch = null
            workspaceId = null
            relayCapabilities = emptySet()
            logSubscribed.set(false)
            logKeywordFilter = null
            pendingTokenRegistrations.values.forEach { it.future.complete(false) }
            pendingTokenRegistrations.clear()

            attemptGeneration = generation.incrementAndGet()
            activeLicense = license
            activeProtocolV2Enabled = v2Enabled
            previousClient = client
            nextClient = createClient(SERVER_URL, registrationContext, attemptGeneration, reconnect)
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
            preparationGeneration.incrementAndGet()
            preparationJob?.cancel()
            preparationJob = null
            reconnectJob?.cancel()
            reconnectJob = null
            registrationTimeoutJob?.cancel()
            registrationTimeoutJob = null
            reconnectScheduled.set(false)
            connected.set(false)
            registered.set(false)
            negotiatedProtocol.set(EditorProtocol.PROTOCOL_V1)
            sessionEpoch = null
            workspaceId = null
            relayCapabilities = emptySet()
            logSubscribed.set(false)
            logKeywordFilter = null
            activeLicense = null
            activeProtocolV2Enabled = null
            pendingTokenRegistrations.values.forEach { it.future.complete(false) }
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
            if (stopping.get() || connected.get() || generation.get() != expectedGeneration) return@launch

            val configuredLicense = getLicense().takeIf { isEnabled() }
            if (configuredLicense == null) {
                if (generation.get() == expectedGeneration) disconnect()
                return@launch
            }
            if (generation.get() != expectedGeneration || activeLicense != license || configuredLicense != license) {
                return@launch
            }
            prepareConnection(
                license = configuredLicense,
                reconnect = true,
                protocolV2Enabled = isProtocolV2Enabled(),
            )
        }
        synchronized(stateLock) {
            if (reconnectScheduled.get() && activeLicense == license && generation.get() == expectedGeneration) {
                reconnectJob = job
            } else {
                job.cancel()
            }
        }
    }

    private fun scheduleRegistrationTimeout(expectedGeneration: Long, expectedClient: WebSocketClient) {
        val job = OrryxAPI.ioScope.launch {
            delay(REGISTRATION_TIMEOUT)
            if (!isCurrent(expectedGeneration, expectedClient) || registered.get()) return@launch
            consoleMessage("&c[Editor] 服务器注册超时，将关闭连接并重试")
            connected.set(false)
            closeQuietly(expectedClient)
            scheduleReconnect(expectedGeneration)
        }
        synchronized(stateLock) {
            if (isCurrent(expectedGeneration, expectedClient) && !registered.get()) {
                registrationTimeoutJob?.cancel()
                registrationTimeoutJob = job
            } else {
                job.cancel()
            }
        }
    }

    private fun cancelRegistrationTimeout(expectedGeneration: Long) {
        synchronized(stateLock) {
            if (generation.get() != expectedGeneration) return
            registrationTimeoutJob?.cancel()
            registrationTimeoutJob = null
        }
    }

    private fun createClient(
        url: String,
        registrationContext: RegistrationContext,
        attemptGeneration: Long,
        reconnect: Boolean,
    ): WebSocketClient {
        val draft = BoundedEditorDraft(MAX_INCOMING_MESSAGE_BYTES, MAX_INCOMING_MESSAGE_FRAGMENTS)
        return object : WebSocketClient(URI(url), draft) {
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
                scheduleRegistrationTimeout(attemptGeneration, this)
                val sent = sendMessageForGeneration(
                    attemptGeneration,
                    EditorProtocol.SERVER_REGISTER,
                    "reg_init",
                    EditorProtocol.registrationData(registrationContext.request),
                )
                if (!sent) {
                    cancelRegistrationTimeout(attemptGeneration)
                    connected.set(false)
                    closeQuietly(this)
                    scheduleReconnect(attemptGeneration)
                }
            }

            override fun onMessage(message: String) {
                if (!isCurrent(attemptGeneration, this) || !connected.get()) return
                if (message.length > MAX_INCOMING_MESSAGE_CHARS) {
                    consoleMessage("&c[Editor] 收到超限消息，已关闭连接: ${message.length} chars")
                    close(1009, "消息超过大小限制")
                    return
                }
                try {
                    val msg = json.parseToJsonElement(message).jsonObject
                    val id = (msg["id"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                    val type = (msg["type"] as? JsonPrimitive)?.contentOrNull
                    if (type.isNullOrBlank()) {
                        sendError(attemptGeneration, id, "缺少消息 type", "INVALID_MESSAGE", null)
                        return
                    }
                    val dataElement = msg["data"]
                    if (dataElement != null && dataElement !is JsonObject) {
                        sendError(attemptGeneration, id, "data 必须是对象", "INVALID_DATA", type)
                        return
                    }
                    val data = dataElement as? JsonObject

                    debug { "&e┣&7[Editor] 收到消息: type=$type, id=$id" }
                    when (EditorProtocol.inboundDisposition(type)) {
                        InboundDisposition.ACCEPT -> Unit
                        InboundDisposition.WRONG_DIRECTION -> {
                            sendError(
                                attemptGeneration,
                                id,
                                "消息方向不允许: $type",
                                "MESSAGE_DIRECTION_NOT_ALLOWED",
                                type,
                            )
                            return
                        }
                        InboundDisposition.UNKNOWN -> {
                            sendError(attemptGeneration, id, "未知消息类型: $type", "UNKNOWN_MESSAGE_TYPE", type)
                            return
                        }
                    }
                    if (type != EditorProtocol.SERVER_REGISTER_RESULT && type != EditorProtocol.ERROR && !registered.get()) {
                        sendError(attemptGeneration, id, "Editor 服务器尚未完成注册", "NOT_REGISTERED", type)
                        return
                    }
                    when (type) {
                        EditorProtocol.SERVER_REGISTER_RESULT -> {
                            if (id == "reg_init" && !registered.get() && !registrationRejected.get()) {
                                handleRegisterResult(data, registrationContext.request)
                            }
                        }
                        EditorProtocol.ERROR -> {
                            val code = (data?.get("code") as? JsonPrimitive)?.contentOrNull.orEmpty()
                            val errorMessage = (data?.get("message") as? JsonPrimitive)?.contentOrNull.orEmpty()
                            consoleMessage(
                                "&c[Editor] 中心返回错误${code.takeIf { it.isNotEmpty() }?.let { " [$it]" }.orEmpty()}: " +
                                    sanitizeLogMessage(errorMessage),
                            )
                        }
                        "token.register.result" -> {
                            val success = (data?.get("success") as? JsonPrimitive)?.booleanOrNull ?: false
                            synchronized(stateLock) {
                                val pending = pendingTokenRegistrations[id]
                                if (
                                    generation.get() == attemptGeneration &&
                                    connected.get() &&
                                    registered.get() &&
                                    pending?.generation == attemptGeneration &&
                                    pendingTokenRegistrations.remove(id, pending)
                                ) {
                                    pending.future.complete(success)
                                }
                            }
                            debug { "&e┣&7[Editor] Token 注册结果: $success" }
                        }
                        "file.list" -> FileHandler.handleList(attemptGeneration, id, data)
                        "file.read" -> FileHandler.handleRead(attemptGeneration, id, data)
                        "file.write" -> FileHandler.handleWrite(attemptGeneration, id, data)
                        "file.create" -> FileHandler.handleCreate(attemptGeneration, id, data)
                        "file.delete" -> FileHandler.handleDelete(attemptGeneration, id, data)
                        "file.rename" -> FileHandler.handleRename(attemptGeneration, id, data)
                        "reload" -> ReloadHandler.handle(attemptGeneration, id, data)
                        "log.subscribe" -> {
                            logSubscribed.set(true)
                            logKeywordFilter = ((data?.get("filters") as? JsonObject)
                                ?.get("keyword") as? JsonPrimitive)?.contentOrNull
                            sendMessage(attemptGeneration, "log.subscribe.result", id, buildJsonObject {
                                put("success", true)
                            })
                            debug { "&e┣&7[Editor] 日志订阅已开启" }
                        }
                        "log.unsubscribe" -> {
                            logSubscribed.set(false)
                            logKeywordFilter = null
                            sendMessage(attemptGeneration, "log.unsubscribe.result", id, buildJsonObject {
                                put("success", true)
                            })
                            debug { "&e┣&7[Editor] 日志订阅已关闭" }
                        }
                        "token.revoke.result" -> {
                            val success = (data?.get("success") as? JsonPrimitive)?.booleanOrNull ?: false
                            debug { "&e┣&7[Editor] Token 撤销结果: $success" }
                        }
                        else -> sendError(
                            attemptGeneration,
                            id,
                            "未处理的消息类型: $type",
                            "UNHANDLED_MESSAGE_TYPE",
                            type,
                        )
                    }
                } catch (e: Exception) {
                    consoleMessage("&c[Editor] 消息处理异常: ${sanitizeLogMessage(e.message)}")
                }
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                if (!isCurrent(attemptGeneration, this)) return
                synchronized(stateLock) {
                    if (client === this) client = null
                    registrationTimeoutJob?.cancel()
                    registrationTimeoutJob = null
                    connected.set(false)
                    registered.set(false)
                    negotiatedProtocol.set(EditorProtocol.PROTOCOL_V1)
                    sessionEpoch = null
                    workspaceId = null
                    logSubscribed.set(false)
                    logKeywordFilter = null
                    pendingTokenRegistrations.values.forEach { it.future.complete(false) }
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

            private fun handleRegisterResult(data: JsonObject?, request: ServerRegisterRequest) {
                cancelRegistrationTimeout(attemptGeneration)
                val result = EditorProtocol.parseRegisterResult(data)
                val validation = EditorProtocol.validateRegisterResult(result, request)
                val success = result.success && validation.accepted
                registered.set(success)
                if (success) {
                    negotiatedProtocol.set(result.negotiatedProtocol)
                    sessionEpoch = result.sessionEpoch
                    workspaceId = result.workspaceId?.takeIf { it.isNotBlank() }
                    relayCapabilities = result.relayCapabilities.toSet()
                    registrationRejected.set(false)
                    val fallback = if (result.negotiatedProtocol == EditorProtocol.PROTOCOL_V1) " (V1 兼容模式)" else ""
                    consoleMessage(
                        "&e┣&7[Editor] 服务器注册成功$fallback: &a${sanitizeLogMessage(result.message)}",
                    )
                } else {
                    registrationRejected.set(true)
                    val message = when {
                        !validation.protocolAccepted -> "中心返回了未提供的协议版本: ${result.negotiatedProtocol}"
                        !validation.serverIdAccepted -> "中心返回的 serverId 与本机稳定身份不匹配"
                        !validation.nonceAccepted -> "中心返回的 connectionNonce 与当前连接不匹配"
                        !validation.sessionMetadataAccepted -> "中心返回的 workspaceId 或 sessionEpoch 无效"
                        !validation.v2ContractAccepted -> "中心返回的 V2 协商元数据或能力不完整"
                        else -> result.message
                    }
                    consoleMessage("&c[Editor] 服务器注册失败，已停止自动重试: ${sanitizeLogMessage(message)}")
                    close()
                }
            }
        }
    }

    /** 发送消息到当前连接；返回 false 表示没有可用连接或发送失败。 */
    fun sendMessage(type: String, id: String, data: JsonObject): Boolean {
        return sendMessageForGeneration(generation.get(), type, id, data)
    }

    internal fun sendMessage(
        expectedGeneration: Long,
        type: String,
        id: String,
        data: JsonObject,
    ): Boolean {
        return sendMessageForGeneration(expectedGeneration, type, id, data)
    }

    private fun sendMessageForGeneration(
        expectedGeneration: Long,
        type: String,
        id: String,
        data: JsonObject,
    ): Boolean {
        if (!EditorProtocol.isServerToCenter(type)) {
            consoleMessage("&c[Editor] 拒绝发送方向未允许的消息: $type")
            return false
        }
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
        sendMessage(EditorProtocol.ERROR, id, buildJsonObject {
            put("code", "REQUEST_FAILED")
            put("message", message)
        })
    }

    internal fun sendError(
        expectedGeneration: Long,
        id: String,
        message: String,
        code: String = "REQUEST_FAILED",
        requestType: String? = null,
    ) {
        sendMessage(expectedGeneration, EditorProtocol.ERROR, id, buildJsonObject {
            put("code", code)
            put("message", message)
            requestType?.let { put("requestType", it) }
        })
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
        val expectedGeneration: Long
        val id: String
        val future = CompletableFuture<Boolean>()
        val pending: PendingTokenRegistration
        synchronized(stateLock) {
            expectedGeneration = generation.get()
            if (!registered.get() || !isGenerationCurrent(expectedGeneration)) {
                return CompletableFuture.completedFuture(false)
            }
            id = "tok_${expectedGeneration}_${System.nanoTime()}"
            pending = PendingTokenRegistration(expectedGeneration, future)
            pendingTokenRegistrations[id] = pending
        }
        val timeoutTask = SimpleTimeoutTask.createSimpleTask(100L) {
            if (pendingTokenRegistrations.remove(id, pending)) future.complete(false)
        }
        future.whenComplete { _, _ ->
            SimpleTimeoutTask.cancel(timeoutTask, false)
            pendingTokenRegistrations.remove(id, pending)
        }
        val sent = sendMessage(expectedGeneration, "token.register", id, buildJsonObject {
            put("token", token)
            put("playerName", playerName)
            put("expiresIn", TOKEN_EXPIRES_SECONDS * 1000L)
        })
        if (!sent && pendingTokenRegistrations.remove(id, pending)) future.complete(false)
        return future
    }

    fun isConnected(): Boolean = connected.get()

    fun isRegistered(): Boolean = registered.get()

    internal fun isProtocolV2(expectedGeneration: Long): Boolean {
        return generation.get() == expectedGeneration &&
            registered.get() &&
            negotiatedProtocol.get() == EditorProtocol.PROTOCOL_V2
    }

    internal fun currentNegotiatedProtocol(): String = negotiatedProtocol.get()

    internal fun currentSessionEpoch(): Long? = sessionEpoch

    internal fun currentWorkspaceId(): String? = workspaceId

    internal fun currentRelayCapabilities(): Set<String> = relayCapabilities

    internal fun currentGeneration(): Long = generation.get()

    internal fun isGenerationCurrent(expectedGeneration: Long): Boolean {
        return generation.get() == expectedGeneration && connected.get() && client != null
    }

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
