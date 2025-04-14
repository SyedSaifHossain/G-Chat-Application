package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

/**
 * Utility object for managing application locale and persisting the selected language.
 */
object LocaleHelper {

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    private const val PREFERENCES_NAME = "Locale.Helper.Preferences"

    /**
     * Attaches the saved locale to the context. Called from BaseActivity.
     * @param context The base context.
     * @return Context with the applied locale.
     */
    fun onAttach(context: Context): Context {
        // Get the persisted language, defaulting to English ("en") if none is found
        val lang = getPersistedLocale(context)
        return setLocale(context, lang)
    }

    /**
     * Gets the currently persisted language code.
     * @param context The context.
     * @return The persisted language code (e.g., "en", "ar"). Defaults to "en".
     */
    fun getLanguage(context: Context): String {
        return getPersistedLocale(context)
    }

    /**
     * Sets the application's locale and persists the choice.
     * @param context The context.
     * @param languageCode The language code to set (e.g., "en", "ar").
     * @return Context with the applied locale.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        persistLocale(context, languageCode)

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        // Set layout direction based on locale (important for RTL languages like Arabic)
        configuration.setLayoutDirection(locale)

        // Create and return a new context with the updated configuration
        return context.createConfigurationContext(configuration)
    }

    /**
     * Saves the selected language code to SharedPreferences.
     * @param context The context.
     * @param languageCode The language code to save (e.g., "en", "ar").
     */
    private fun persistLocale(context: Context, languageCode: String) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(SELECTED_LANGUAGE, languageCode)
        editor.apply() // Use apply() for asynchronous saving
    }

    /**
     * Retrieves the saved language code from SharedPreferences.
     * @param context The context.
     * @return The saved language code, defaulting to "en" if none is found.
     */
    private fun getPersistedLocale(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        // Default to English ("en") if no language is saved
        return preferences.getString(SELECTED_LANGUAGE, "en") ?: "en"
    }
}