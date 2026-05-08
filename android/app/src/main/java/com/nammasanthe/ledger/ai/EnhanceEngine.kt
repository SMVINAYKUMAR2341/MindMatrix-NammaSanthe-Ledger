package com.nammasanthe.ledger.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local "Enhance" engine. By default this runs a deterministic cleanup
 * (whitespace collapse, line dedupe, summary). If you ship a GGUF 1B-3B
 * quantized model with a JNI runtime (e.g. llama.cpp), wire it inside
 * [runLlm] and gate by [supportsLlm].
 *
 * Triggered ONLY by an explicit user "Enhance" button on a background thread.
 */
class EnhanceEngine {

    var supportsLlm: Boolean = false
        private set

    suspend fun cleanup(rawText: String): String = withContext(Dispatchers.Default) {
        if (rawText.isBlank()) return@withContext rawText
        rawText
            .replace(Regex("[ \\t]+"), " ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")
    }

    suspend fun summarize(rawText: String, maxChars: Int = 240): String =
        withContext(Dispatchers.Default) {
            val cleaned = cleanup(rawText)
            if (supportsLlm) runLlm(cleaned, maxChars) else heuristicSummary(cleaned, maxChars)
        }

    private fun heuristicSummary(text: String, maxChars: Int): String {
        val sentences = text.split(Regex("[.!?\\n]")).map { it.trim() }.filter { it.isNotBlank() }
        val out = StringBuilder()
        for (s in sentences) {
            if (out.length + s.length + 1 > maxChars) break
            if (out.isNotEmpty()) out.append(" ")
            out.append(s).append('.')
        }
        return if (out.isEmpty()) text.take(maxChars) else out.toString()
    }

    /** Replace this with a real GGUF inference call if/when bundled. */
    @Suppress("UNUSED_PARAMETER")
    private fun runLlm(text: String, maxChars: Int): String = text.take(maxChars)
}
