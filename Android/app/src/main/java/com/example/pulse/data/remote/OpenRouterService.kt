package com.example.pulse.data.remote

import com.example.pulse.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Singleton
class OpenRouterService @Inject constructor(
    private val client: HttpClient
) {
    fun streamChat(
        messages: List<ChatMessage>,
        agentModel: String,
        systemPrompt: String?
    ): Flow<String> = flow {
        val payload = buildJsonObject {
            put("model", agentModel)
            put("messages", buildJsonArray {
                systemPrompt?.let {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", it)
                    })
                }
                messages.forEach { msg ->
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
            put("stream", true)
        }

        try {
            client.preparePost("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${BuildConfig.OPEN_ROUTER_API_KEY}")
                header(HttpHeaders.ContentType, "application/json")
                header("HTTP-Referer", "https://pulse-social.app")
                header("X-Title", "Pulse Social Android")
                setBody(payload)
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val element = Json.parseToJsonElement(data).jsonObject
                            val deltaContent = element["choices"]
                                ?.jsonArray?.firstOrNull()
                                ?.jsonObject?.get("delta")
                                ?.jsonObject?.get("content")
                                ?.jsonPrimitive?.content ?: ""
                            if (deltaContent.isNotEmpty()) {
                                emit(deltaContent)
                            }
                        } catch (e: Exception) {
                            // Ignore SSE malformed blocks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("Error connecting to OpenRouter: ${e.localizedMessage}")
        }
    }
}
