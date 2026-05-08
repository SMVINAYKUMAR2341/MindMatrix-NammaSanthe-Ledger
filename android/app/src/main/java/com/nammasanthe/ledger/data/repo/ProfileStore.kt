package com.nammasanthe.ledger.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("profile")

data class AppProfile(
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val gstNumber: String = "",
    val language: String = "en",
    val pinHash: String? = null,
    val syncEnabled: Boolean = false
)

class ProfileStore(private val context: Context) {

    private object Keys {
        val business = stringPreferencesKey("business")
        val owner = stringPreferencesKey("owner")
        val phone = stringPreferencesKey("phone")
        val address = stringPreferencesKey("address")
        val gst = stringPreferencesKey("gst")
        val lang = stringPreferencesKey("lang")
        val pin = stringPreferencesKey("pin")
        val sync = booleanPreferencesKey("sync")
    }

    val profile: Flow<AppProfile> = context.dataStore.data.map { prefs ->
        AppProfile(
            businessName = prefs[Keys.business].orEmpty(),
            ownerName = prefs[Keys.owner].orEmpty(),
            phone = prefs[Keys.phone].orEmpty(),
            address = prefs[Keys.address].orEmpty(),
            gstNumber = prefs[Keys.gst].orEmpty(),
            language = prefs[Keys.lang] ?: "en",
            pinHash = prefs[Keys.pin],
            syncEnabled = prefs[Keys.sync] ?: false
        )
    }

    suspend fun update(profile: AppProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.business] = profile.businessName
            prefs[Keys.owner] = profile.ownerName
            prefs[Keys.phone] = profile.phone
            prefs[Keys.address] = profile.address
            prefs[Keys.gst] = profile.gstNumber
            prefs[Keys.lang] = profile.language
            profile.pinHash?.let { prefs[Keys.pin] = it }
            prefs[Keys.sync] = profile.syncEnabled
        }
    }

    suspend fun setPin(hash: String?) {
        context.dataStore.edit { prefs ->
            if (hash == null) prefs.remove(Keys.pin) else prefs[Keys.pin] = hash
        }
    }
}
