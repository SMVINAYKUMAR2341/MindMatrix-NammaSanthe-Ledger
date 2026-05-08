package com.nammasanthe.ledger.gemini

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gemini API service for advanced OCR and translation.
 * Requires API key from https://ai.google.dev/
 */
class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
        
        val DEFAULT_OCR_PROMPT = """
            Analyze this image containing Kannada handwritten text (likely a bill or ledger entry).
            
            Extract and return:
            1. KANNADA TEXT: The original Kannada text exactly as written
            2. ENGLISH TRANSLATION: Translate the Kannada text to English
            3. ITEMS: List each item with quantity and amount in format:
               - Item Name | Quantity | Amount
            
            Example format:
            KANNADA:
            [Kannada text here]
            
            ENGLISH:
            [English translation here]
            
            ITEMS:
            - Onion | 1kg | 50
            - Tomato | 500g | 40
            - Potato | 2kg | 60
            
            Be precise with numbers and amounts.
        """.trimIndent()
    }

    /**
     * Perform OCR on a bitmap image using Gemini.
     * Extracts Kannada text and provides translation.
     */
    suspend fun performOcr(
        bitmap: Bitmap,
        apiKey: String,
        prompt: String = DEFAULT_OCR_PROMPT
    ): GeminiOcrResult = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)
            
            val requestBody = buildOcrRequest(base64Image, prompt)
            
            val request = Request.Builder()
                .url("$GEMINI_API_URL?key=$apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw IOException("Gemini API error: ${response.code} - $errorBody")
            }

            val responseBody = response.body?.string()
                ?: throw IOException("Empty response from Gemini API")

            parseOcrResponse(responseBody)
            
        } catch (e: Exception) {
            GeminiOcrResult(
                success = false,
                error = e.message ?: "Unknown error",
                rawText = "",
                translatedText = "",
                items = emptyList()
            )
        }
    }

    /**
     * Validate if an API key is working.
     */
    suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val testRequest = buildOcrRequest("", "Test")
            val request = Request.Builder()
                .url("$GEMINI_API_URL?key=$apiKey")
                .post(testRequest.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful || response.code == 400 // 400 means key works but request was bad
        } catch (e: Exception) {
            false
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun buildOcrRequest(base64Image: String, prompt: String): String {
        val parts = mutableListOf<ContentPart>()
        
        // Add text prompt
        parts.add(ContentPart(text = prompt))
        
        // Add image if provided
        if (base64Image.isNotBlank()) {
            parts.add(ContentPart(
                inlineData = InlineData(
                    mimeType = "image/jpeg",
                    data = base64Image
                )
            ))
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = parts)
            ),
            generationConfig = GenerationConfig(
                temperature = 0.1,
                maxOutputTokens = 2048
            )
        )

        return json.encodeToString(request)
    }

    private fun parseOcrResponse(responseBody: String): GeminiOcrResult {
        return try {
            val response = json.decodeFromString<GeminiResponse>(responseBody)
            val text = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()
                ?.text ?: ""

            // Parse the response to extract Kannada text, translation, and items
            parseOcrText(text)
        } catch (e: Exception) {
            GeminiOcrResult(
                success = false,
                error = "Failed to parse response: ${e.message}",
                rawText = "",
                translatedText = "",
                items = emptyList()
            )
        }
    }

    private fun parseOcrText(text: String): GeminiOcrResult {
        // Try to extract structured data from Gemini response
        val lines = text.lines()
        
        var kannadaText = ""
        var englishText = ""
        val items = mutableListOf<BillItem>()
        
        var inKannadaSection = false
        var inEnglishSection = false
        var inItemsSection = false

        for (line in lines) {
            val trimmed = line.trim()
            
            when {
                trimmed.contains("KANNADA", ignoreCase = true) || 
                trimmed.contains("Original", ignoreCase = true) -> {
                    inKannadaSection = true
                    inEnglishSection = false
                    inItemsSection = false
                }
                trimmed.contains("ENGLISH", ignoreCase = true) || 
                trimmed.contains("Translation", ignoreCase = true) -> {
                    inKannadaSection = false
                    inEnglishSection = true
                    inItemsSection = false
                }
                trimmed.contains("ITEMS", ignoreCase = true) || 
                trimmed.contains("BILL", ignoreCase = true) -> {
                    inKannadaSection = false
                    inEnglishSection = false
                    inItemsSection = true
                }
                trimmed.startsWith("-") || trimmed.startsWith("=") || trimmed.isBlank() -> {
                    // Separator or blank
                }
                else -> {
                    when {
                        inKannadaSection -> kannadaText += " $trimmed"
                        inEnglishSection -> englishText += " $trimmed"
                        inItemsSection -> {
                            // Try to parse item: qty - item - amount
                            val item = parseItemLine(trimmed)
                            if (item != null) items.add(item)
                        }
                        else -> {
                            // Auto-detect based on content
                            if (trimmed.any { it.code in 0x0C80..0xCFF }) {
                                kannadaText += " $trimmed"
                            } else {
                                val item = parseItemLine(trimmed)
                                if (item != null) items.add(item)
                            }
                        }
                    }
                }
            }
        }

        return GeminiOcrResult(
            success = true,
            rawText = kannadaText.trim(),
            translatedText = englishText.trim(),
            items = items,
            fullResponse = text
        )
    }

    private fun parseItemLine(line: String): BillItem? {
        // Try to extract item name, quantity, and amount from a line
        // Formats: "Onion 1kg Rs.50", "Onion - 1kg - 50", "1. Onion (1kg) - 50"
        
        val amountRegex = Regex("""(?:Rs\.?|₹|INR)?\s*(\d+(?:\.\d+)?)\s*$""")
        val qtyRegex = Regex("""(\d+(?:\.\d+)?)\s*(kg|g|gm|pcs|pc|ltr|ml|dozen|dz)?""")
        
        val amountMatch = amountRegex.find(line)
        val amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull()?.toInt() ?: 0
        
        val qtyMatch = qtyRegex.find(line)
        val qty = qtyMatch?.value ?: "unknown"
        
        // Extract item name - remove amount, qty, and numbers
        var itemName = line
            .replace(amountMatch?.value ?: "", "")
            .replace(qtyMatch?.value ?: "", "")
            .replace(Regex("""^\d+\.?\s*"""), "") // Remove leading numbers (1. Item)
            .replace(Regex("""[-|]"""), " ")
            .trim()
        
        if (itemName.isBlank() && amount == 0) return null
        
        return BillItem(
            name = itemName.ifBlank { "Unknown" },
            quantity = qty,
            amount = amount
        )
    }
}

// Data classes for Gemini API

@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig
)

@Serializable
data class Content(
    val parts: List<ContentPart>
)

@Serializable
data class ContentPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val temperature: Double,
    val maxOutputTokens: Int
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class Candidate(
    val content: ResponseContent
)

@Serializable
data class ResponseContent(
    val parts: List<ResponsePart>
)

@Serializable
data class ResponsePart(
    val text: String
)

@Serializable
data class GeminiError(
    val code: Int,
    val message: String,
    val status: String
)

// Result data class

data class GeminiOcrResult(
    val success: Boolean,
    val rawText: String = "",
    val translatedText: String = "",
    val items: List<BillItem> = emptyList(),
    val error: String? = null,
    val fullResponse: String? = null
)

data class BillItem(
    val name: String,
    val quantity: String,
    val amount: Int
)
