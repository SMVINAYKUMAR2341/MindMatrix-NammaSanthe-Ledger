package com.nammasanthe.ledger.ocr

/**
 * A single parsed line from a handwritten / scanned market bill.
 *
 * @param item         Normalized English item name (e.g. "Onion", "Banana").
 * @param originalLine Raw OCR line before any processing.
 * @param quantity     Quantity string e.g. "1kg", "500g", "2pcs" —
 *                     "unknown" when not determinable.
 * @param amount       Price in whole rupees (0 when not parseable).
 * @param confidence   Parser confidence in 0.0–1.0.
 */
data class BillItem(
    val item        : String,
    val originalLine: String,
    val quantity    : String,
    val amount      : Int,
    val confidence  : Float
)
