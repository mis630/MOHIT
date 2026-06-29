package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

data class GeminiPart(
    @Json(name = "text") val text: String
)

data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiResponseFormatText(
    @Json(name = "mimeType") val mimeType: String
)

data class GeminiResponseFormat(
    @Json(name = "text") val text: GeminiResponseFormatText
)

data class GeminiGenerationConfig(
    @Json(name = "responseFormat") val responseFormat: GeminiResponseFormat? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent
)

data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

// Structural Output class
data class ParsedInvoiceItem(
    @Json(name = "itemName") val itemName: String = "",
    @Json(name = "brandName") val brandName: String = "",
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "rate") val rate: Double = 0.0
)

data class ParsedInvoiceResponse(
    @Json(name = "customerName") val customerName: String? = null,
    @Json(name = "customerAddress") val customerAddress: String? = null,
    @Json(name = "customerMobile") val customerMobile: String? = null,
    @Json(name = "items") val items: List<ParsedInvoiceItem>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun parseVoiceTranscript(transcript: String): ParsedInvoiceResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return null
        }

        val systemInstruction = "You are the billing assistant of 'Shree Ram Trader' Jamshedpur, Jharkhand. " +
                "Parse the unstructured spoken invoice/billing text. " +
                "Extract: customerName, customerAddress, customerMobile, and a list of items (itemName, brandName, quantity, rate). " +
                "Brand matching logic: Match shoe brands exactly to ('Acme', 'Bata', 'Image', 'Hillson', 'local') " +
                "or goggles brands ('Himalayan Plastic', 'Himalayan Fiber', 'local') or gloves ('Rubber', 'Leather', 'local'). " +
                "For accessories (Socks, Shoe Polish, Shoelaces), mark brandName as empty or local. " +
                "If no brand is explicitly mentioned but it's safety shoes, default brandName to 'local'. " +
                "Parse numerical quantities/rates cleanly. " +
                "Output strictly valid JSON complying with this structure: " +
                "{\"customerName\": \"Name\", \"customerAddress\": \"Address\", \"customerMobile\": \"Mobile\", \"items\": " +
                "[{\"itemName\": \"Product\", \"brandName\": \"Brand\", \"quantity\": 1, \"rate\": 120.0}]}"

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = "Transcript: $transcript")))),
            generationConfig = GeminiGenerationConfig(
                responseFormat = GeminiResponseFormat(
                    text = GeminiResponseFormatText(mimeType = "application/json")
                ),
                temperature = 0.1f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction)))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                moshi.adapter(ParsedInvoiceResponse::class.java).fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
