package com.nammasanthe.ledger.security

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * CameraX [ImageAnalysis.Analyzer] that decodes QR codes from
 * live camera frames using ZXing.
 *
 * Fires [onDecoded] at most once per analyzer instance — re-create or
 * call [reset] to allow further scans.
 *
 * Thread-safety: called on an executor thread by CameraX.
 */
class QrScannerAnalyzer(
    private val onDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)))
    }

    @Volatile private var fired = false

    /** Allow the analyzer to fire again after a decode. */
    fun reset() { fired = false }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        image.use { proxy ->
            if (fired) return

            val media = proxy.image ?: return

            // ── Extract Y-plane luminance ────────────────────────────────────
            val yPlane     = media.planes[0]
            val rowStride  = yPlane.rowStride
            val yBuffer    = yPlane.buffer
            val imgWidth   = media.width
            val imgHeight  = media.height

            val luminance = if (rowStride == imgWidth) {
                // No row padding — use buffer directly
                ByteArray(yBuffer.remaining()).also { yBuffer.get(it) }
            } else {
                // Rows padded — copy only the actual pixel columns
                ByteArray(imgWidth * imgHeight).also { dst ->
                    for (row in 0 until imgHeight) {
                        yBuffer.position(row * rowStride)
                        yBuffer.get(dst, row * imgWidth, imgWidth)
                    }
                }
            }

            val source = PlanarYUVLuminanceSource(
                luminance, imgWidth, imgHeight,
                0, 0, imgWidth, imgHeight, false
            )

            try {
                val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                if (!fired) {
                    fired = true
                    onDecoded(result.text)
                }
            } catch (_: NotFoundException) {
                // No QR found in this frame — normal, keep scanning
            } finally {
                reader.reset()
            }
        }
    }
}
