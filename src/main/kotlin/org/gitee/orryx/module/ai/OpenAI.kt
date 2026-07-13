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
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.debug
import taboolib.module.configuration.util.ReloadAwareLazy
import java.io.IOException
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object OpenAI {

    private val client: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClient().newBuilder().callTimeout(Duration.of(1, ChronoUnit.MINUTES)).build()
    }
    private val json = Json { ignoreUnknownKeys = true }

    private val npcChatCache = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .scheduler(Scheduler.systemScheduler())
        .build<String, List<SendMessage>>()

    /** 同一玩家与 NPC 的请求按进入顺序串行，避免并发回复覆盖会话历史。 */
    private val conversationTails = ConcurrentHashMap<String, CompletableFuture<String>>()

    @Reload(0)
    private fun reload() {
        npcChatCache.invalidateAll()
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
        val name: String?,
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String,
    )

    private val API_KEY by ReloadAwareLazy(Orryx.config) {
        Orryx.config.getString("OpenAI.ApiKey")?.trim()?.takeIf { it.isNotEmpty() }
            ?: error("未配置 OpenAI ApiKey")
    }
    private val BASE_URL by ReloadAwareLazy(Orryx.config) {
        val raw = Orryx.config.getString("OpenAI.BaseUrl")?.trimEnd('/')?.takeIf { it.isNotEmpty() }
            ?: error("未配置 OpenAI BaseUrl")
        val uri = runCatching { URI(raw) }.getOrElse { error("OpenAI BaseUrl 格式无效") }
        val insecureLocalAllowed = Orryx.config.getBoolean("OpenAI.AllowInsecureLocalHttp", false)
        val localHost = uri.host?.lowercase() in setOf("localhost", "127.0.0.1", "::1")
        require(uri.scheme.equals("https", true) || (insecureLocalAllowed && localHost && uri.scheme.equals("http", true))) {
            "OpenAI BaseUrl 必须使用 HTTPS；本机 HTTP 代理需显式开启 OpenAI.AllowInsecureLocalHttp"
        }
        raw
    }

    fun npcChat(
        player: String,
        npc: String,
        npcDescription: String,
        message: String,
        model: String,
        maxTokens: Int,
        temperature: Double,
    ): CompletableFuture<String> {
        val conversationKey = "$player@$npc"
        lateinit var requestFuture: CompletableFuture<String>
        conversationTails.compute(conversationKey) { _, previous ->
            val ready = previous?.handle { _, _ -> Unit } ?: CompletableFuture.completedFuture(Unit)
            requestFuture = ready.thenCompose {
                sendChatRequest(
                    conversationKey = conversationKey,
                    player = player,
                    npc = npc,
                    npcDescription = npcDescription,
                    message = message,
                    model = model,
                    maxTokens = maxTokens,
                    temperature = temperature,
                )
            }
            requestFuture
        }
        requestFuture.whenComplete { _, _ -> conversationTails.remove(conversationKey, requestFuture) }
        return requestFuture
    }

    private fun sendChatRequest(
        conversationKey: String,
        player: String,
        npc: String,
        npcDescription: String,
        message: String,
        model: String,
        maxTokens: Int,
        temperature: Double,
    ): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        try {
            val previous = npcChatCache.get(conversationKey) {
                listOf(SendMessage(role = "system", content = npcDescription, name = npc))
            } ?: listOf(SendMessage(role = "system", content = npcDescription, name = npc))
            val requestMessages = appendMessage(
                previous,
                SendMessage(role = "user", content = message, name = player),
            )
            val requestBody = OpenAIRequest(
                model = model,
                maxTokens = maxTokens,
                messages = requestMessages,
                temperature = temperature,
            )
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $API_KEY")
                .post(RequestBody.create(JSON_MEDIA_TYPE, json.encodeToString(requestBody)))
                .build()

            val call = client.newCall(request)
            future.whenComplete { _, _ -> if (future.isCancelled) call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { closedResponse ->
                        try {
                            if (!closedResponse.isSuccessful) {
                                future.completeExceptionally(
                                    OpenAIRequestException("OpenAI 请求失败: HTTP ${closedResponse.code()}")
                                )
                                return
                            }
                            val responseBody = closedResponse.body()?.string()
                                ?: throw OpenAIRequestException("OpenAI 返回了空响应")
                            val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBody)
                            val reply = openAIResponse.choices.firstOrNull()?.message
                                ?: throw OpenAIRequestException("OpenAI 响应中没有回复内容")
                            npcChatCache.put(
                                conversationKey,
                                appendMessage(requestMessages, SendMessage(reply.role, reply.content, npc)),
                            )
                            future.complete(reply.content)
                            openAIResponse.usage?.let { usage ->
                                debug {
                                    "完成了一次 AI 调用，本次消耗 ${usage.totalTokens} Token （ 用户侧: ${usage.promptTokens}， AI侧: ${usage.completionTokens} ）"
                                }
                            }
                        } catch (e: Exception) {
                            future.completeExceptionally(e)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        return future
    }

    /** 保留 system 消息与最近的对话，且只在请求成功后写回缓存。 */
    internal fun appendMessage(history: List<SendMessage>, message: SendMessage): List<SendMessage> {
        val system = history.firstOrNull { it.role == "system" }
        val recent = history
            .filterNot { it === system }
            .takeLast(MAX_CONTEXT_MESSAGES - 1)
        val combined = buildList {
            system?.let(::add)
            addAll(recent)
            add(message)
        }
        if (combined.size <= MAX_CONTEXT_MESSAGES) return combined
        val preservedSystem = combined.firstOrNull { it.role == "system" }
        val tailSize = MAX_CONTEXT_MESSAGES - if (preservedSystem == null) 0 else 1
        return buildList {
            preservedSystem?.let(::add)
            addAll(combined.filterNot { it === preservedSystem }.takeLast(tailSize))
        }
    }

    class OpenAIRequestException(message: String) : IOException(message)

    private val JSON_MEDIA_TYPE = MediaType.parse("application/json")
    private const val MAX_CONTEXT_MESSAGES = 6
}
