package org.gitee.orryx.module.ai

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.lark.oapi.okhttp.MediaType
import com.lark.oapi.okhttp.OkHttpClient
import com.lark.oapi.okhttp.Request
import com.lark.oapi.okhttp.RequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.module.wiki.LarkSuite
import org.gitee.orryx.utils.debug
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


object OpenAI {

    private val client: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { OkHttpClient().newBuilder().build() }
    private val json = Json {
        ignoreUnknownKeys = true  // 忽略未知键
    }

    private val npcChatCache = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .scheduler(Scheduler.systemScheduler())
        .build<String, MutableList<SendMessage>> {
            mutableListOf()
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

    @Serializable
    data class Content(
        val token: String
    )

    private val API_KEY
        get() = Orryx.config.getString("OpenAI.ApiKey") ?: error("未配置OpenAI ApiKey")

    private val BASE_URL
        get() = Orryx.config.getString("OpenAI.BaseUrl") ?: error("未配置OpenAI BaseUrl")

    fun npcChat(player: String, npc: String, npcDescription: String, message: String, model: String, maxTokens: Int, temperature: Double): CompletableFuture<String> {
        val mediaType = MediaType.parse("application/json")

        val context = npcChatCache.get("$player@$npc")

        val list = mutableListOf<SendMessage>()

        list += SendMessage(role = "system", content = npcDescription, name = npc)
        context?.forEach {
            list += it
        }
        val userMessage = SendMessage(role = "user", content = message, name = player)
        list += userMessage

        val requestBody = OpenAIRequest(
            model = model,
            maxTokens = maxTokens,
            messages = list,
            temperature = temperature
        )

        val future = CompletableFuture<String>()
        LarkSuite.ioScope.launch(Dispatchers.IO) {
            val jsonBody = json.encodeToString(requestBody)

            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $API_KEY")
                .post(RequestBody.create(mediaType, jsonBody))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body()!!.string()
                    val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBody)

                    // 提取回复内容
                    val reply = openAIResponse.choices.first().message
                    npcChatCache.put("$player@$npc", (list + SendMessage(reply.role, reply.content, npc)).toMutableList())
                    future.complete(reply.content)
                    debug("完成了一次 AI 调用，本次消耗 ${openAIResponse.usage.totalTokens} Token （ 用户侧: ${openAIResponse.usage.promptTokens}， AI侧: ${openAIResponse.usage.completionTokens} ）")
                } else {
                    future.complete("请求失败，请重试")
                }
            } catch (e: Exception) {
                future.completeExceptionally(e)
                e.printStackTrace()
            }
        }
        return future
    }

}