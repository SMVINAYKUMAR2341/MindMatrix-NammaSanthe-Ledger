package com.nammasanthe.ledger.security

/**
 * Pure, stateless QR payload validator.
 *
 * Does NOT touch the database — nonce uniqueness is the caller's
 * responsibility (see [ConfirmationViewModel]).
 */
object QrValidator {

    /** Minimum milliseconds between QR generation and scanning. */
    const val MIN_SCAN_DELAY_MS = 2_000L

    // ── Sealed result ─────────────────────────────────────────────────────────

    sealed class Result {
        data class Accept(val payload: QrPayload, val trustLevel: TrustLevel) : Result()
        data class Reject(val reason: String) : Result()
    }

    // ── Parse ─────────────────────────────────────────────────────────────────

    /** 
     * Deserialises QR data from either user-friendly format or raw JSON.
     * Returns null on any parse failure.
     */
    fun parse(qrData: String): QrPayload? {
        // Try to extract JSON from user-friendly format first
        val jsonPart = QrGenerator.extractJsonFromUserFriendlyQr(qrData)
        return if (jsonPart != null) {
            QrPayload.fromJson(jsonPart)
        } else {
            // Fallback: treat as raw JSON
            QrPayload.fromJson(qrData)
        }
    }

    // ── Structural validation (no DB) ─────────────────────────────────────────

    /**
     * Validates expiry and hash integrity.
     *
     * @return [Result.Reject] with a human-readable reason, or
     *         [Result.Accept] with a preliminary [TrustLevel.UNVERIFIED]
     *         (trust is finalised in [determineTrustLevel]).
     */
    fun validateStructure(payload: QrPayload): Result {
        // 1. Expiry check
        if (System.currentTimeMillis() > payload.expiresAt) {
            return Result.Reject("QR code has expired")
        }

        // 2. Hash integrity — recompute and compare
        val expected = HashUtil.txnHash(
            txnId     = payload.txnId,
            amount    = payload.amount,
            type      = payload.type,
            timestamp = payload.timestamp,   // timestamp baked into hash at generation time
            prevHash  = ""
        )
        if (expected != payload.hash) {
            return Result.Reject("QR data has been tampered with")
        }

        return Result.Accept(payload, TrustLevel.UNVERIFIED)
    }

    // ── Trust level determination ─────────────────────────────────────────────

    /**
     * Determines the final [TrustLevel] after the structural check passes.
     *
     * Rules:
     * - SUSPICIOUS if scanner device == vendor device (self-confirmation)
     * - SUSPICIOUS if scan happened < [MIN_SCAN_DELAY_MS] after QR generation
     * - VERIFIED   otherwise
     *
     * @param scannedAt epoch millis when the QR was decoded by the camera
     */
    fun determineTrustLevel(
        payload         : QrPayload,
        scannerDeviceId : String,
        vendorDeviceId  : String,
        scannedAt       : Long = System.currentTimeMillis()
    ): TrustLevel {
        val isSameDevice = scannerDeviceId == vendorDeviceId
        val isTooFast    = (scannedAt - payload.timestamp) < MIN_SCAN_DELAY_MS
        return if (isSameDevice || isTooFast) TrustLevel.SUSPICIOUS else TrustLevel.VERIFIED
    }
}
