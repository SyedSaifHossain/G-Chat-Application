package com.syedsaifhossain.g_chatapplication

import android.app.Application
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EmojiManager.install(GoogleEmojiProvider())
    }
} 