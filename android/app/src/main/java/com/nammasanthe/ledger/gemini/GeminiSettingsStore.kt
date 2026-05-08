package com.nammasanthe.ledger.gemini

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.geminiDataStore by preferencesDataStore("gemini_settings")

/**
 * Stores Gemini API settings including API key and preferences.
 * API key is stored in plain text - consider using Android Keystore for production.
 */
class GeminiSettingsStore(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val USE_GEMINI = booleanPreferencesKey("use_gemini_for_ocr")
        val GEMINI_ENABLED = booleanPreferencesKey("gemini_enabled")
    }

    /**
     * Flow of current settings.
     */
    val settings: Flow<GeminiSettings> = context.geminiDataStore.data.map { prefs ->
        val savedKey = prefs[Keys.API_KEY]
        GeminiSettings(
            apiKey = if (savedKey.isNullOrEmpty()) "YOUR_GEMINI_API_KEY" else savedKey,
            useGeminiForOcr = prefs[Keys.USE_GEMINI] ?: true,
            geminiEnabled = prefs[Keys.GEMINI_ENABLED] ?: true
        )
    }

    /**
     * Check if Gemini is configured and enabled.
     */
    suspend fun isGeminiAvailable(): Boolean {
        val settings = this.settings.first()
        return settings.geminiEnabled && 
               settings.apiKey.isNotBlank() && 
               settings.useGeminiForOcr
    }

    /**
     * Get API key (synchronous for use in ViewModels).
     */
    fun getApiKey(): String = runBlocking {
        val savedKey = context.geminiDataStore.data.first()[Keys.API_KEY]
        if (savedKey.isNullOrEmpty()) "YOUR_GEMINI_API_KEY" else savedKey
    }

    /**
     * Save API key.
     */
    suspend fun saveApiKey(apiKey: String) {
        context.geminiDataStore.edit { prefs ->
            prefs[Keys.API_KEY] = apiKey.trim()
        }
    }

    /**
     * Clear API key.
     */
    suspend fun clearApiKey() {
        context.geminiDataStore.edit { prefs ->
            prefs.remove(Keys.API_KEY)
        }
    }

    /**
     * Enable/disable using Gemini for OCR.
     */
    suspend fun setUseGeminiForOcr(useGemini: Boolean) {
        context.geminiDataStore.edit { prefs ->
            prefs[Keys.USE_GEMINI] = useGemini
        }
    }

    /**
     * Enable/disable Gemini integration entirely.
     */
    suspend fun setGeminiEnabled(enabled: Boolean) {
        context.geminiDataStore.edit { prefs ->
            prefs[Keys.GEMINI_ENABLED] = enabled
        }
    }

    /**
     * Save all settings at once.
     */
    suspend fun saveSettings(settings: GeminiSettings) {
        context.geminiDataStore.edit { prefs ->
            prefs[Keys.API_KEY] = settings.apiKey
            prefs[Keys.USE_GEMINI] = settings.useGeminiForOcr
            prefs[Keys.GEMINI_ENABLED] = settings.geminiEnabled
        }
    }
}

/**
 * Data class representing Gemini settings.
 */
data class GeminiSettings(
    val apiKey: String = "YOUR_GEMINI_API_KEY",
    val useGeminiForOcr: Boolean = true,
    val geminiEnabled: Boolean = true
)
