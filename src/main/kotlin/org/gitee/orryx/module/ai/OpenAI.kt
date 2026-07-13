package org.gitee.orryx.module.ai

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.lark.oapi.okhttp.Call
import com.lark.oapi.okhttp.Callback
import com.lark.oapi.okhttp.MediaType
import com.lark.oapi.okhttp.OkHttpClient
import com.lark.oapi.okhttp.Request
import com.lark.oapi.okhttp.RequestBody
import com.lark.oapi.okhttp.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.util.ReloadAwareLazy
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal fun isOpenAIRequestExpired(deadlineNanos: Long, nowNanos: Long = System.nanoTime()): Boolean {
    return nowNanos >= deadlineNanos
}

object OpenAI {

    private val clientDelegate = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClient().newBuilder().callTimeout(Duration.of(1, ChronoUnit.MINUTES)).build()
    }
    private val client: OkHttpClient by clientDelegate
    private val json = Json { ignoreUnknownKeys = true }

    private val npcChatCache = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .scheduler(Scheduler.systemScheduler())
        .build<String, List<SendMessage>>()

    private val requestQueue = SerialFutureQueue<String>()
    private val activeRequests = ConcurrentHashMap.newKeySet<QueuedChatRequest>()
    private val lifecycleGeneration = AtomicLong()
    private val acceptingRequests = AtomicBoolean(true)
    private val lifecycleLock = Any()
    private val schedulerLock = Any()

    @Volatile
    private var timeoutScheduler: ScheduledExecutorService? = null

    @Reload(0)
    private fun reload() {
        val requests = synchronized(lifecycleLock) {
            lifecycleGeneration.incrementAndGet()
            npcChatCache.invalidateAll()
            activeRequests.toList()
        }
        cancelRequests(requests, OpenAIRequestException("OpenAI 配置已重载，请重新发起请求"))
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        synchronized(lifecycleLock) {
            acceptingRequests.set(true)
        }
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        val requests = synchronized(lifecycleLock) {
            acceptingRequests.set(false)
            lifecycleGeneration.incrementAndGet()
            npcChatCache.invalidateAll()
            activeRequests.toList()
        }
        cancelRequests(requests, OpenAIRequestException("Orryx 已关闭"))
        if (clientDelegate.isInitialized()) {
            client.dispatcher().cancelAll()
            client.connectionPool().evictAll()
            client.dispatcher().executorService().shutdown()
        }
        synchronized(schedulerLock) {
            timeoutScheduler?.shutdownNow()
            timeoutScheduler = null
        }
    }

    @SubscribeEvent
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId
        activeRequests.filter { it.playerId == playerId }.forEach {
            it.cancel(OpenAIRequestException("玩家已离线"))
        }
    }

    @Serializable
    data class OpenAIRequest(
        val model: String,
        @SerialName("max_tokens")
        val maxTokens: Int,
        val messages: List<SendMessage>,
        val temperature: Double = 1.0,
    )

    @Serializable
    data class OpenAIResponse(
        val id: String = "",
        val choices: List<Choice> = emptyList(),
        val usage: Usage? = null,
    )

    @Serializable
    data class Usage(
        @SerialName("completion_tokens")
        val completionTokens: Int = 0,
        @SerialName("prompt_tokens")
        val promptTokens: Int = 0,
        @SerialName("total_tokens")
        val totalTokens: Int = 0,
    )

    @Serializable
    data class Choice(
        val index: Int = 0,
        @SerialName("finish_reason")
        val finishReason: String? = null,
        val message: Message,
    )

    @Serializable
    data class SendMessage(
        val role: String,
        val content: String,
        val name: String? = null,
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String,
    )

    private data class ChatReply(
        val role: String,
        val content: String,
        val usage: Usage?,
    )

    private enum class RequestState {
        ACTIVE,
        SUCCEEDED,
        FAILED,
        CANCELLED,
    }

    private class RequestFuture<T>(
        private val beforeCancel: () -> Boolean,
    ) : CompletableFuture<T>() {

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            if (!beforeCancel()) return false
            return super.cancel(mayInterruptIfRunning)
        }
    }

    private class QueuedChatRequest(
        val playerId: UUID,
        private val playerName: String,
        private val npcKey: String,
        private val npcName: String,
        private val npcDescription: String,
        private val message: String,
        private val model: String,
        private val maxTokens: Int,
        private val temperature: Double,
        private val generation: Long,
        private val deadlineNanos: Long,
    ) {
        private val state = AtomicReference(RequestState.ACTIVE)
        private val call = AtomicReference<Call?>()
        private val timeoutTask = AtomicReference<ScheduledFuture<*>?>()
        val result = RequestFuture<String>(::cancelFromCaller)
        val conversationKey: String = conversationKey(playerId, npcKey)

        fun scheduleTimeout() {
            val remaining = deadlineNanos - System.nanoTime()
            if (remaining <= 0L) {
                cancel(TimeoutException("OpenAI 请求排队超时"))
                return
            }
            timeoutTask.set(timeoutScheduler().schedule({
                cancel(TimeoutException("OpenAI 请求总耗时超过 ${REQUEST_TIMEOUT_MILLIS}ms"))
            }, remaining, TimeUnit.NANOSECONDS))
        }

        fun execute(): CompletableFuture<Unit> {
            if (state.get() != RequestState.ACTIVE) return CompletableFuture.completedFuture(Unit)
            val launch = synchronized(lifecycleLock) {
                if (state.get() != RequestState.ACTIVE) return@synchronized null
                if (!acceptingRequests.get() || generation != lifecycleGeneration.get()) {
                    cancel(OpenAIRequestException("OpenAI 请求已失效"))
                    return@synchronized null
                }
                if (isOpenAIRequestExpired(deadlineNanos)) {
                    cancel(TimeoutException("OpenAI 请求排队超时"))
                    return@synchronized null
                }

                val previous = npcChatCache.get(conversationKey) {
                    listOf(SendMessage("system", npcDescription, safeMessageName(npcName)))
                } ?: listOf(SendMessage("system", npcDescription, safeMessageName(npcName)))
                val requestMessages = appendMessage(
                    previous,
                    SendMessage("user", message, safeMessageName(playerName)),
                )
                requestMessages to sendChatRequest(requestMessages, model, maxTokens, temperature, this)
            } ?: return CompletableFuture.completedFuture(Unit)

            val (requestMessages, transport) = launch
            return transport.handle { reply, throwable ->
                if (throwable == null) {
                    completeSuccess(requestMessages, reply)
                } else {
                    fail(unwrapCompletionFailure(throwable))
                }
                Unit
            }
        }

        fun attachCall(newCall: Call): Boolean {
            call.set(newCall)
            if (state.get() != RequestState.ACTIVE) {
                newCall.cancel()
                return false
            }
            if (isOpenAIRequestExpired(deadlineNanos)) {
                cancel(TimeoutException("OpenAI 请求总耗时超过 ${REQUEST_TIMEOUT_MILLIS}ms"))
                return false
            }
            return true
        }

        fun completeSuccess(requestMessages: List<SendMessage>, reply: ChatReply) {
            synchronized(lifecycleLock) {
                if (!acceptingRequests.get() || generation != lifecycleGeneration.get()) {
                    fail(OpenAIRequestException("OpenAI 请求已被重载或关闭"))
                    return
                }
                if (isOpenAIRequestExpired(deadlineNanos)) {
                    cancel(TimeoutException("OpenAI 请求总耗时超过 ${REQUEST_TIMEOUT_MILLIS}ms"))
                    return
                }
                if (!state.compareAndSet(RequestState.ACTIVE, RequestState.SUCCEEDED)) return
                npcChatCache.put(
                    conversationKey,
                    appendMessage(requestMessages, SendMessage(reply.role, reply.content, safeMessageName(npcName))),
                )
                result.complete(reply.content)
            }
            reply.usage?.let { usage ->
                debug {
                    "完成了一次 AI 调用，本次消耗 ${usage.totalTokens} Token （ 用户侧: ${usage.promptTokens}， AI侧: ${usage.completionTokens} ）"
                }
            }
        }

        fun fail(throwable: Throwable) {
            if (state.compareAndSet(RequestState.ACTIVE, RequestState.FAILED)) {
                result.completeExceptionally(throwable)
            }
        }

        fun cancel(throwable: Throwable) {
            if (state.compareAndSet(RequestState.ACTIVE, RequestState.CANCELLED)) {
                call.get()?.cancel()
                result.completeExceptionally(throwable)
            }
        }

        fun cleanUp() {
            timeoutTask.getAndSet(null)?.cancel(false)
            call.set(null)
        }

        private fun cancelFromCaller(): Boolean {
            if (!state.compareAndSet(RequestState.ACTIVE, RequestState.CANCELLED)) return false
            call.get()?.cancel()
            return true
        }
    }

    private val API_KEY by ReloadAwareLazy(Orryx.config) {
        Orryx.config.getString("OpenAI.ApiKey")?.trim()?.takeIf { it.isNotEmpty() }
            ?: error("未配置 OpenAI ApiKey")
    }
    private val BASE_URL by ReloadAwareLazy(Orryx.config) {
        validateBaseUrl(
            Orryx.config.getString("OpenAI.BaseUrl").orEmpty(),
            Orryx.config.getBoolean("OpenAI.AllowInsecureLocalHttp", false),
        )
    }

    fun npcChat(
        playerId: UUID,
        playerName: String,
        npcKey: String,
        npcName: String,
        npcDescription: String,
        message: String,
        model: String,
        maxTokens: Int,
        temperature: Double,
    ): CompletableFuture<String> {
        val validationFailure = validateRequest(playerName, npcKey, npcDescription, message, model, maxTokens, temperature)
        if (validationFailure != null) return failedFuture(validationFailure)

        return synchronized(lifecycleLock) {
            if (!acceptingRequests.get()) {
                return@synchronized failedFuture(OpenAIRequestException("OpenAI 模块正在关闭"))
            }
            val request = QueuedChatRequest(
                playerId = playerId,
                playerName = playerName,
                npcKey = npcKey,
                npcName = npcName,
                npcDescription = npcDescription,
                message = message,
                model = model,
                maxTokens = maxTokens,
                temperature = temperature,
                generation = lifecycleGeneration.get(),
                deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REQUEST_TIMEOUT_MILLIS),
            )
            activeRequests.add(request)
            try {
                request.scheduleTimeout()
            } catch (throwable: Throwable) {
                activeRequests.remove(request)
                request.fail(throwable)
                return@synchronized request.result
            }
            val node = requestQueue.enqueue(request.conversationKey, request.result, request::execute)
            node.whenComplete { _, _ ->
                request.cleanUp()
                activeRequests.remove(request)
            }
            request.result
        }
    }

    private fun sendChatRequest(
        messages: List<SendMessage>,
        model: String,
        maxTokens: Int,
        temperature: Double,
        owner: QueuedChatRequest,
    ): CompletableFuture<ChatReply> {
        val future = CompletableFuture<ChatReply>()
        try {
            val requestBody = OpenAIRequest(model, maxTokens, messages, temperature)
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $API_KEY")
                .post(RequestBody.create(JSON_MEDIA_TYPE, json.encodeToString(requestBody)))
                .build()
            val call = client.newCall(request)
            if (!owner.attachCall(call)) {
                future.completeExceptionally(OpenAIRequestException("OpenAI 请求已取消"))
                return future
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { closedResponse ->
                        try {
                            if (!closedResponse.isSuccessful) {
                                throw OpenAIRequestException("OpenAI 请求失败: HTTP ${closedResponse.code()}")
                            }
                            val responseBody = readLimitedBody(closedResponse)
                            val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBody)
                            val reply = openAIResponse.choices.firstOrNull()?.message
                                ?: throw OpenAIRequestException("OpenAI 响应中没有回复内容")
                            future.complete(ChatReply(reply.role, reply.content, openAIResponse.usage))
                        } catch (throwable: Throwable) {
                            future.completeExceptionally(throwable)
                            rethrowFatal(throwable)
                        }
                    }
                }
            })
        } catch (throwable: Throwable) {
            future.completeExceptionally(throwable)
            rethrowFatal(throwable)
        }
        return future
    }

    private fun readLimitedBody(response: Response): String {
        val body = response.body() ?: throw OpenAIRequestException("OpenAI 返回了空响应")
        val declaredLength = body.contentLength()
        if (declaredLength > MAX_RESPONSE_BYTES) {
            throw OpenAIRequestException("OpenAI 响应超过大小限制: $declaredLength > $MAX_RESPONSE_BYTES")
        }
        body.byteStream().use { input ->
            val output = ByteArrayOutputStream(
                if (declaredLength in 1..MAX_RESPONSE_BYTES) declaredLength.toInt() else DEFAULT_RESPONSE_BUFFER_BYTES,
            )
            val buffer = ByteArray(RESPONSE_BUFFER_BYTES)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                total += read
                if (total > MAX_RESPONSE_BYTES) {
                    throw OpenAIRequestException("OpenAI 响应超过大小限制: $total > $MAX_RESPONSE_BYTES")
                }
                output.write(buffer, 0, read)
            }
            return String(output.toByteArray(), StandardCharsets.UTF_8)
        }
    }

    internal fun appendMessage(history: List<SendMessage>, message: SendMessage): List<SendMessage> {
        return appendBoundedConversation(history, message, MAX_CONTEXT_MESSAGES) { it.role == "system" }
    }

    internal fun conversationKey(playerId: UUID, npcKey: String): String = "$playerId\u0000$npcKey"

    internal fun validateBaseUrl(rawValue: String, allowInsecureLocalHttp: Boolean): String {
        val raw = rawValue.trim().trimEnd('/').takeIf { it.isNotEmpty() }
            ?: error("未配置 OpenAI BaseUrl")
        val uri = runCatching { URI(raw) }.getOrElse { error("OpenAI BaseUrl 格式无效") }
        val host = uri.host?.lowercase()?.trim('[', ']') ?: error("OpenAI BaseUrl 缺少有效主机名")
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "OpenAI BaseUrl 不允许包含用户信息、查询参数或片段"
        }
        val localHost = host in setOf("localhost", "127.0.0.1", "::1")
        require(uri.scheme.equals("https", true) || (allowInsecureLocalHttp && localHost && uri.scheme.equals("http", true))) {
            "OpenAI BaseUrl 必须使用 HTTPS；本机 HTTP 代理需显式开启 OpenAI.AllowInsecureLocalHttp"
        }
        return raw
    }

    private fun validateRequest(
        playerName: String,
        npcKey: String,
        npcDescription: String,
        message: String,
        model: String,
        maxTokens: Int,
        temperature: Double,
    ): Throwable? {
        if (playerName.isBlank() || npcKey.isBlank() || npcDescription.isBlank() || model.isBlank()) {
            return IllegalArgumentException("OpenAI 玩家、NPC 和模型参数不能为空")
        }
        if (message.length > MAX_INPUT_CHARS || npcDescription.length > MAX_INPUT_CHARS) {
            return IllegalArgumentException("OpenAI 输入超过长度限制")
        }
        if (maxTokens <= 0 || maxTokens > MAX_TOKENS) {
            return IllegalArgumentException("OpenAI maxTokens 必须在 1..$MAX_TOKENS 内")
        }
        if (!temperature.isFinite() || temperature !in 0.0..2.0) {
            return IllegalArgumentException("OpenAI temperature 必须是 0.0..2.0 的有限值")
        }
        return null
    }

    private fun timeoutScheduler(): ScheduledExecutorService {
        timeoutScheduler?.takeUnless { it.isShutdown }?.let { return it }
        synchronized(schedulerLock) {
            timeoutScheduler?.takeUnless { it.isShutdown }?.let { return it }
            return Executors.newSingleThreadScheduledExecutor(OpenAIThreadFactory).also { timeoutScheduler = it }
        }
    }

    private fun cancelRequests(requests: Collection<QueuedChatRequest>, throwable: Throwable) {
        requests.forEach { it.cancel(throwable) }
    }

    private fun safeMessageName(value: String): String? {
        val safe = value.replace(UNSAFE_NAME_REGEX, "_").trim('_').take(MAX_MESSAGE_NAME_LENGTH)
        return safe.takeIf { it.isNotEmpty() }
    }

    private fun <T> failedFuture(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }

    private fun unwrapCompletionFailure(throwable: Throwable): Throwable = throwable.cause ?: throwable

    private fun rethrowFatal(throwable: Throwable) {
        when (throwable) {
            is VirtualMachineError -> throw throwable
            is ThreadDeath -> throw throwable
            is LinkageError -> throw throwable
        }
    }

    private object OpenAIThreadFactory : ThreadFactory {
        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, "Orryx-OpenAI-Timeout").apply { isDaemon = true }
        }
    }

    class OpenAIRequestException(message: String, cause: Throwable? = null) : IOException(message, cause)

    private val JSON_MEDIA_TYPE = MediaType.parse("application/json")
    private val UNSAFE_NAME_REGEX = Regex("[^A-Za-z0-9_-]")
    private const val MAX_CONTEXT_MESSAGES = 6
    private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    private const val RESPONSE_BUFFER_BYTES = 8 * 1024
    private const val DEFAULT_RESPONSE_BUFFER_BYTES = 8 * 1024
    private const val MAX_INPUT_CHARS = 64 * 1024
    private const val MAX_TOKENS = 1_000_000
    private const val MAX_MESSAGE_NAME_LENGTH = 64
    private const val REQUEST_TIMEOUT_MILLIS = 60_000L
}
