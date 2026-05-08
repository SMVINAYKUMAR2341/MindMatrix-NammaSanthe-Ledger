package com.nammasanthe.ledger.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit based OCR. Supports Latin + Devanagari scripts natively.
 *
 * For Kannada: ML Kit can't recognize Kannada script, but the Latin recognizer
 * can still extract numbers, prices (Rs.50, ₹100), and any English text on
 * mixed-language bills. This is valuable supplementary data for the pipeline.
 */
class MlKitOcr {
    private val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    suspend fun recognize(bitmap: Bitmap, preferLanguage: OcrLanguage = OcrLanguage.UNKNOWN): Pair<String, Float> {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        return when (preferLanguage) {
            OcrLanguage.KANNADA -> {
                // ML Kit can't read Kannada script, but CAN extract:
                // - Numbers and prices (50, Rs.100, ₹250)
                // - English text on mixed-language bills
                // This supplements Tesseract which handles the Kannada script.
                val latinResult = runRecognizer(image, latin)
                if (latinResult.first.isNotBlank()) {
                    // Mark confidence lower since this is partial extraction
                    latinResult.first to (latinResult.second * 0.6f)
                } else {
                    "" to 0f
                }
            }
            OcrLanguage.HINDI -> {
                val devText = runRecognizer(image, devanagari)
                val latinText = runRecognizer(image, latin)
                if (devText.first.length > latinText.first.length) devText else latinText
            }
            OcrLanguage.ENGLISH -> {
                runRecognizer(image, latin)
            }
            else -> {
                // Auto-detection: try both and pick best
                val latinText = runRecognizer(image, latin)
                val devText = runRecognizer(image, devanagari)
                if (devText.first.length > latinText.first.length) devText else latinText
            }
        }
    }

    private suspend fun runRecognizer(
        image: InputImage,
        recognizer: com.google.mlkit.vision.text.TextRecognizer
    ): Pair<String, Float> = suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val confidence = if (result.textBlocks.isEmpty()) 0f else 0.85f
                cont.resume(result.text to confidence)
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    fun close() {
        latin.close()
        devanagari.close()
    }
}
