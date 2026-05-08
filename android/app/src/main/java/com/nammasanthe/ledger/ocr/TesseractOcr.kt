package com.nammasanthe.ledger.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.io.File

/**
 * Simplified OCR for Kannada text with focus on reliability.
 * Uses both ML Kit and Tesseract with proper fallback.
 *
 * Key improvements:
 * - Image scaling to optimal resolution (~1200px width) for Tesseract
 * - Reduced Tesseract runs (2 modes instead of 5) for speed
 * - Post-processing preserves Latin characters (prices, English item names)
 * - Better result scoring using content quality metrics
 */
class SimpleKannadaOcr(private val context: Context) {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val dataDir: File by lazy {
        File(context.filesDir, "tesseract").also { it.mkdirs() }
    }

    /** Optimal width for Tesseract processing (~300 DPI for typical documents) */
    private companion object {
        const val OPTIMAL_WIDTH = 1200
        const val MIN_WIDTH = 600
    }

    fun hasLanguage(lang: String): Boolean {
        val tessFolder = File(dataDir, "tessdata")
        val internal = File(tessFolder, "$lang.traineddata")
        return internal.exists()
    }

    suspend fun recognize(bitmap: Bitmap, languages: String = "kan+eng"): Pair<String, Float> =
        withContext(Dispatchers.Default) {
            val results = mutableListOf<Pair<String, Float>>()

            // Scale image to optimal size for OCR
            val scaled = scaleForOcr(bitmap)

            // Preprocessing 1: Adaptive thresholding (best for handwritten text)
            val adaptiveImg = enhanceImage(scaled)
            if (hasLanguage("kan")) {
                val r1 = tryTesseract(adaptiveImg, "kan")
                if (r1.first.isNotBlank()) results.add(r1)
            }

            // Preprocessing 2: Otsu binarization (best for printed text)
            val binaryImg = preprocessForTesseract(scaled)
            if (hasLanguage("kan")) {
                val r2 = tryTesseract(binaryImg, "kan+eng")
                if (r2.first.isNotBlank()) results.add(r2)
            }

            // Preprocessing 3: ML Kit Latin recognizer for numbers & English text
            val mlkitResult = tryMlKit(bitmap) // Use original (higher res) for ML Kit
            if (mlkitResult.first.isNotBlank() && mlkitResult.first.length > 3) {
                results.add(mlkitResult)
            }

            if (results.isEmpty()) {
                return@withContext "" to 0f
            }

            // Post-process all results and pick the best one
            val processedResults = results.map { (text, conf) ->
                postProcess(text) to conf
            }

            // Select best result based on quality metrics
            processedResults.maxByOrNull { (text, conf) ->
                scoreResult(text, conf)
            } ?: processedResults.first()
        }

    /**
     * Scale bitmap to optimal width for Tesseract processing.
     * Too large = slow & noisy; too small = loss of detail.
     */
    private fun scaleForOcr(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        if (w in MIN_WIDTH..OPTIMAL_WIDTH) return bitmap // Already good size
        if (w < MIN_WIDTH) return bitmap // Too small to downscale

        val scale = OPTIMAL_WIDTH.toFloat() / w
        val newW = OPTIMAL_WIDTH
        val newH = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    /**
     * Score OCR result by quality. Higher = better.
     * Weights: Kannada chars, digits, amount patterns, confidence.
     */
    private fun scoreResult(text: String, conf: Float): Float {
        val kannadaChars = text.count { it.code in 0x0C80..0x0CFF }
        val digits = text.count { it.isDigit() }
        val hasAmounts = Regex("""(?:Rs\.?|₹|INR)?\s*\d+""").findAll(text).count()
        val lines = text.lines().count { it.isNotBlank() }
        // Prefer results with: Kannada text + numbers/amounts + multiple lines
        return (kannadaChars * 3f) + (digits * 2f) + (hasAmounts * 10f) +
               (lines * 5f) + (text.length * 0.1f) + (conf * 8f)
    }

    private suspend fun tryMlKit(bitmap: Bitmap): Pair<String, Float> =
        suspendCancellableCoroutine { cont ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                latinRecognizer.process(image)
                    .addOnSuccessListener { result ->
                        val text = result.text.trim()
                        val confidence = if (result.textBlocks.isEmpty()) 0f else 0.8f
                        cont.resume(text to confidence)
                    }
                    .addOnFailureListener { cont.resume("" to 0f) }
            } catch (e: Exception) {
                cont.resume("" to 0f)
            }
        }

    private fun tryTesseract(bitmap: Bitmap, languages: String): Pair<String, Float> {
        val results = mutableListOf<Pair<String, Float>>()

        // Use only 2 most effective modes (reduced from 5 for speed)
        val modes = listOf(
            TessBaseAPI.PageSegMode.PSM_AUTO,         // Best general mode
            TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK   // Good for bill blocks
        )

        for (mode in modes) {
            val result = runTesseract(bitmap, languages, mode)
            if (result.first.isNotBlank()) {
                results.add(result)
            }
        }

        if (results.isEmpty()) {
            return "" to 0f
        }

        // Pick best result using quality scoring
        return results.maxByOrNull { (text, conf) ->
            scoreResult(text, conf)
        } ?: results.first()
    }

    private fun runTesseract(bitmap: Bitmap, lang: String, mode: Int): Pair<String, Float> {
        val api = TessBaseAPI()
        try {
            val tessDataPath = File(context.filesDir, "tesseract").absolutePath
            if (!api.init(tessDataPath, lang)) {
                return "" to 0f
            }

            // Configure for best accuracy
            api.setPageSegMode(mode)

            api.setImage(bitmap)

            val text = api.utF8Text?.trim() ?: ""
            val confidence = try { api.meanConfidence() / 100f } catch (_: Throwable) { 0.4f }
            return text to confidence
        } catch (e: Exception) {
            return "" to 0f
        } finally {
            api.recycle()
        }
    }

    private fun enhanceImage(bitmap: Bitmap): Bitmap {
        // Advanced preprocessing: grayscale + adaptive thresholding
        val width = bitmap.width
        val height = bitmap.height
        val processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale
        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            gray[i] = (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)).toInt()
        }

        // Adaptive thresholding: larger window + higher constant for photographed bills
        val thresholded = adaptiveThreshold(gray, width, height, 31, 15)

        // Convert back to bitmap (black text on white background)
        for (i in thresholded.indices) {
            val v = thresholded[i]
            pixels[i] = Color.rgb(v, v, v)
        }
        processed.setPixels(pixels, 0, width, 0, 0, width, height)
        return processed
    }

    private fun adaptiveThreshold(gray: IntArray, width: Int, height: Int, windowSize: Int, c: Int): IntArray {
        val result = IntArray(gray.size)
        val halfWindow = windowSize / 2

        // Use integral image for O(1) mean calculation per pixel
        val integral = LongArray((width + 1) * (height + 1))
        for (y in 1..height) {
            var rowSum = 0L
            for (x in 1..width) {
                rowSum += gray[(y - 1) * width + (x - 1)]
                integral[y * (width + 1) + x] = rowSum + integral[(y - 1) * (width + 1) + x]
            }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val y1 = maxOf(0, y - halfWindow)
                val y2 = minOf(height - 1, y + halfWindow)
                val x1 = maxOf(0, x - halfWindow)
                val x2 = minOf(width - 1, x + halfWindow)

                val count = (y2 - y1 + 1) * (x2 - x1 + 1)
                val sum = integral[(y2 + 1) * (width + 1) + (x2 + 1)] -
                          integral[y1 * (width + 1) + (x2 + 1)] -
                          integral[(y2 + 1) * (width + 1) + x1] +
                          integral[y1 * (width + 1) + x1]
                val mean = (sum / count).toInt()
                val idx = y * width + x
                result[idx] = if (gray[idx] < mean - c) 0 else 255
            }
        }
        return result
    }

    private fun preprocessForTesseract(bitmap: Bitmap): Bitmap {
        // Otsu binarization preprocessing
        val width = bitmap.width
        val height = bitmap.height
        val processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            gray[i] = (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p)).toInt()
        }

        val threshold = calculateThreshold(gray)

        for (i in gray.indices) {
            val v = if (gray[i] < threshold) 0 else 255
            pixels[i] = Color.rgb(v, v, v)
        }
        processed.setPixels(pixels, 0, width, 0, 0, width, height)
        return processed
    }

    private fun calculateThreshold(gray: IntArray): Int {
        // Otsu's method
        val histogram = IntArray(256)
        for (v in gray) histogram[v.coerceIn(0, 255)]++

        val total = gray.size
        var sum = 0.0
        for (i in 0..255) sum += i * histogram[i]

        var sumB = 0.0
        var wB = 0
        var maxVar = 0.0
        var threshold = 128

        for (i in 0..255) {
            wB += histogram[i]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += i * histogram[i]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val betweenVar = wB * wF * (mB - mF) * (mB - mF)
            if (betweenVar > maxVar) {
                maxVar = betweenVar
                threshold = i
            }
        }
        return threshold
    }

    /**
     * Post-process OCR text to correct common errors.
     * IMPORTANT: Preserves Latin characters (prices, English item names on bills).
     */
    private fun postProcess(text: String): String {
        var result = text

        // Normalize whitespace (but preserve newlines for line-based parsing)
        result = result.replace(Regex("""[^\S\n]+"""), " ")

        // Remove only actual noise characters (control chars, box-drawing, etc.)
        // Keep: Kannada (0C80-0CFF), Latin letters, digits, common punctuation
        result = result.replace(Regex("""[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]"""), "")

        // Fix common Kannada OCR number confusions
        // Kannada digits ೦-೯ → Arabic digits 0-9
        val kannadaDigitMap = mapOf(
            '೦' to '0', '೧' to '1', '೨' to '2', '೩' to '3', '೪' to '4',
            '೫' to '5', '೬' to '6', '೭' to '7', '೮' to '8', '೯' to '9'
        )
        result = result.map { kannadaDigitMap[it] ?: it }.joinToString("")

        // Normalize common OCR confusions in prices
        result = result.replace(Regex("""(?i)rs\s*\.\s*"""), "Rs.")
        result = result.replace(Regex("""(?i)r5\.?"""), "Rs.")

        // Normalize multiple consecutive blank lines
        result = result.replace(Regex("""\n{3,}"""), "\n\n")

        return result.trim()
    }

    fun close() {
        latinRecognizer.close()
    }
}

class MissingTessdataException(missing: List<String>) : IllegalStateException(
    "Missing Tesseract data: ${missing.joinToString(", ")}. " +
        "Add *.traineddata files to app/src/main/assets/tessdata/."
)
