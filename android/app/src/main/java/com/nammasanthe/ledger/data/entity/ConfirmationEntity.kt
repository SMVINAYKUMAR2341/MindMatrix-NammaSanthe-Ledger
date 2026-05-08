package com.nammasanthe.ledger.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nammasanthe.ledger.security.TrustLevel

/**
 * Immutable confirmation record — never hard-deleted (security rule).
 *
 * One confirmation per transaction (1-to-1 with TxnEntity).
 * ON DELETE RESTRICT prevents removing a transaction that has a confirmation.
 */
@Entity(
    tableName = "confirmations",
    foreignKeys = [ForeignKey(
        entity      = TxnEntity::class,
        parentColumns = ["id"],
        childColumns  = ["txnId"],
        onDelete    = ForeignKey.RESTRICT
    )],
    indices = [Index("txnId", unique = true)]
)
data class ConfirmationEntity(
    @PrimaryKey val txnId          : Long,
    val confirmedAt                : Long,
    val scannerDeviceId            : String,
    val vendorDeviceId             : String,
    val trustLevel                 : TrustLevel,
    val nonce                      : String
)
