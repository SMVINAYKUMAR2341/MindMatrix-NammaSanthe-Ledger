package com.nammasanthe.ledger.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import com.nammasanthe.ledger.data.entity.TxnType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class VoiceCommand(
    val customerName: String?,
    val amount: Double?,
    val type: TxnType?,
    val rawText: String
)

sealed class VoiceEvent {
    data class Partial(val text: String) : VoiceEvent()
    data class Final(val command: VoiceCommand) : VoiceEvent()
    data class Error(val code: Int) : VoiceEvent()
}

class VoiceInput(private val context: Context) {

    fun listen(language: String = "en-US"): Flow<VoiceEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(VoiceEvent.Error(-1))
            close()
            return@callbackFlow
        }
        
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    trySend(VoiceEvent.Error(error))
                }
                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = list?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) {
                        trySend(VoiceEvent.Final(parseCommand(text)))
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = list?.firstOrNull().orEmpty()
                    if (partialText.isNotBlank()) {
                        trySend(VoiceEvent.Partial(partialText))
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            recognizer.setRecognitionListener(listener)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            recognizer.startListening(intent)
            awaitClose {
                try {
                    recognizer.stopListening()
                    recognizer.destroy()
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        } catch (e: Exception) {
            trySend(VoiceEvent.Error(-2))
            close()
        }
    }

    companion object {
        private val NUMBER_WORDS = mapOf(
            "ten" to 10, "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90, "hundred" to 100,
            "thousand" to 1000,
            "ಹತ್ತು" to 10, "ಇಪ್ಪತ್ತು" to 20, "ನೂರು" to 100, "ಸಾವಿರ" to 1000,
            "दस" to 10, "बीस" to 20, "सौ" to 100, "हज़ार" to 1000
        )

        fun parseCommand(text: String): VoiceCommand {
            val lower = text.lowercase()
            val type = when {
                listOf("paid", "payment", "received", "ಪಾವತಿ", "भुगतान").any { lower.contains(it) } ->
                    TxnType.PAYMENT
                listOf("credit", "udari", "udhar", "ಸಾಲ", "उधार", "क्रेडिट").any { lower.contains(it) } ->
                    TxnType.CREDIT
                else -> null
            }
            val numberMatch = Regex("\\b(\\d{1,7}(?:\\.\\d{1,2})?)\\b").find(text)
            val numericAmount = numberMatch?.value?.toDoubleOrNull()
            val wordAmount = if (numericAmount == null) {
                text.split(" ").mapNotNull { NUMBER_WORDS[it.lowercase()] }
                    .fold(0) { acc, v -> if (v >= 100) acc * v else acc + v }
                    .takeIf { it > 0 }?.toDouble()
            } else null
            val amount = numericAmount ?: wordAmount

            val nameTokens = text.split(" ").filter {
                it.isNotBlank() && !it.matches(Regex("\\d+(\\.\\d+)?")) &&
                    it.lowercase() !in listOf(
                        "paid", "payment", "received", "credit", "udari", "udhar",
                        "rupees", "rupee", "rs"
                    )
            }
            val name = nameTokens.firstOrNull()?.replaceFirstChar { it.uppercase() }
            return VoiceCommand(name, amount, type, text)
        }
    }
}
