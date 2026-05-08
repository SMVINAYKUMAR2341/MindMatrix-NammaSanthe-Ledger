package com.nammasanthe.ledger.security

import org.json.JSONObject

/**
 * JSON payload embedded in the QR code.
 *
 * JSON shape:
 * {
 *   "txnId"     : String,
 *   "amount"    : Double,
 *   "type"      : "CREDIT" | "PAYMENT",
 *   "timestamp" : Long,
 *   "nonce"     : String  (UUID, single-use),
 *   "expiresAt" : Long    (timestamp + 60 s),
 *   "hash"      : String  (SHA-256 of core fields)
 * }
 */
data class QrPayload(
    val txnId     : String,
    val amount    : Double,
    val type      : String,
    val timestamp : Long,
    val nonce     : String,
    val expiresAt : Long,
    val hash      : String
) {
    fun toJson(): String = JSONObject()
        .put("txnId",     txnId)
        .put("amount",    amount)
        .put("type",      type)
        .put("timestamp", timestamp)
        .put("nonce",     nonce)
        .put("expiresAt", expiresAt)
        .put("hash",      hash)
        .toString()

    companion object {
        /** Returns null if json is malformed / missing fields. */
        fun fromJson(json: String): QrPayload? = runCatching {
            val o = JSONObject(json)
            QrPayload(
                txnId     = o.getString("txnId"),
                amount    = o.getDouble("amount"),
                type      = o.getString("type"),
                timestamp = o.getLong("timestamp"),
                nonce     = o.getString("nonce"),
                expiresAt = o.getLong("expiresAt"),
                hash      = o.getString("hash")
            )
        }.getOrNull()
    }
}
