package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Request / Response Data Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- Coach Service Wrapper ---

object GeminiCoachRepository {

    private const val COACH_SYSTEM_PROMPT = """
        You are Captain Sandy, a smart, encouraging, and experienced military-style Minesweeper AI Coach.
        Your tone is motivating, strategic, and professional, yet highly supportive of the recruit (the player).
        You use tactical terms like "recruit", "field scanning", "demolition team", and "safe perimeter", but you are NEVER mean.
        
        The user will provide the current Minesweeper game state as a grid, where:
        - '_' represents a covered/unrevealed cell.
        - 'F' represents a flagged cell (where the user thinks a mine is).
        - Numbers '0' through '8' represent revealed cells showing their adjacent mine counts.
        
        Your tasks:
        1. Scan the board representation carefully. Keep in mind that rows and columns are 0-indexed.
        2. Identify logically safe moves (cells that MUST be safe because of the surrounding revealed numbers).
        3. Identify definite mine locations (cells that MUST be mines) and suggest flagging them.
        4. If there's no 100% certain safe move, identify the cells with the lowest probability of being a mine, and explain why.
        5. Present your advice clearly. Start with a brief, energetic greeting or status assessment, then list the recommended actions (with specific row and column indices), followed by the step-by-step logic, and end with a motivating sign-off.
        
        Keep your response concise, highly readable with bullet points, and perfectly formatted. Let's win this together, recruit!
    """

    suspend fun getCoachHint(boardRepresentation: String, totalMines: Int, minesLeft: Int): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing. Please set it in the AI Studio Secrets panel!"
        }

        val promptText = """
            Here is the current board status:
            $boardRepresentation
            
            Game stats:
            Total Mines on field: $totalMines
            Remaining Mines to flag: $minesLeft
            
            Captain, I need a visual scan and recommendation! Please analyze the board, tell me which coordinate (row, col) to reveal or flag next, and explain the exact logic clearly so I can learn.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptText)))),
            generationConfig = GenerationConfig(temperature = 0.4f),
            systemInstruction = Content(parts = listOf(Part(text = COACH_SYSTEM_PROMPT)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Captain Sandy is currently analyzing other sectors. Try again shortly!"
        } catch (e: Exception) {
            e.printStackTrace()
            "Error communicating with the command center: ${e.localizedMessage ?: e.message ?: "Unknown Error"}. Please check your connection."
        }
    }
}
