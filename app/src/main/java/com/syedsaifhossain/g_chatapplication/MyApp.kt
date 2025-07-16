package com.syedsaifhossain.g_chatapplication

import android.app.Application
import android.content.Context
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import com.yariksoffice.lingver.Lingver

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize emojis
        EmojiManager.install(GoogleEmojiProvider())

        // Get saved language or default to "en"
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("lang", "en") ?: "en"

        // Initialize Lingver with saved language
        Lingver.init(this, lang)
    }
}