package com.syedsaifhossain.g_chatapplication.utils

import android.content.Context
import android.content.res.Configuration
import java.util.*

object LanguageUtil {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}