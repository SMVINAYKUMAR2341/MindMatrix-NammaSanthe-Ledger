package com.nammasanthe.ledger.ocr

import com.nammasanthe.ledger.translation.KannadaOfflineTranslator

/**
 * Multi-line bill parser — the public entry point for OCR post-processing.
 *
 * Typical flow:
 *  1. [OcrPipeline] produces raw text + detected language.
 *  2. [OcrViewModel] translates (Hindi → ML Kit, Kannada → [KannadaOfflineTranslator]).
 *  3. Caller passes translated text to [parseTranslated] (or raw text + language to [parse]).
 *  4. Returns a ranked list of [BillItem]s ready for display / transaction creation.
 *
 * Thread-safe (no mutable state).
 */
class BillParser(private val kannadaTranslator: KannadaOfflineTranslator) {

    companion object {
        /** Items with confidence below this are discarded by default. */
        const val MIN_CONFIDENCE = 0.20f
    }

    // ── Primary entry points ─────────────────────────────────────────────────

    /**
     * Parse [rawText] in any supported language.
     * Applies offline Kannada translation when [language] is KANNADA.
     * For Hindi / English the text is expected to already be translated upstream.
     */
    fun parse(rawText: String, language: OcrLanguage): List<BillItem> {
        if (rawText.isBlank()) return emptyList()
        val working = when (language) {
            OcrLanguage.KANNADA -> kannadaTranslator.translate(rawText)
            else                -> rawText
        }
        return parseTranslated(working)
    }

    /**
     * Parse text that has already been translated to English (or is mixed English).
     * This is the preferred entry point from [OcrViewModel].
     */
    fun parseTranslated(translatedText: String): List<BillItem> {
        if (translatedText.isBlank()) return emptyList()
        
        val items = translatedText
            .lines()
            .asSequence()
            .map    { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { BillLineParser.parse(it) }
            .filter { it.confidence >= MIN_CONFIDENCE || it.amount > 0 }
            .toList()
            
        // If no items were parsed, try more aggressive parsing
        if (items.isEmpty()) {
            return parseAggressively(translatedText)
        }
        
        return items
    }
    
    /**
     * More aggressive parsing for difficult OCR text.
     * Attempts to extract items even with poor OCR quality.
     */
    private fun parseAggressively(text: String): List<BillItem> {
        val items = mutableListOf<BillItem>()
        
        // Split by common separators and try to parse each part
        val parts = text.split(Regex("""[,\n;]"""))
        
        for (part in parts) {
            val cleanPart = part.trim()
            if (cleanPart.isBlank()) continue
            
            // Try to extract numbers and words
            val numbers = Regex("""\d+(?:\.\d+)?""").findAll(cleanPart).map { it.value }.toList()
            val words = cleanPart.split(Regex("""\s+""")).filter { it.isNotBlank() && !it.matches(Regex("""\d+(?:\.\d+)?""")) }
            
            if (numbers.isNotEmpty() && words.isNotEmpty()) {
                // Last number is likely the amount
                val amount = numbers.lastOrNull()?.toDoubleOrNull()?.toInt() ?: 0
                val itemName = words.take(3).joinToString(" ").trim() // Take first 3 words as item name
                
                if (itemName.isNotBlank() && amount > 0) {
                    items.add(BillItem(
                        item = itemName.replaceFirstChar { it.uppercase() },
                        originalLine = cleanPart,
                        quantity = "unknown",
                        amount = amount,
                        confidence = 0.3f // Lower confidence for aggressive parsing
                    ))
                }
            }
        }
        
        return items
    }

    // ── Aggregation helpers ──────────────────────────────────────────────────

    /** Sum of [BillItem.amount] across all items. */
    fun grandTotal(items: List<BillItem>): Int = items.sumOf { it.amount }

    /**
     * One-line summary string suitable for a transaction note.
     *
     * Example: "Onion 1kg ₹50, Banana 1kg ₹40, Potato ₹30 | Total ₹120"
     */
    fun summarize(items: List<BillItem>, maxItems: Int = 5): String {
        if (items.isEmpty()) return ""
        val parts = items.take(maxItems).map { i ->
            buildString {
                append(i.item)
                if (i.quantity != "unknown") append(" ${i.quantity}")
                if (i.amount > 0)            append(" ₹${i.amount}")
            }
        }
        val total = grandTotal(items)
        val suffix = if (items.size > maxItems) " +${items.size - maxItems} more" else ""
        return parts.joinToString(", ") + suffix +
               if (total > 0) " | Total ₹$total" else ""
    }

    /**
     * Produce a JSON-style string (no external dependency) for logging / export.
     *
     * Output matches the spec format:
     * [{"item":"Onion","quantity":"1kg","amount":50}, ...]
     */
    fun toJsonString(items: List<BillItem>): String {
        if (items.isEmpty()) return "[]"
        val rows = items.joinToString(",\n  ") { i ->
            """{"item":"${i.item.replace("\"","'")}","quantity":"${i.quantity}","amount":${i.amount}}"""
        }
        return "[\n  $rows\n]"
    }
}
