package com.nammasanthe.ledger.security

import android.content.Context
import android.provider.Settings

/**
 * Provides a stable, hardware-backed device identifier for
 * self-confirmation fraud detection.
 *
 * Uses ANDROID_ID which is unique per app/user since Android 8.
 * Falls back to "unknown" if unavailable.
 */
object DeviceIdHelper {
    fun getDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: "unknown_device"
}
