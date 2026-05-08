package com.nammasanthe.ledger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val originalText: String,
    val translatedText: String? = null,
    val detectedLanguage: String,
    val extractedAmount: Double? = null,
    val extractedDate: Long? = null,
    val customerId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
