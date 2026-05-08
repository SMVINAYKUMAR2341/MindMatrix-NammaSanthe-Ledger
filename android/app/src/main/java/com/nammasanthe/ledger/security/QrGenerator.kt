package com.nammasanthe.ledger.security

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.nammasanthe.ledger.data.entity.TxnEntity
import java.net.URLEncoder
import java.util.UUID

/**
 * Generates tamper-evident QR payloads and renders them as Bitmaps.
 *
 * QR now encodes a Firebase Hosting URL so that any phone's camera
 * can open the confirmation page — no Namma-Santhe app needed on
 * the customer's device.
 *
 * Error correction: H (30 %) — tolerates partial obscuring.
 */
object QrGenerator {

    const val QR_TTL_MS = 60_000L

    /** Base URL for the Firebase-hosted confirmation page. */
    private const val CONFIRM_BASE_URL = "https://namma-santhe-ledger.web.app/confirm"

    /**
     * Build a signed [QrPayload] for [txn].
     *
     * @param prevHash Pass the previous transaction's hash to chain records;
     *                 leave empty for standalone confirmation.
     */
    fun buildPayload(txn: TxnEntity, prevHash: String = ""): QrPayload {
        val now   = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString()
        val hash  = HashUtil.txnHash(
            txnId     = txn.id.toString(),
            amount    = txn.amount,
            type      = txn.type.name,
            timestamp = now,
            prevHash  = prevHash
        )
        return QrPayload(
            txnId     = txn.id.toString(),
            amount    = txn.amount,
            type      = txn.type.name,
            timestamp = now,
            nonce     = nonce,
            expiresAt = now + QR_TTL_MS,
            hash      = hash
        )
    }

    /**
     * Build a Firebase Hosting URL for the confirmation web page.
     *
     * @param payload The QR payload with transaction details
     * @param userId  The Firebase Auth UID of the vendor — used by the web page
     *                to write the confirmation into the correct Firestore path.
     * @param vendorName Optional vendor business name shown to customer.
     */
    fun buildConfirmUrl(
        payload    : QrPayload,
        userId     : String,
        vendorName : String = "Namma Santhe Vendor"
    ): String {
        val encodedVendor = URLEncoder.encode(vendorName, "UTF-8")
        return "$CONFIRM_BASE_URL" +
                "?txnId=${payload.txnId}" +
                "&amt=${payload.amount}" +
                "&type=${payload.type}" +
                "&hash=${payload.hash}" +
                "&ts=${payload.timestamp}" +
                "&uid=$userId" +
                "&vendor=$encodedVendor"
    }

    /**
     * Render a URL as a QR code Bitmap of [sizePx] × [sizePx].
     * Must be called on a background thread.
     */
    fun generateBitmapFromUrl(url: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN           to 1,
            EncodeHintType.CHARACTER_SET    to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp    = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    // ── Legacy helpers (kept for backward compat with existing scan screen) ──

    /**
     * Generate a user-friendly QR code text that contains both readable info and JSON data.
     * Format: "Namma Santhe Payment | Amount: ₹XX.XX | Type: CREDIT/PAYMENT | [JSON]"
     */
    fun generateUserFriendlyQrText(payload: QrPayload): String {
        val amountStr = "₹${"%.2f".format(payload.amount)}"
        val typeStr = if (payload.type == "CREDIT") "Credit" else "Payment"
        val jsonPart = payload.toJson()
        return "Namma Santhe $typeStr | Amount: $amountStr | ID: ${payload.txnId} | [$jsonPart]"
    }

    /**
     * Render [payload] as a square Bitmap of [sizePx] × [sizePx].
     * Uses user-friendly format instead of raw JSON.
     * Must be called on a background thread.
     */
    fun generateBitmap(payload: QrPayload, sizePx: Int = 512): Bitmap {
        val qrText = generateUserFriendlyQrText(payload)
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN           to 1
        )
        val matrix = QRCodeWriter().encode(qrText, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp    = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    /**
     * Extract JSON data from user-friendly QR text.
     */
    fun extractJsonFromUserFriendlyQr(qrText: String): String? {
        val startIndex = qrText.indexOf('[')
        val endIndex = qrText.lastIndexOf(']')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            qrText.substring(startIndex + 1, endIndex)
        } else {
            if (qrText.trim().startsWith("{") && qrText.trim().endsWith("}")) {
                qrText
            } else {
                null
            }
        }
    }
}
