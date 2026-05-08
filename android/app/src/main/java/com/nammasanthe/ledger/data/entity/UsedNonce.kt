package com.nammasanthe.ledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-use nonce registry.
 *
 * Once a nonce is inserted here, any subsequent QR bearing the same
 * nonce is rejected — preventing replay attacks.
 *
 * Never deleted (security rule).
 */
@Entity(tableName = "used_nonces")
data class UsedNonce(
    @PrimaryKey val nonce : String,
    val txnId             : Long,
    val usedAt            : Long = System.currentTimeMillis()
)
