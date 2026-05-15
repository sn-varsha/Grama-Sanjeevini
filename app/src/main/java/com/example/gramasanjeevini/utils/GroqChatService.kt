package com.example.gramasanjeevini.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Groq AI chat service for the Grama Sanjeevini symptom checker.
 *
 * Uses Groq's FREE inference API (https://console.groq.com) to run
 * LLaMA 3.3 70B — no Google Cloud or billing required.
 *
 * Free tier limits: 30 requests/min, 14,400 requests/day.
 *
 * API key setup:
 *   1. Sign up free at https://console.groq.com
 *   2. Create an API Key
 *   3. Add  GROQ_API_KEY=gsk_xxx  to local.properties
 *   4. Clean build: ./gradlew clean assembleDebug
 */
object GroqChatService {

    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"

    // llama-3.3-70b-versatile gives high-quality medical responses
    private const val MODEL = "llama-3.3-70b-versatile"

    // Focused, concise system prompt for rural India health assistant
    private const val SYSTEM_PROMPT = """You are Grama Sanjeevini AI — a friendly health assistant for rural villages in India. Always reply in clear, simple English that someone with basic literacy can understand.

Your job:
- Listen to the user's symptoms and give practical, specific advice.
- Suggest over-the-counter medicines available at local pharmacies (paracetamol, ORS, antacids, cetirizine, ibuprofen, cough syrup, etc.) with dosage when appropriate.
- Mention simple home remedies (ginger tea, steam inhalation, turmeric milk, etc.) when suitable.
- Tell the user exactly when they MUST see a doctor.
- For emergencies (chest pain, breathing difficulty, severe bleeding, fits/seizures, unconsciousness, snake bite, high fever in infants), immediately say: "⚠️ EMERGENCY — Call 108 now or go to the nearest hospital immediately."

Response format:
1. What you likely have (brief)
2. What to do right now (home care + medicine if safe)
3. When to see a doctor
4. Any warning signs to watch for

Keep responses under 200 words. Be warm and reassuring. Do NOT give the same generic answer — always respond to the specific symptoms mentioned.

End every response with: "💊 This is general guidance only. For serious symptoms, visit your nearest PHC or call 108." """

    /**
     * Sends conversation history to Groq and returns the AI reply.
     *
     * @param conversationHistory  Full list of (role, content) pairs so far
     * @param apiKey               BuildConfig.GROQ_API_KEY from local.properties
     * @return Result with the reply string, or an error
     */
    suspend fun chat(
        conversationHistory: List<Pair<String, String>>,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {

        // Guard: key not configured
        if (apiKey.isBlank() || apiKey == "your_groq_api_key_here") {
            return@withContext Result.failure(
                IllegalStateException(
                    "GROQ_API_KEY not set. " +
                    "Get your free key at https://console.groq.com " +
                    "and add it to local.properties"
                )
            )
        }

        try {
            // Build messages array
            val messagesArray = JSONArray()

            // System message first
            messagesArray.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                }
            )

            // Conversation history (keep last 10 turns to stay within context limit)
            val recentHistory = if (conversationHistory.size > 20) {
                conversationHistory.takeLast(20)
            } else {
                conversationHistory
            }
            for ((role, content) in recentHistory) {
                messagesArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    }
                )
            }

            // Request body
            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("messages", messagesArray)
                put("max_tokens", 500)
                put("temperature", 0.6)  // slightly lower for more consistent medical advice
                put("top_p", 0.9)
            }.toString()

            // HTTP POST
            val url = URL(ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 15_000
                readTimeout    = 30_000
                doOutput       = true
            }

            connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode

            if (responseCode != 200) {
                val errorText = runCatching {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                }.getOrDefault("HTTP $responseCode")

                val friendlyError = when (responseCode) {
                    401 -> "Invalid API key. Please check the key in local.properties and rebuild."
                    429 -> "Rate limit reached. Please wait a moment and try again."
                    503 -> "Groq servers are temporarily unavailable. Try again shortly."
                    else -> "Server error $responseCode: ${parseErrorMessage(errorText)}"
                }
                return@withContext Result.failure(Exception(friendlyError))
            }

            val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }

            // Parse JSON response
            val json    = JSONObject(responseText)
            val choices = json.getJSONArray("choices")
            val reply   = choices
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            Result.success(reply)

        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Request timed out. Check your connection and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String): String {
        return try {
            JSONObject(errorBody).optJSONObject("error")?.optString("message") ?: errorBody
        } catch (_: Exception) {
            errorBody.take(200)
        }
    }
}
