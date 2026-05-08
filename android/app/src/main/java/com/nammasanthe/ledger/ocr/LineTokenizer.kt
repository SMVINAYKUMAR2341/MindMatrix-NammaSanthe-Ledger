package com.nammasanthe.ledger.ocr

/**
 * Breaks a single OCR line into structured tokens for downstream parsing.
 *
 * Token types:
 *  [Token.Price]  – a rupee amount (optionally prefixed by ₹ / Rs / INR)
 *  [Token.Qty]    – a number + unit-of-measure (1kg, 500g, 2L …)
 *  [Token.Word]   – a word that is a candidate item-name fragment
 *
 * Handles OCR glueing ("Onion1kg50" → inserts spaces) and
 * Kannada / Devanagari digit normalisation (done upstream, but guarded here).
 */
object LineTokenizer {

    // ── Patterns ──────────────────────────────────────────────────────────────

    /** Matches a number followed by a unit-of-measure. */
    private val QTY_REGEX = Regex(
        """(\d+(?:\.\d+)?)\s*(kgs?|grams?|gms?|g\b|litres?|liters?|ltrs?|l\b|ml\b|pcs?|pieces?|nos?|dozen|dz|pkt|pkts|pack|packs?|bags?|bunche?s?)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    /** Matches an explicit currency-tagged price (₹ / Rs / INR prefix). */
    private val TAGGED_PRICE_REGEX = Regex(
        """(?:₹|rs\.?|inr)\s*(\d{1,6})(?:[./]\s*-?)?""",
        RegexOption.IGNORE_CASE
    )

    /** Matches a plain integer that could be a price or quantity count. */
    private val BARE_INT_REGEX = Regex("""(?<![.\d])(\d{1,5})(?![.\d%])""")

    private val NOISE_WORDS = setOf(
        "and","the","of","per","each","only","total","bill","receipt","invoice","paid",
        "rs","inr","grand","sub","net","tax","gst","mrp","mop","no","sno","sr","sl"
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Tokenise [line] into an ordered list of [Token]s.
     * Returns an empty list for blank or separator-only lines.
     */
    fun tokenize(line: String): List<Token> {
        if (line.isBlank()) return emptyList()

        // 1 — Pre-process: normalise Devanagari digits, collapse spaces, insert
        //     spaces around glued number-unit or alpha-digit boundaries
        val pre = preprocess(line)

        // 2 — Collect qty spans (contain numbers; must be claimed first)
        data class Span(val range: IntRange, val token: Token)
        val spans = mutableListOf<Span>()

        for (m in QTY_REGEX.findAll(pre)) {
            val num = m.groupValues[1].toDoubleOrNull() ?: continue
            val unit = normaliseUnit(m.groupValues[2])
            spans += Span(m.range, Token.Qty(m.value.trim(), num, unit))
        }

        // 3 — Collect tagged price spans (outside qty spans)
        for (m in TAGGED_PRICE_REGEX.findAll(pre)) {
            if (spans.any { it.range.overlaps(m.range) }) continue
            val v = m.groupValues[1].toIntOrNull() ?: continue
            spans += Span(m.range, Token.Price(v))
        }

        // Sort by position
        spans.sortBy { it.range.first }

        // 4 — Walk the string collecting word tokens between spans
        val tokens = mutableListOf<Token>()
        var cursor = 0
        for ((range, tok) in spans) {
            if (cursor < range.first) {
                tokens += extractWordsAndBareInts(pre.substring(cursor, range.first))
            }
            tokens += tok
            cursor = range.last + 1
        }
        if (cursor < pre.length) {
            tokens += extractWordsAndBareInts(pre.substring(cursor))
        }

        return tokens
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun preprocess(text: String): String = text
        .map { devanagariToAscii(it) }
        .joinToString("")
        // insert space between digit-letter and letter-digit boundaries (OCR glueing)
        .replace(Regex("""(\d+(?:\.\d+)?)(kgs?|gms?|grams?|g|litr?e?s?|ltr?s?|l|ml|pcs?|pieces?|nos?|dz|dozen|pkts?|pack|bags?)"""),
            { mr -> "${mr.groupValues[1]} ${mr.groupValues[2]}" })
        .replace(Regex("""([\p{L}\p{M}])(\d)"""), "$1 $2")
        .replace(Regex("""(\d)([\p{L}\p{M}])""")) { mr ->
            val next = mr.groupValues[2]
            if (next.lowercase() in setOf("k","g","l","m","p")) "${mr.groupValues[1]} $next"
            else "${mr.groupValues[1]} $next"
        }
        .replace(Regex("""\s{2,}"""), " ")
        .trim()

    /** Convert Devanagari / Kannada digits to ASCII. */
    private fun devanagariToAscii(ch: Char): Char {
        val devanagari = ch.code in 0x0966..0x096F
        val kannada    = ch.code in 0x0CE6..0x0CEF
        return when {
            devanagari -> ('0'.code + (ch.code - 0x0966)).toChar()
            kannada    -> ('0'.code + (ch.code - 0x0CE6)).toChar()
            else       -> ch
        }
    }

    private fun extractWordsAndBareInts(segment: String): List<Token> {
        val result = mutableListOf<Token>()
        // First pass: strip ₹-prefixed numbers already caught, grab bare ints
        var remaining = segment
        // tagged prices in this segment
        for (m in TAGGED_PRICE_REGEX.findAll(segment)) {
            m.groupValues[1].toIntOrNull()?.let { result += Token.Price(it) }
            remaining = remaining.replace(m.value, " ")
        }
        // split remaining into space-separated parts
        for (part in remaining.split(Regex("""\s+""")).filter { it.isNotBlank() }) {
            val clean = part.replace(Regex("[₹,।/\\-]"), "").trim()
            val num = clean.toIntOrNull()
                ?: clean.takeIf { Regex("""\d+\.0{1,2}""").matches(it) }
                    ?.substringBefore(".")
                    ?.toIntOrNull()
            if (num != null && num > 0) {
                result += Token.Price(num)
            } else {
                // keep only letter + script characters
                val word = part.replace(Regex("[^a-zA-Z\\u0C00-\\u0CFF\\u0900-\\u097F]"), "").trim()
                if (word.length >= 2 && word.lowercase() !in NOISE_WORDS) {
                    result += Token.Word(word)
                }
            }
        }
        return result
    }

    private fun normaliseUnit(raw: String): String = when (raw.lowercase()) {
        "kgs","kg"               -> "kg"
        "grams","gram","gms","gm","g" -> "g"
        "litres","litre","liters","liter","ltrs","ltr","l" -> "L"
        "ml"                     -> "ml"
        "pcs","pc","pieces","piece" -> "pcs"
        "pkts","pkt","packs","pack" -> "pkt"
        "nos","no"               -> "nos"
        "dozen","dz"             -> "dozen"
        "bags","bag"             -> "bag"
        "bunches","bunche","bunch" -> "bunch"
        else                     -> raw.lowercase()
    }

    private fun IntRange.overlaps(other: IntRange): Boolean =
        first <= other.last && last >= other.first
}

/** Tokens produced by [LineTokenizer]. */
sealed class Token {
    /** A rupee price (plain integer). */
    data class Price(val rupees: Int) : Token()
    /** A measured quantity, e.g. "1kg", "500g". */
    data class Qty(val raw: String, val numeric: Double, val unit: String) : Token()
    /** A candidate item-name word. */
    data class Word(val text: String) : Token()
}
