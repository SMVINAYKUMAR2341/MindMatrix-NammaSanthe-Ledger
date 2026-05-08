package com.nammasanthe.ledger.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxnType { CREDIT, PAYMENT }

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = Customer::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("customerId")]
)
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: TxnType,
    val amount: Double,
    val note: String? = null,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val syncPending: Boolean = true,
    val photoPath: String? = null  // Photo proof for dispute evidence
)
