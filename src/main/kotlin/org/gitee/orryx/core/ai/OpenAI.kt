package org.gitee.orryx.core.ai

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.lark.oapi.okhttp.MediaType
import com.lark.oapi.okhttp.OkHttpClient
import com.lark.oapi.okhttp.Request
import com.lark.oapi.okhttp.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.api.OrryxAPI
import java.util.concurrent.TimeUnit


object OpenAI {

    private val client: OkHttpClient by lazy { OkHttpClient().newBuilder().build() }

    private val npcChatCache = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .scheduler(Scheduler.systemScheduler())
        .build<String, MutableList<Message>> {
            mutableListOf()
        }

    // 请求参数模型
    @Serializable
    data class OpenAIRequest(
        val model: String,
        @SerialName("max_tokens")
        val maxTokens: Int,
        val messages: List<Message>,
        val temperature: Double = 1.0
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String,
        val name: String?,
    )

    // 响应解析模型
    @Serializable
    data class OpenAIResponse(
        val id: String,
        val choices: List<Choice>
    )

    @Serializable
    data class Choice(
        val message: Message,
        @SerialName("finish_reason")
        val finishReason: String
    )

    private val API_KEY
        get() = OrryxAPI.config.getString("OpenAI.ApiKey") ?: error("未配置OpenAI ApiKey")

    private val BASE_URL
        get() = OrryxAPI.config.getString("OpenAI.BaseUrl") ?: error("未配置OpenAI BaseUrl")

    fun npcChat(player: String, npc: String, npcDescription: String, message: String, model: String, maxTokens: Int, temperature: Double): String? {
        val mediaType = MediaType.parse("application/json")

        val context = npcChatCache.get("$player@$npc")

        val list = mutableListOf<Message>()

        list += Message(role = "system", content = npcDescription, name = npc)
        context?.forEach {
            list += it
        }
        val userMessage = Message(role = "user", content = message, name = player)
        list += userMessage

        val requestBody = OpenAIRequest(
            model = model,
            maxTokens = maxTokens,
            messages = list,
            temperature = temperature
        )

        val jsonBody = Json.encodeToString(requestBody)

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
                val openAIResponse = Json.decodeFromString<OpenAIResponse>(responseBody)

                // 提取回复内容
                val reply = openAIResponse.choices.first().message
                npcChatCache.put("$player@$npc", (list + reply).toMutableList())
                return reply.content
            } else {
                println("请求失败: ${response.code()} - ${response.message()}")
            }
        } catch (_: Exception) {
        }
        return null
    }

}