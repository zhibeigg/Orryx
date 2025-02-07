package org.gitee.orryx.core.ai

import com.google.gson.annotations.SerializedName
import com.lark.oapi.okhttp.MediaType
import com.lark.oapi.okhttp.OkHttpClient
import com.lark.oapi.okhttp.Request
import com.lark.oapi.okhttp.RequestBody
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.utils.gson


object OpenAI {

    private val client: OkHttpClient by lazy { OkHttpClient().newBuilder().build() }

    // 请求参数模型
    data class OpenAIRequest(
        val model: String,
        @SerializedName("max_tokens")
        val maxTokens: Int,
        val messages: List<Message>,
        val temperature: Double = 1.0
    )

    data class Message(
        val role: String,
        val content: String,
        val name: String?,
    )

    // 响应解析模型
    data class OpenAIResponse(
        val id: String,
        val choices: List<Choice>
    )

    data class Choice(
        val message: Message,
        @SerializedName("finish_reason")
        val finishReason: String
    )

    private val API_KEY
        get() = OrryxAPI.config.getString("OpenAI.ApiKey")

    private val BASE_URL
        get() = OrryxAPI.config.getString("OpenAI.BaseUrl")

    fun npcChat(player: String, npc: String, npcDescription: String, message: String, model: String, maxTokens: Int, temperature: Double): String? {
        val mediaType = MediaType.parse("application/json")

        val requestBody = OpenAIRequest(
            model = model,
            maxTokens = maxTokens,
            messages = listOf(Message(role = "system", content = npcDescription, name = npc), Message(role = "user", content = message, name = player)),
            temperature = temperature
        )

        val jsonBody = gson.toJson(requestBody)

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
                val responseBody = response.body()?.string()
                val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)

                // 提取回复内容
                val reply = openAIResponse.choices.first().message.content
                return reply
            } else {
                println("请求失败: ${response.code()} - ${response.message()}")
            }
        } catch (_: Exception) {
        }
        return null
    }

}