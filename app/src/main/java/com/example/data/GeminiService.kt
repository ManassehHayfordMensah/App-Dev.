package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Moshi Serialized Data Classes ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @Json(name = "thinkingLevel") val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Veo Video Generation Request ---

@JsonClass(generateAdapter = true)
data class VeoConfig(
    @Json(name = "numberOfVideos") val numberOfVideos: Int,
    @Json(name = "resolution") val resolution: String,
    @Json(name = "aspectRatio") val aspectRatio: String
)

@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "config") val config: VeoConfig? = null
)

// --- Retrofit API Service ---

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/{model}:generateVideos")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateVideosRequest
    ): ResponseBody
}

// --- Chat Message Model helper for UI ---
data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    // Access API Key from BuildConfig
    private val apiKey: String
        get() = com.example.BuildConfig.GEMINI_API_KEY

    /**
     * Send a general conversational text prompt to Gemini.
     * Supports multi-turn chat history.
     */
    suspend fun generateText(
        model: String = "gemini-3.5-flash",
        prompt: String,
        systemInstruction: String? = null,
        history: List<ChatMessage> = emptyList(),
        useThinking: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "[Developer Notice: Gemini API Key is currently empty or placeholder. To experience full conversational AI, please add a valid key via the Secrets panel.]\n\nFallback Answer:\nWelcome to SkillJet Support! Ghana's tradespeople (plumbers, mechanics, electricians) are key to Adenta and Greater Accra. If you need local advice, you can connect with Kwame or Kofi in our directory."
        }

        val contents = mutableListOf<Content>()
        
        // Add chat history
        history.forEach { msg ->
            val role = if (msg.sender == "USER") "user" else "model"
            contents.add(
                Content(
                    role = role,
                    parts = listOf(Part(text = msg.text))
                )
            )
        }
        
        // Add current prompt
        contents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = prompt))
            )
        )

        val sysInst = systemInstruction?.let {
            Content(parts = listOf(Part(text = it)))
        }

        val config = if (useThinking) {
            GenerationConfig(thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH"))
        } else {
            null
        }

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = sysInst,
            generationConfig = config
        )

        try {
            val response = api.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response returned from the model."
        } catch (e: Exception) {
            "API Call failed: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * Analyze an image (Bitmap) and provide insights or verification.
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String,
        model: String = "gemini-3.1-pro-preview"
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Analysis Mockup (Demo Mode):\nWe successfully scanned your landmark photo. Our AI verifies that this Melcom / Junction landmark is well-known and readable in our system. A skilled worker can find this easily!"
        }

        // Convert Bitmap to Base64
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                    )
                )
            )
        )

        try {
            val response = api.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response returned."
        } catch (e: Exception) {
            "Image analysis failed: ${e.localizedMessage}"
        }
    }

    /**
     * Generate an Image using gemini-3.1-flash-image-preview or gemini-3-pro-image-preview
     * and custom configurations.
     * Returns base64 image string or empty string on error.
     */
    suspend fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        size: String = "1K",
        model: String = "gemini-3.1-flash-image-preview"
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "" // Return empty to fallback to a beautiful preloaded or simulated thumbnail
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = size),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = api.generateContent(model, apiKey, request)
            // Look for inlineData or image part in the response
            val imagePart = response.candidates?.firstOrNull()?.content?.parts?.find { it.inlineData != null }
            imagePart?.inlineData?.data ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Generate videos using Veo veo-3.1-fast-generate-preview.
     * Returns response raw string.
     */
    suspend fun generateVideo(
        prompt: String,
        aspectRatio: String = "16:9",
        model: String = "veo-3.1-fast-generate-preview"
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Video Generation Mockup (Demo Mode):\nWe initiated the video generation flow for prompt: \"$prompt\" with aspect ratio $aspectRatio. Video compiled in high quality!"
        }

        val request = GenerateVideosRequest(
            prompt = prompt,
            config = VeoConfig(
                numberOfVideos = 1,
                resolution = "720p",
                aspectRatio = aspectRatio
            )
        )

        try {
            val response = api.generateVideos(model, apiKey, request)
            response.string()
        } catch (e: Exception) {
            "Video generation failed: ${e.localizedMessage}"
        }
    }
}
