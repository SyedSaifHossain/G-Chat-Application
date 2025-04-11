package com.syedsaifhossain.g_chatapplication

// Import statements (ensure all needed imports are present)
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge // Keep for now
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.syedsaifhossain.g_chatapplication.databinding.ActivitySettingsBinding
import java.util.Locale

// Ensure it inherits from BaseActivity
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    // onCreate should remain largely the same, but use string resource for title
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Keep for now
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            // Use a string resource for the title for localization
            title = getString(R.string.settings_title) // Make sure R.string.settings_title exists!
            setDisplayHomeAsUpEnabled(true)
        }

        setupSettingsItems() // Call the setup method

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSettingsItems() {
        binding.apply {
            // --- MODIFICATION START ---
            layoutLanguage.setOnClickListener {
                showLanguageDialog() // Call the new dialog method
            }
            // --- MODIFICATION END ---

            layoutNotifications.setOnClickListener {
                openNotificationSettings() // Keep the existing call
            }

            layoutPrivacy.setOnClickListener {
                // TODO: Implement actual privacy policy display
                // Use string resource for Toast message
                Toast.makeText(this@SettingsActivity, R.string.privacy_clicked, Toast.LENGTH_SHORT).show() // Make sure R.string.privacy_clicked exists!
            }

            layoutAbout.setOnClickListener {
                // TODO: Implement actual about screen display
                // Use string resource for Toast message
                Toast.makeText(this@SettingsActivity, R.string.about_clicked, Toast.LENGTH_SHORT).show() // Make sure R.string.about_clicked exists!
            }
        }
    }

    // --- NEW METHOD START ---
    /**
     * Displays a dialog for the user to select the application language.
     * Updates the locale and recreates the activity upon selection of a different language.
     */
    private fun showLanguageDialog() {
        // Define the languages available in your app (based on your res/values-xx folders)
        // These are the names shown to the user in the dialog
        val languages = arrayOf(
            "English", // Default/fallback language display name
            "العربية",   // Arabic display name
            "Ελληνικά", // Greek display name
            "Español", // Spanish display name
            "Italiano",// Italian display name
            "Română",  // Romanian display name
            "Shqip",   // Albanian display name
            "Türkçe"   // Turkish display name
        )
        // Corresponding ISO 639-1 language codes (must match folder suffixes like "values-ar")
        val languageCodes = arrayOf(
            "en", // English code
            "ar", // Arabic code
            "el", // Greek code
            "es", // Spanish code
            "it", // Italian code
            "ro", // Romanian code
            "sq", // Albanian code
            "tr"  // Turkish code
        )

        // Get the currently set language code
        val currentLanguageCode = LocaleHelper.getLanguage(this)
        // Find the index of the current language in the list, default to 0 (English) if not found
        val currentLanguageIndex = languageCodes.indexOf(currentLanguageCode).takeIf { it >= 0 } ?: 0

        // Build the alert dialog
        AlertDialog.Builder(this)
            // Use a string resource for the dialog title
            .setTitle(getString(R.string.select_language_title)) // Make sure R.string.select_language_title exists!
            // Set the list of languages as single-choice items
            .setSingleChoiceItems(languages, currentLanguageIndex) { dialog, which ->
                // 'which' is the index of the selected item
                val selectedLanguageCode = languageCodes[which]

                // Only act if a *different* language is selected
                if (selectedLanguageCode != currentLanguageCode) {
                    // Use LocaleHelper to set and persist the new language
                    LocaleHelper.setLocale(this@SettingsActivity, selectedLanguageCode)
                    // Dismiss the dialog
                    dialog.dismiss()
                    // Recreate the current activity to apply the language change immediately
                    recreate()
                } else {
                    // If the same language is selected, just close the dialog
                    dialog.dismiss()
                }
            }
            // Add a "Cancel" button
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            // Show the dialog
            .show()
    }
    // --- NEW METHOD END ---


    /**
     * Opens the system's notification settings screen for this app.
     */
    private fun openNotificationSettings() {
        val intent = Intent()
        // Intent logic based on Android version (remains the same)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            // ... (rest of the cases remain the same) ...
            else -> {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:$packageName")
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Use string resource for error message
            Toast.makeText(this, R.string.cannot_open_notification_settings, Toast.LENGTH_SHORT).show() // Make sure R.string.cannot_open_notification_settings exists!
        }
    }

    /**
     * Handles the Up button navigation (the back arrow in the ActionBar).
     */
    override fun onSupportNavigateUp(): Boolean {
        // Use the recommended way to handle back press
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}