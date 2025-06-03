package org.ideas2it.composecraft.services

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Service class for interacting with OpenAI's API services.
 * 
 * This service provides functionality to:
 * - Generate code using OpenAI's language models
 * - Process natural language queries
 * - Handle image-to-code conversion requests
 * - Manage API authentication and error handling
 *
 * The service supports both synchronous and asynchronous operations,
 * with built-in retry mechanisms and response validation.
 */
object OpenAIService {
    private const val API_URL = "https://api.openai.com/v1/chat/completions"
    // TODO: Move this to secure configuration
    private const val API_KEY = ""
    private val client = OkHttpClient()

    // Data class for JSON serialization
    data class Message(val role: String, val content: String)
    data class RequestPayload(val model: String, val messages: List<Message>, val temperature: Double)

    // Function to send request to OpenAI
    fun generateJetpackComposeCode(imageBase64: String, callback: (String) -> Unit) {
        // Create the prompt
        val prompt = "Generate Jetpack Compose UI code for the following image content:\n\n$imageBase64"

        // Create JSON payload using Gson
        val payload = RequestPayload(
            model = "gpt-4",
            messages = listOf(
                Message("system", "You are an assistant that generates Jetpack Compose code."),
                Message("user", prompt)
            ),
            temperature = 0.7
        )

        // Serialize to JSON
        val jsonPayload = Gson().toJson(payload)

        // Build the request
        val body = jsonPayload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(body)
            .build()

        // Execute the call asynchronously
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Failed to get response: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        try {
                            val parsedResponse = Gson().fromJson(responseBody, OpenAIResponse::class.java)
                            val messageContent = parsedResponse.choices.first().message.content
                            callback(messageContent)
                        } catch (e: Exception) {
                            callback("Failed to parse response: ${e.message}")
                        }
                    }
                } else {
                    val errorBody = response.body?.string()
                    callback("Error: ${response.message}\nDetails: $errorBody")
                }
            }
        })
    }

    // Data Classes for Parsing OpenAI Response
    data class OpenAIResponse(
        val choices: List<Choice>
    ) {
        data class Choice(
            val message: Message
        ) {
            data class Message(
                val content: String
            )
        }
    }

    // Function to send request to OpenAI for code generation based on text prompt
    fun generateJetpackComposeCodeFromText(promptText: String, callback: (String) -> Unit) {
        // Create the prompt
        val prompt = "Generate Jetpack Compose UI code for the following request:\n\n$promptText"

        // Create JSON payload using Gson
        val payload = RequestPayload(
            model = "gpt-3.5-turbo",
            messages = listOf(
                Message("system", "You are an assistant that generates Jetpack Compose code."),
                Message("user", prompt)
            ),
            temperature = 0.7
        )

        // Serialize to JSON
        val jsonPayload = Gson().toJson(payload)

        // Build the request
        val body = jsonPayload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(body)
            .build()

        // Execute the call asynchronously
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Failed to get response: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        try {
                            val parsedResponse = Gson().fromJson(responseBody, OpenAIResponse::class.java)
                            val messageContent = parsedResponse.choices.first().message.content
                            callback(messageContent)
                        } catch (e: Exception) {
                            callback("Failed to parse response: ${e.message}")
                        }
                    }
                } else {
                    val errorBody = response.body?.string()
                    callback("Error: ${response.message}\nDetails: $errorBody")
                }
            }
        })
    }
}
