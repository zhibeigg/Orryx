package org.gitee.orryx.module.ai

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.lark.oapi.okhttp.MediaType
import com.lark.oapi.okhttp.OkHttpClient
import com.lark.oapi.okhttp.Request
import com.lark.oapi.okhttp.RequestBody
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI.Companion.ioScope
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.debug
import taboolib.module.configuration.util.ReloadAwareLazy
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object OpenAI {

    private val client: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { OkHttpClient().newBuilder().callTimeout(Duration.of(1, ChronoUnit.MINUTES)).build() }
    private val json = Json {
        ignoreUnknownKeys = true  // 忽略未知键
    }

    private val npcChatCache = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .scheduler(Scheduler.systemScheduler())
        .build<String, MutableList<SendMessage>>()

    @Reload(0)
    private fun reload() {
        npcChatCache.invalidateAll()
    }

    // 请求参数模型
    @Serializable
    data class OpenAIRequest(
        val model: String,
        @SerialName("max_tokens")
        val maxTokens: Int,
        val messages: List<SendMessage>,
        val temperature: Double = 1.0
    )

    // 响应解析模型
    @Serializable
    data class OpenAIResponse(
        val id: String,
        val choices: List<Choice>,
        val usage: Usage
    )

    @Serializable
    data class Usage(
        @SerialName("completion_tokens")
        val completionTokens: Int,
        @SerialName("prompt_tokens")
        val promptTokens: Int,
        @SerialName("total_tokens")
        val totalTokens: Int
    )

    @Serializable
    data class Choice(
        val index: Int,
        @SerialName("finish_reason")
        val finishReason: String,
        val message: Message
    )

    @Serializable
    data class SendMessage(
        val role: String,
        val content: String,
        val name: String?
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String
    )

    private val API_KEY by ReloadAwareLazy(Orryx.config) { Orryx.config.getString("OpenAI.ApiKey") ?: error("未配置OpenAI ApiKey") }
    private val BASE_URL by ReloadAwareLazy(Orryx.config) { Orryx.config.getString("OpenAI.BaseUrl") ?: error("未配置OpenAI BaseUrl") }

    fun npcChat(player: String, npc: String, npcDescription: String, message: String, model: String, maxTokens: Int, temperature: Double): CompletableFuture<String> {
        val mediaType = MediaType.parse("application/json")

        val context = npcChatCache.get("$player@$npc") { mutableListOf(SendMessage(role = "system", content = npcDescription, name = npc)) }!!
        val future = CompletableFuture<String>()

        ioScope.launch {
            synchronized(context) {
                if (context.size > 5) {
                    // 清除除了 system 后面的第一条信息
                    context.removeAt(1)
                }
                val userMessage = SendMessage(role = "user", content = message, name = player)
                context += userMessage

                val requestBody = OpenAIRequest(
                    model = model,
                    maxTokens = maxTokens,
                    messages = context,
                    temperature = temperature
                )
                val jsonBody = json.encodeToString(requestBody)

                val request = Request.Builder()
                    .url("$BASE_URL/chat/completions")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .post(RequestBody.create(mediaType, jsonBody))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body()!!.string()
                    val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBody)

                    // 提取回复内容
                    val reply = openAIResponse.choices.first().message
                    npcChatCache.put("$player@$npc", (context + SendMessage(reply.role, reply.content, npc)).toMutableList())
                    future.complete(reply.content)
                    debug { "完成了一次 AI 调用，本次消耗 ${openAIResponse.usage.totalTokens} Token （ 用户侧: ${openAIResponse.usage.promptTokens}， AI侧: ${openAIResponse.usage.completionTokens} ）" }
                } else {
                    future.complete("请求失败，请重试")
                }
            }
        }.invokeOnCompletion {
            if (!future.isDone) {
                future.completeExceptionally(it)
            }
        }
        return future
    }
}