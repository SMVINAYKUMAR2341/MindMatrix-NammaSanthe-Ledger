package com.nammasanthe.ledger.security

import java.security.MessageDigest

/**
 * SHA-256 utilities for tamper-evident QR payload hashing.
 *
 * Hash input = txnId | amount | type | timestamp | prevHash
 * Fields are joined with '|' and encoded as UTF-8.
 */
object HashUtil {

    fun sha256(vararg parts: String): String {
        val input  = parts.joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Canonical hash for a transaction payload.
     * prevHash can be empty string for the first transaction in a chain.
     */
    fun txnHash(
        txnId    : String,
        amount   : Double,
        type     : String,
        timestamp: Long,
        prevHash : String = ""
    ): String = sha256(txnId, amount.toString(), type, timestamp.toString(), prevHash)
}
