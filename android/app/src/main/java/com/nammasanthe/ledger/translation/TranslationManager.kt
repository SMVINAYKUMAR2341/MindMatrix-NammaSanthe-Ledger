package com.nammasanthe.ledger.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device ML Kit translation. Models download once on first use,
 * then run fully offline. Language codes follow ISO 639-1 (e.g. "hi", "en").
 */
class TranslationManager {

    private val translators = HashMap<String, Translator>()

    private fun key(src: String, dst: String) = "$src->$dst"

    private fun getTranslator(src: String, dst: String): Translator {
        return translators.getOrPut(key(src, dst)) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(src) ?: TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(dst) ?: TranslateLanguage.ENGLISH)
                .build()
            Translation.getClient(options)
        }
    }

    fun isLanguageSupported(tag: String): Boolean =
        TranslateLanguage.fromLanguageTag(tag) != null

    suspend fun isModelDownloaded(tag: String): Boolean {
        val lang = TranslateLanguage.fromLanguageTag(tag) ?: return false
        val model = TranslateRemoteModel.Builder(lang).build()
        val manager = RemoteModelManager.getInstance()
        return suspendCancellableCoroutine { cont ->
            manager.isModelDownloaded(model)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(false) }
        }
    }

    suspend fun ensureModel(src: String, dst: String, requireWifi: Boolean = false) {
        val translator = getTranslator(src, dst)
        val conditions = DownloadConditions.Builder().apply {
            if (requireWifi) requireWifi()
        }.build()
        suspendCancellableCoroutine<Unit> { cont ->
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    suspend fun translate(text: String, src: String, dst: String): String {
        if (text.isBlank()) return ""
        ensureModel(src, dst)
        val translator = getTranslator(src, dst)
        return suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    suspend fun translateIfDownloaded(text: String, src: String, dst: String): String? {
        if (text.isBlank()) return ""
        if (!isLanguageSupported(src) || !isLanguageSupported(dst)) return null
        val srcReady = isModelDownloaded(src)
        val dstReady = isModelDownloaded(dst)
        if (!srcReady || !dstReady) return null
        val translator = getTranslator(src, dst)
        return suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
