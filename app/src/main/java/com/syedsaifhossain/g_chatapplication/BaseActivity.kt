package com.syedsaifhossain.g_chatapplication

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

/**
 * Base Activity class that applies the saved locale configuration from LocaleHelper
 * to its context. All other Activities in the app should inherit from this class
 * to ensure consistent language settings.
 */
// 'open' keyword allows this class to be inherited by other classes
open class BaseActivity : AppCompatActivity() {

    /**
     * Overrides attachBaseContext to wrap the base context with the
     * locale configuration provided by LocaleHelper.
     */
    override fun attachBaseContext(newBase: Context) {
        // 恢复为原始实现
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}