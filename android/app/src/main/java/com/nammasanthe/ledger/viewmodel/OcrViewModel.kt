package com.nammasanthe.ledger.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammasanthe.ledger.NammaSantheApp
import com.nammasanthe.ledger.ai.EnhanceEngine
import com.nammasanthe.ledger.camera.decodeAndOrient
import com.nammasanthe.ledger.data.entity.ScanData
import com.nammasanthe.ledger.ocr.OcrLanguage
import com.nammasanthe.ledger.ocr.OcrPipeline
import com.nammasanthe.ledger.ocr.OcrResult
import com.nammasanthe.ledger.ocr.BillItem
import com.nammasanthe.ledger.ocr.BillParser
import com.nammasanthe.ledger.translation.KannadaOfflineTranslator
import com.nammasanthe.ledger.translation.TranslationManager
import com.nammasanthe.ledger.util.ImageStore
import com.nammasanthe.ledger.gemini.GeminiService
import com.nammasanthe.ledger.gemini.GeminiSettingsStore
import com.nammasanthe.ledger.gemini.GeminiOcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class OcrUiState(
    val processing: Boolean = false,
    val original: String = "",
    val translated: String = "",
    val language: OcrLanguage = OcrLanguage.UNKNOWN,
    val confidence: Float = 0f,
    val source: String = "",
    val amount: Double? = null,
    val dateMillis: Long? = null,
    val warning: String? = null,
    val error: String? = null,
    val imagePath: String? = null,
    val parsedItems: List<com.nammasanthe.ledger.ocr.BillItem> = emptyList(),
    val usingGemini: Boolean = false
)

class OcrViewModel(private val app: NammaSantheApp) : ViewModel() {

    private val pipeline = OcrPipeline(app)
    private val translator = TranslationManager()
    private val knTranslator = KannadaOfflineTranslator()
    private val enhancer = EnhanceEngine()
    private val billParser = BillParser(knTranslator)
    private val geminiService = GeminiService()
    private val geminiSettings = GeminiSettingsStore(app)

    private val _state = MutableStateFlow(OcrUiState())
    val state = _state.asStateFlow()

    fun setPreferredLanguage(language: OcrLanguage) {
        pipeline.setPreferredLanguage(language)
    }

    fun processCapturedFile(file: File) {
        viewModelScope.launch {
            _state.value = _state.value.copy(processing = true, error = null, warning = null, usingGemini = false)
            try {
                val savedPath = withContext(Dispatchers.IO) {
                    ImageStore.saveCompressed(app, file)
                }
                val bitmap = withContext(Dispatchers.IO) { decodeAndOrient(file) }
                
                // Check if Gemini is enabled and available
                val useGemini = geminiSettings.isGeminiAvailable()
                
                val ocr: OcrResult
                val geminiResult: GeminiOcrResult?
                
                if (useGemini) {
                    _state.value = _state.value.copy(usingGemini = true)
                    val apiKey = geminiSettings.getApiKey()
                    geminiResult = geminiService.performOcr(bitmap, apiKey)
                    
                    if (geminiResult.success) {
                        // Convert Gemini result to OcrResult format
                        ocr = OcrResult(
                            rawText = geminiResult.rawText,
                            language = OcrLanguage.KANNADA,
                            confidence = 0.95f,
                            source = "gemini-api",
                            amount = extractAmountFromItems(geminiResult.items),
                            dateMillis = null,
                            warning = null
                        )
                        
                        // Parse Gemini items directly
                        val geminiItems = geminiResult.items.map { item ->
                            com.nammasanthe.ledger.ocr.BillItem(
                                item = item.name,
                                originalLine = "${item.name} ${item.quantity} ${item.amount}",
                                quantity = item.quantity,
                                amount = item.amount,
                                confidence = 0.9f
                            )
                        }
                        
                        _state.value = OcrUiState(
                            processing = false,
                            original = geminiResult.rawText,
                            translated = geminiResult.translatedText,
                            language = OcrLanguage.KANNADA,
                            confidence = 0.95f,
                            source = "gemini-api",
                            amount = extractAmountFromItems(geminiResult.items),
                            warning = null,
                            imagePath = savedPath,
                            parsedItems = geminiItems,
                            usingGemini = true
                        )
                        return@launch
                    } else {
                        // Fall back to on-device OCR if Gemini fails
                        geminiResult.error?.let {
                            _state.value = _state.value.copy(
                                warning = "Gemini API failed: $it. Falling back to on-device OCR."
                            )
                        }
                        ocr = pipeline.run(bitmap)
                    }
                } else {
                    geminiResult = null
                    ocr = pipeline.run(bitmap)
                }
                val translation = when (ocr.language) {
                    OcrLanguage.ENGLISH -> ocr.rawText
                    OcrLanguage.HINDI -> pipeline.translateIfNeeded(ocr, translator)
                    OcrLanguage.KANNADA -> knTranslator.translate(ocr.rawText)
                    else -> ""
                }
                val warnings = mutableListOf<String>()
                ocr.warning?.let { warnings.add(it) }
                when (ocr.language) {
                    OcrLanguage.HINDI -> {
                        if (ocr.rawText.isNotBlank() && translation.isNullOrBlank()) {
                            val modelReady = translator.isModelDownloaded("hi") && translator.isModelDownloaded("en")
                            val msg = if (modelReady) {
                                "Translation failed. Tap Translate Again."
                            } else {
                                "Hindi translation model not downloaded. Connect once to download for offline use."
                            }
                            warnings.add(msg)
                        }
                    }
                    OcrLanguage.KANNADA -> {
                        if (!knTranslator.hasChanges(ocr.rawText, translation ?: "")) {
                            warnings.add("Kannada translation is limited. Edit manually if needed.")
                        }
                    }
                    else -> Unit
                }
                val warningText = warnings.joinToString("\n").ifBlank { null }
                val translatedForParsing = translation ?: ocr.rawText
                val parsedItems = withContext(Dispatchers.Default) {
                    billParser.parseTranslated(translatedForParsing)
                }
                _state.value = OcrUiState(
                    processing   = false,
                    original     = ocr.rawText,
                    translated   = translation ?: "",
                    language     = ocr.language,
                    confidence   = ocr.confidence,
                    source       = ocr.source,
                    amount       = ocr.amount,
                    dateMillis   = ocr.dateMillis,
                    warning      = warningText,
                    imagePath    = savedPath,
                    parsedItems  = parsedItems
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(processing = false, error = t.message, warning = null)
            }
        }
    }

    fun retranslate() {
        viewModelScope.launch {
            val s = _state.value
            if (s.original.isBlank()) return@launch
            _state.value = s.copy(processing = true, error = null)
            try {
                when (s.language) {
                    OcrLanguage.ENGLISH -> {
                        _state.value = s.copy(processing = false, translated = s.original, warning = null)
                    }
                    OcrLanguage.HINDI -> {
                        val translated = translator.translate(s.original, "hi", "en")
                        _state.value = s.copy(processing = false, translated = translated, warning = null)
                    }
                    OcrLanguage.KANNADA -> {
                        val translated = knTranslator.translate(s.original)
                        val warn = if (knTranslator.hasChanges(s.original, translated)) null
                        else "Kannada translation is limited. Edit manually if needed."
                        _state.value = s.copy(processing = false, translated = translated, warning = warn)
                    }
                    else -> {
                        _state.value = s.copy(
                            processing = false,
                            warning = "Translation is not available for this language."
                        )
                    }
                }
            } catch (t: Throwable) {
                val msg = t.message?.takeIf { it.isNotBlank() }
                    ?: "Translation failed. Connect once to download the model."
                _state.value = _state.value.copy(processing = false, warning = msg)
            }
        }
    }

    /**
     * Re-parse the current translated (or original) text into structured [BillItem]s.
     * Called automatically after [processCapturedFile] and also available as a manual
     * "Re-Parse" action in the UI.
     */
    fun parseBill() {
        viewModelScope.launch {
            val s = _state.value
            val text = s.translated.ifBlank { s.original }
            if (text.isBlank()) return@launch
            _state.value = s.copy(processing = true)
            val items = withContext(Dispatchers.Default) {
                billParser.parseTranslated(text)
            }
            _state.value = _state.value.copy(processing = false, parsedItems = items)
        }
    }

    fun enhance() {
        viewModelScope.launch {
            val s = _state.value
            if (s.original.isBlank()) return@launch
            _state.value = s.copy(processing = true)
            val cleaned = enhancer.cleanup(s.original)
            val summary = enhancer.summarize(cleaned)
            _state.value = _state.value.copy(processing = false, original = cleaned, translated = summary)
        }
    }

    fun updateOriginal(text: String) {
        _state.value = _state.value.copy(original = text)
    }

    fun updateTranslated(text: String) {
        _state.value = _state.value.copy(translated = text)
    }

    fun saveScan(customerId: Long? = null) {
        val s = _state.value
        if (s.imagePath == null || s.original.isBlank()) return
        viewModelScope.launch {
            app.repository.saveScan(
                ScanData(
                    imagePath = s.imagePath,
                    originalText = s.original,
                    translatedText = s.translated.ifBlank { null },
                    detectedLanguage = s.language.name,
                    extractedAmount = s.amount,
                    extractedDate = s.dateMillis,
                    customerId = customerId
                )
            )
        }
    }

    override fun onCleared() {
        pipeline.close()
        translator.close()
        // GeminiService doesn't require explicit cleanup
    }

    @Suppress("unused")
    fun context(): Context = app

    /**
     * Extract total amount from Gemini bill items.
     */
    private fun extractAmountFromItems(items: List<com.nammasanthe.ledger.gemini.BillItem>): Double? {
        if (items.isEmpty()) return null
        return items.sumOf { it.amount.toDouble() }
    }
}
