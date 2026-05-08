package com.nammasanthe.ledger.ocr

import android.content.Context
import android.graphics.Bitmap
import com.nammasanthe.ledger.translation.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * OCR Pipeline — orchestrates ML Kit and Tesseract (SimpleKannadaOcr) for
 * optimal text extraction from bill/ledger images.
 *
 * Strategy:
 * - For Kannada: Run both ML Kit (numbers/prices) + Tesseract (Kannada text),
 *   then merge results for best coverage.
 * - For Hindi/English: ML Kit is primary, Tesseract is fallback.
 */
class OcrPipeline(context: Context) {

    private val mlKit = MlKitOcr()
    private val simpleOcr = SimpleKannadaOcr(context)
    private var preferredLanguage: OcrLanguage = OcrLanguage.UNKNOWN

    fun setPreferredLanguage(language: OcrLanguage) {
        preferredLanguage = language
    }

    suspend fun run(bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        // Step 1: Run ML Kit (fast, good for numbers & Latin text)
        val (mlText, mlConf) = runCatching { mlKit.recognize(bitmap, preferredLanguage) }
            .getOrDefault("" to 0f)

        var bestText = mlText
        var bestConf = mlConf
        var source = "ml-kit"
        var warning: String? = null
        val detected = detectLanguage(mlText)

        // Step 2: For Kannada or when ML Kit is insufficient, run Tesseract
        val needsSimpleOcr = preferredLanguage == OcrLanguage.KANNADA ||
            detected == OcrLanguage.KANNADA ||
            mlText.isBlank() ||
            mlConf < 0.5f

        if (needsSimpleOcr) {
            val sResult = runCatching { simpleOcr.recognize(bitmap, "kan+eng") }
            val (sText, sConf) = sResult.getOrDefault("" to 0f)
            if (sResult.isFailure) {
                val msg = sResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                warning = msg ?: "Simple OCR failed."
            }

            if (sText.isNotBlank()) {
                if (preferredLanguage == OcrLanguage.KANNADA || detected == OcrLanguage.KANNADA) {
                    // For Kannada: Merge ML Kit numbers with Tesseract Kannada text
                    bestText = mergeResults(mlText, sText)
                    bestConf = maxOf(mlConf, sConf)
                    source = "merged-ocr"
                } else if (sText.length > bestText.length || sConf > bestConf) {
                    bestText = sText
                    bestConf = sConf
                    source = "simple-ocr"
                }
            }
        }

        val finalLang = if (preferredLanguage != OcrLanguage.UNKNOWN) preferredLanguage
                        else detectLanguage(bestText)
        val amount = extractAmount(bestText)
        val date = parseDate(bestText)

        OcrResult(bestText.trim(), finalLang, bestConf, source, amount, date, warning)
    }

    /**
     * Merge ML Kit result (good for numbers/English) with Tesseract result
     * (good for Kannada script). Keeps the Tesseract text as base but
     * supplements with any prices/amounts ML Kit found that Tesseract missed.
     */
    private fun mergeResults(mlKitText: String, tesseractText: String): String {
        if (mlKitText.isBlank()) return tesseractText
        if (tesseractText.isBlank()) return mlKitText

        // If Tesseract result already has good numbers, just use it
        val tessAmounts = AMOUNT_REGEX.findAll(tesseractText).count()
        val mlAmounts = AMOUNT_REGEX.findAll(mlKitText).count()

        if (tessAmounts >= mlAmounts) return tesseractText

        // Tesseract has Kannada text but is missing prices — append ML Kit amounts
        val mlAmountLines = mlKitText.lines().filter { line ->
            AMOUNT_REGEX.containsMatchIn(line)
        }

        return if (mlAmountLines.isNotEmpty()) {
            tesseractText + "\n" + mlAmountLines.joinToString("\n")
        } else {
            tesseractText
        }
    }

    fun close() {
        mlKit.close()
        simpleOcr.close()
    }

    companion object {
        // Enhanced amount regex: handles Rs., ₹, INR, plain numbers, and x/- format
        private val AMOUNT_REGEX = Regex(
            """(?i)(?:rs\.?\s*|₹\s*|inr\s*)?(\d{1,3}(?:[,\d]{0,9})(?:\.\d{1,2})?)\s*(?:/[-−])?"""
        )

        fun detectLanguage(text: String): OcrLanguage {
            if (text.isBlank()) return OcrLanguage.UNKNOWN
            val kannada = text.count { it.code in 0x0C80..0x0CFF }
            val devanagari = text.count { it.code in 0x0900..0x097F }
            val latin = text.count { it.isLetter() && it.code < 0x80 }
            return when {
                kannada > 0 && kannada >= devanagari && kannada >= latin -> OcrLanguage.KANNADA
                devanagari > 0 && devanagari >= latin -> OcrLanguage.HINDI
                latin > 0 -> OcrLanguage.ENGLISH
                else -> OcrLanguage.UNKNOWN
            }
        }

        /**
         * Extract the most likely total amount from OCR text.
         * Priority: explicit total line > largest number > last number.
         */
        private fun extractAmount(text: String): Double? {
            // First check for explicit "total" lines
            val totalLine = text.lines().firstOrNull { line ->
                line.contains("total", ignoreCase = true) ||
                line.contains("ಒಟ್ಟು", ignoreCase = true) || // Kannada "total"
                line.contains("ಮೊತ್ತ", ignoreCase = true)    // Kannada "amount"
            }

            if (totalLine != null) {
                val amount = AMOUNT_REGEX.find(totalLine)
                    ?.groupValues?.get(1)
                    ?.replace(",", "")
                    ?.toDoubleOrNull()
                if (amount != null && amount > 0) return amount
            }

            // Fallback: find the largest reasonable amount
            val allAmounts = AMOUNT_REGEX.findAll(text)
                .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .filter { it > 0 && it < 100000 } // Reasonable range for bills
                .toList()

            return allAmounts.maxOrNull()
        }

        private val DATE_FORMATS = listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd MMM yyyy")

        private fun parseDate(text: String): Long? {
            val match = Regex("""\b(\d{1,2}[\-/ ]\d{1,2}[\-/ ]\d{2,4})\b""").find(text) ?: return null
            for (fmt in DATE_FORMATS) {
                try {
                    val parsed = SimpleDateFormat(fmt, Locale.ENGLISH).parse(match.value) ?: continue
                    return parsed.time
                } catch (_: Throwable) {}
            }
            return null
        }
    }

    suspend fun translateIfNeeded(
        result: OcrResult,
        translator: TranslationManager
    ): String? {
        if (result.rawText.isBlank()) return null
        // ML Kit on-device translation supports Hindi -> English. Kannada is not in ML Kit's
        // language set, so for Kannada we leave the original text and let the user edit.
        val src = when (result.language) {
            OcrLanguage.HINDI -> "hi"
            else -> return null
        }
        return runCatching { translator.translateIfDownloaded(result.rawText, src, "en") }.getOrNull()
    }
}
