package org.ideas2it.composecraft.services

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Service class for interacting with Google's Gemini AI model.
 * 
 * This service provides functionality to:
 * - Generate Jetpack Compose code from images
 * - Process natural language queries about code
 * - Handle asynchronous communication with the Gemini API
 * - Manage API responses and error handling
 *
 * The service supports both image-to-code conversion and text-based
 * code generation/modification requests.
 */
object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun getApiUrl(modelName: String): String {
        println("Using Gemini model: $modelName") // Debug log
        return "$BASE_URL/$modelName:generateContent"
    }

    suspend fun generateJetpackComposeCode(prompt: String, modelName: String = "gemini-pro", apiKey: String): String {
        return withContext(Dispatchers.IO) {
            val payload = """
                {
                    "contents": [
                        {
                            "parts": [
                                {
                                    "text": "$prompt"
                                }
                            ]
                        }
                    ]
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("${getApiUrl(modelName)}?key=$apiKey")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("API call failed: ${response.code} ${response.message}")
                }
                
                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                parseGeminiResponse(responseBody)
            } catch (e: Exception) {
                throw IOException("Failed to generate code: ${e.message}", e)
            }
        }
    }

    suspend fun generateWithImage(prompt: String, imageData: String, modelName: String = "gemini-pro-vision", apiKey: String): String {
        return withContext(Dispatchers.IO) {
            val payload = """
                {
                    "contents": [
                        {
                            "parts": [
                                {
                                    "text": "$prompt"
                                },
                                {
                                    "inline_data": {
                                        "mime_type": "image/jpeg",
                                        "data": "$imageData"
                                    }
                                }
                            ]
                        }
                    ]
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("${getApiUrl(modelName)}?key=$apiKey")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("API call failed: ${response.code} ${response.message}")
                }
                
                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                parseGeminiResponse(responseBody)
            } catch (e: Exception) {
                throw IOException("Failed to generate code: ${e.message}", e)
            }
        }
    }

    private fun parseGeminiResponse(responseBody: String): String {
        try {
            val parsedResponse = Gson().fromJson(responseBody, GeminiResponse::class.java)
            if (parsedResponse.candidates.isNotEmpty()) {
                val content = parsedResponse.candidates[0].content
                if (content != null) {
                    return content.parts[0].text
                }
                throw IOException("No content found in response")
            }
            throw IOException("No candidates found in response")
        } catch (e: Exception) {
            throw IOException("Failed to parse response: ${e.message}", e)
        }
    }

    // Response data classes
    data class GeminiResponse(
        val candidates: List<Candidate>
    ) {
        data class Candidate(
            val content: Content?
        )

        data class Content(
            val parts: List<Part>
        )

        data class Part(
            val text: String
        )
    }
}
