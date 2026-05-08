package com.nammasanthe.ledger.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {
    fun wrap(base: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
