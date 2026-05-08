package com.nammasanthe.ledger.ocr

enum class OcrLanguage { ENGLISH, HINDI, KANNADA, UNKNOWN }

data class OcrResult(
    val rawText: String,
    val language: OcrLanguage,
    val confidence: Float,
    val source: String,
    val amount: Double? = null,
    val dateMillis: Long? = null,
    val warning: String? = null
)
