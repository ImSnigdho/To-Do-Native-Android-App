package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NlpParsedTask(
    val title: String,
    val dueDate: Long? = null,    // Timestamp in ms
    val dueTime: String? = null,    // "HH:mm" format
    val priority: Int = 4           // P1 (1) to P4 (4)
)

// --- Moshi Parsing Data Classes ---

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

private interface GeminiNlpService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun parseTask(
        @Query("key") apiKey: String,
        @Body request: okhttp3.RequestBody
    ): ResponseBody
}

object NlpParser {
    private const val TAG = "NlpParser"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val formatService: GeminiNlpService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
            .create(GeminiNlpService::class.java)
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Parses a local natural language query. First tries Gemini if an API key is present.
     * Falls back to a robust local rule-based extractor if offline or if the API key is empty/unused.
     */
    suspend fun parse(input: String): NlpParsedTask {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return NlpParsedTask("")

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && !apiKey.startsWith("PLACEHOLDER")) {
            try {
                return parseWithGemini(trimmed, apiKey)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini NLP failed, falling back to local parsing: ${e.message}")
            }
        }

        return parseLocally(trimmed)
    }

    /**
     * Call Gemini directly using REST with raw Request/Response strings to parse user text.
     */
    private suspend fun parseWithGemini(input: String, apiKey: String): NlpParsedTask {
        val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentDayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(Date())

        val systemPrompt = """
            You are an NLP parsing system for an Android To-Do App. 
            Parse the user's natural text into a structured task. 
            The current date is $currentDateStr ($currentDayOfWeek).
            Extract the following fields in strict JSON format:
            - title: The plain text task description WITHOUT date, time or priority words. (string)
            - dueDate: Milliseconds epoch timestamp for the logical start of the day. (Long epoch millis). If 'tomorrow', calculate tomorrow's date. If next week, next monday, etc., calculate properly. If no date mentioned, return null.
            - dueTime: Standard clock time in 'HH:mm' format (e.g. '17:00' for 5 PM). Null if not mentioned.
            - priority: Integer priority level where 1=P1 (highest), 2=P2 (orange), 3=P3 (yellow), 4=P4 (default/no priority). Look for urgent/p1/high -> 1, p2/medium -> 2, p3/low -> 3, otherwise 4.
            
            Return JSON in this exact structure:
            {
               "title": "Clean room",
               "dueDate": 1779878400000,
               "dueTime": "15:30",
               "priority": 2
            }
            Ensure the output contains ONLY valid, parseable JSON and no additional Markdown formatting.
        """.trimIndent()

        // Double quote escaping for inner prompt string safety
        val escapedInput = input.replace("\"", "\\\"")
        val escapedSystemPrompt = systemPrompt.replace("\"", "\\\"").replace("\n", "\\n")

        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": "Parse: $escapedInput" }
                  ]
                }
              ],
              "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.1
              },
              "systemInstruction": {
                "parts": [
                  { "text": "$escapedSystemPrompt" }
                ]
              }
            }
        """.trimIndent()

        val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())
        val responseBody = formatService.parseTask(apiKey, requestBody)
        val responseText = responseBody.string()

        Log.d(TAG, "Gemini raw response: $responseText")

        val responseAdapter = moshi.adapter(GeminiResponse::class.java)
        val geminiResponse = responseAdapter.fromJson(responseText)
            ?: throw IllegalStateException("Failed to parse Gemini response with Moshi")

        val parsedJsonText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty parsed text candidates from Gemini")

        Log.d(TAG, "Gemini extracted JSON text: $parsedJsonText")

        val taskAdapter = moshi.adapter(NlpParsedTask::class.java)
        return taskAdapter.fromJson(parsedJsonText) 
            ?: throw IllegalStateException("Failed to parse NlpParsedTask from extracted text")
    }

    /**
     * Robust local fallback parsing logic using regex and keyword tracking.
     */
    fun parseLocally(input: String): NlpParsedTask {
        var remainingText = input
        var dueDate: Long? = null
        var dueTime: String? = null
        var priority = 4

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // 1. Check for priority tags
        val p1Regex = Regex("(?i)\\b(p1|urgent|asap|critical)\\b")
        val p2Regex = Regex("(?i)\\b(p2|high priority|important)\\b")
        val p3Regex = Regex("(?i)\\b(p3|medium priority|low)\\b")
        val p4Regex = Regex("(?i)\\b(p4|none)\\b")

        if (p1Regex.containsMatchIn(remainingText)) {
            priority = 1
            remainingText = remainingText.replace(p1Regex, "")
        } else if (p2Regex.containsMatchIn(remainingText)) {
            priority = 2
            remainingText = remainingText.replace(p2Regex, "")
        } else if (p3Regex.containsMatchIn(remainingText)) {
            priority = 3
            remainingText = remainingText.replace(p3Regex, "")
        } else if (p4Regex.containsMatchIn(remainingText)) {
            priority = 4
            remainingText = remainingText.replace(p4Regex, "")
        }

        // 2. Check for dates
        val todayRegex = Regex("(?i)\\b(today)\\b")
        val tomorrowRegex = Regex("(?i)\\b(tomorrow)\\b")
        val nextWeekRegex = Regex("(?i)\\b(next week)\\b")

        if (tomorrowRegex.containsMatchIn(remainingText)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dueDate = calendar.timeInMillis
            remainingText = remainingText.replace(tomorrowRegex, "")
        } else if (todayRegex.containsMatchIn(remainingText)) {
            dueDate = todayStart
            remainingText = remainingText.replace(todayRegex, "")
        } else if (nextWeekRegex.containsMatchIn(remainingText)) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            dueDate = calendar.timeInMillis
            remainingText = remainingText.replace(nextWeekRegex, "")
        } else {
            // Day of week check (e.g. Monday, Friday, etc.)
            val daysOfWeek = listOf(
                "sunday" to Calendar.SUNDAY,
                "monday" to Calendar.MONDAY,
                "tuesday" to Calendar.TUESDAY,
                "wednesday" to Calendar.WEDNESDAY,
                "thursday" to Calendar.THURSDAY,
                "friday" to Calendar.FRIDAY,
                "saturday" to Calendar.SATURDAY
            )
            for ((dayName, dayConstant) in daysOfWeek) {
                val dayRegex = Regex("(?i)\\b(on $dayName|next $dayName|$dayName)\\b")
                if (dayRegex.containsMatchIn(remainingText)) {
                    val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                    var daysToAdd = dayConstant - currentDay
                    if (daysToAdd <= 0) {
                        daysToAdd += 7 // Schedule for next week
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                    dueDate = calendar.timeInMillis
                    remainingText = remainingText.replace(dayRegex, "")
                    break
                }
            }
        }

        // 3. Check for times
        // Match expressions like "at 5 pm", "at 5pm", "at 17:00", "at 9 am"
        val timeRegex = Regex("(?i)\\b(at|around)?\\s*(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?\\b")
        val matchResult = timeRegex.find(remainingText)
        if (matchResult != null) {
            val hourStr = matchResult.groupValues[2]
            var hour = hourStr.toIntOrNull() ?: 12
            val minuteStr = matchResult.groupValues[4]
            val minute = if (minuteStr.isNotEmpty()) minuteStr.toIntOrNull() ?: 0 else 0
            val amPm = matchResult.groupValues[5].lowercase(Locale.ROOT)

            if (amPm == "pm" && hour < 12) {
                hour += 12
            } else if (amPm == "am" && hour == 12) {
                hour = 0
            }

            dueTime = String.format(Locale.US, "%02d:%02d", hour, minute)
            remainingText = remainingText.replace(timeRegex, "")
        }

        // Clean up title text (remove multiple spaces, trailing " at ", etc.)
        var title = remainingText.replace(Regex("\\s+"), " ").trim()
        if (title.endsWith(" at", ignoreCase = true)) {
            title = title.substring(0, title.length - 3).trim()
        }
        if (title.endsWith(" on", ignoreCase = true)) {
            title = title.substring(0, title.length - 3).trim()
        }

        if (title.isEmpty()) {
            title = input // Safe fallback
        }

        return NlpParsedTask(
            title = title,
            dueDate = dueDate,
            dueTime = dueTime,
            priority = priority
        )
    }
}
