package com.ftt.aichat.api

import com.ftt.aichat.data.Message
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles all API communication with Anthropic's Claude.
 * Uses Server-Sent Events (SSE) for real-time streaming.
 */
class ClaudeApiService {

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val MAX_TOKENS = 8096
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Streams a chat completion from Claude API.
     *
     * @param apiKey    Your Anthropic API key (sk-ant-...)
     * @param model     Model ID string (e.g. "claude-sonnet-4-6")
     * @param messages  Full conversation history
     * @param systemPrompt  Optional system prompt
     * @param onToken   Called on each streamed text token (background thread)
     * @param onComplete Called when streaming finishes successfully
     * @param onError   Called on any error with a user-friendly message
     * @return OkHttp Call — save this to cancel on user request
     */
    fun streamChat(
        apiKey: String,
        model: String,
        messages: List<Message>,
        systemPrompt: String?,
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ): Call {
        // Convert to API format (only role + content)
        val apiMessages = messages
            .filter { !it.isStreaming && !it.isError }
            .map { mapOf("role" to it.role, "content" to it.content) }

        // Build request body
        val bodyMap = mutableMapOf<String, Any>(
            "model" to model,
            "max_tokens" to MAX_TOKENS,
            "stream" to true,
            "messages" to apiMessages
        )
        if (!systemPrompt.isNullOrBlank()) {
            bodyMap["system"] = systemPrompt
        }

        val requestBody = gson.toJson(bodyMap)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    onError("Connection failed: ${e.message ?: "Unknown network error"}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle HTTP error responses
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        val errorJson = gson.fromJson(errorBody, JsonObject::class.java)
                        errorJson.getAsJsonObject("error")?.get("message")?.asString
                            ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    onError(errorMsg)
                    return
                }

                // Parse SSE stream line by line
                try {
                    var hasCompleted = false
                    response.body?.charStream()?.buffered()?.forEachLine { line ->
                        // SSE lines start with "data: "
                        if (!line.startsWith("data: ")) return@forEachLine

                        val data = line.substring(6).trim()
                        if (data == "[DONE]") {
                            if (!hasCompleted) {
                                hasCompleted = true
                                onComplete()
                            }
                            return@forEachLine
                        }

                        try {
                            val json = gson.fromJson(data, JsonObject::class.java)
                            when (json.get("type")?.asString) {
                                "content_block_delta" -> {
                                    // Extract text delta
                                    val deltaType = json.getAsJsonObject("delta")
                                        ?.get("type")?.asString
                                    if (deltaType == "text_delta") {
                                        val text = json.getAsJsonObject("delta")
                                            ?.get("text")?.asString ?: ""
                                        if (text.isNotEmpty()) onToken(text)
                                    }
                                }
                                "message_stop" -> {
                                    if (!hasCompleted) {
                                        hasCompleted = true
                                        onComplete()
                                    }
                                }
                                "error" -> {
                                    val msg = json.getAsJsonObject("error")
                                        ?.get("message")?.asString ?: "Stream error"
                                    onError(msg)
                                }
                                else -> { /* ignore other event types */ }
                            }
                        } catch (_: Exception) {
                            // Skip malformed JSON events silently
                        }
                    }

                    // Ensure onComplete is always called even if [DONE] was missed
                    if (!hasCompleted) onComplete()

                } catch (e: Exception) {
                    if (!call.isCanceled()) {
                        onError("Stream error: ${e.message ?: "Unknown error"}")
                    }
                }
            }
        })

        return call
    }
}
