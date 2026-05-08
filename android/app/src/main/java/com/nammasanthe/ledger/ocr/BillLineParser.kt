package com.nammasanthe.ledger.ocr

/**
 * Parses a single pre-processed OCR line into a [BillItem].
 *
 * Decision rules (aligned with spec):
 *  • Quantity  = first [Token.Qty] token (if present).
 *  • Amount    = last [Token.Price] value  (per spec: "multiple numbers → last = amount").
 *  • Item name = [Token.Word] tokens → normalised via [GroceryDictionary].
 *  • Confidence computed from token quality and dictionary match.
 *
 * Returns null for blank lines, separator lines, and bill headers/footers.
 */
object BillLineParser {

    // Lines that are bill metadata, not items
    private val SKIP_RE = listOf(
        Regex("""(?i)^\s*(grand\s*total|sub[\s-]?total|total|balance|amount due|net\s*payable|tax|gst|paid|discount)\s*:?\s*\d*${'$'}"""),
        Regex("""^\s*[-=*_#.]{3,}\s*${'$'}"""),
        Regex("""^\s*\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}\s*${'$'}"""),  // date-only
        Regex("""(?i)^\s*[Ss](r|l)?\s*[Nn][Oo]\.?\s*\d*\s*${'$'}"""),  // "SNo."
        Regex("""(?i)^\s*(bill|receipt|invoice|khata|udhaar)\s*${'$'}"""),
    )

    fun parse(line: String): BillItem? {
        val clean = line.trim()
        if (clean.isBlank() || SKIP_RE.any { it.containsMatchIn(clean) }) return null

        val tokens = LineTokenizer.tokenize(clean)
        if (tokens.isEmpty()) return null

        val qtys   = tokens.filterIsInstance<Token.Qty>()
        val prices = tokens.filterIsInstance<Token.Price>()
        val words  = tokens.filterIsInstance<Token.Word>()

        // ── Amount ────────────────────────────────────────────────────────────
        // Spec: multiple numbers → last = amount
        val amount: Int = prices.lastOrNull()?.rupees ?: 0

        // ── Quantity ──────────────────────────────────────────────────────────
        val quantity: String = when {
            qtys.isNotEmpty() -> formatQuantity(qtys.first())
            prices.size >= 2  -> {
                // First price-number might be a loose quantity (e.g. "2 60" → 2 pcs / ₹60)
                val first = prices.first().rupees
                if (first in 1..50 && first < amount) "${first}pcs"
                else "unknown"
            }
            else -> "unknown"
        }

        // ── Item name ─────────────────────────────────────────────────────────
        val itemName = resolveItem(words, clean)

        // Reject lines with nothing useful
        if (itemName.isBlank()) return null

        // ── Confidence ────────────────────────────────────────────────────────
        val inDict   = itemName.isNotBlank() && GroceryDictionary.normalize(itemName) != null
        val confidence = buildConfidence(
            hasAmount    = amount > 0,
            hasQty       = quantity != "unknown",
            hasName      = itemName.isNotBlank(),
            nameInDict   = inDict,
            priceAmbig   = prices.size > 2
        )

        val displayName = (if (inDict) GroceryDictionary.normalize(itemName) ?: itemName else itemName)
            .trim()
            .replaceFirstChar { it.uppercase() }
            .ifBlank { "Unknown Item" }

        return BillItem(
            item         = displayName,
            originalLine = clean,
            quantity     = quantity,
            amount       = amount,
            confidence   = confidence
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun resolveItem(words: List<Token.Word>, originalLine: String): String {
        if (words.isEmpty()) {
            // last resort: substring scan of the original line
            return GroceryDictionary.findInLine(originalLine) ?: ""
        }

        // 1 — Try word-by-word dictionary lookup (keep first hit)
        for (w in words) {
            GroceryDictionary.normalize(w.text)?.let { return it }
        }

        // 2 — Try combined phrase
        val phrase = words.joinToString(" ") { it.text }
        GroceryDictionary.normalize(phrase)?.let { return it }

        // 3 — Full-line substring scan
        GroceryDictionary.findInLine(originalLine)?.let { return it }

        // 4 — Return cleaned raw words as the best guess
        return words
            .map { it.text }
            .filter { it.length >= 2 && it.first().isLetter() }
            .joinToString(" ")
    }

    private fun formatQuantity(qty: Token.Qty): String {
        val number = if (qty.numeric % 1.0 == 0.0) {
            qty.numeric.toInt().toString()
        } else {
            qty.numeric.toString().trimEnd('0').trimEnd('.')
        }
        return "$number${qty.unit}"
    }

    private fun buildConfidence(
        hasAmount  : Boolean,
        hasQty     : Boolean,
        hasName    : Boolean,
        nameInDict : Boolean,
        priceAmbig : Boolean
    ): Float {
        var s = 0f
        if (hasAmount)   s += 0.35f
        if (hasQty)      s += 0.20f
        if (hasName)     s += 0.20f
        if (nameInDict)  s += 0.20f
        if (priceAmbig)  s -= 0.10f
        return s.coerceIn(0.10f, 1.00f)
    }
}
