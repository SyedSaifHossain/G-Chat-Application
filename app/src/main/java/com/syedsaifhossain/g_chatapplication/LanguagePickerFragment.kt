package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.LanguageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLanguagePickerBinding
import com.syedsaifhossain.g_chatapplication.models.LanguageItem
import com.yariksoffice.lingver.Lingver


class LanguagePickerFragment : Fragment() {
    private var _binding: FragmentLanguagePickerBinding? = null
    private val binding get() = _binding!!
    private val languageList = listOf(
        LanguageItem("en", "English (US)"),
        LanguageItem("es", "Spanish"),
        LanguageItem("pt", "Portuguese (Brazil)"),
        LanguageItem("ru", "Russian"),
        LanguageItem("id", "Indonesian"),
        LanguageItem("ar", "Arabic"),
        LanguageItem("fr", "French"),
        LanguageItem("de", "German"),
        LanguageItem("tr", "Turkish"),
        LanguageItem("it", "Italian"),
        LanguageItem("hi", "Hindi"),
        LanguageItem("bn", "Bengali"),
        LanguageItem("mr", "Marathi"),
        LanguageItem("ur", "Urdu (Pakistan)"),
        LanguageItem("gu", "Gujarati"),
        LanguageItem("fa", "Persian"),
        LanguageItem("nl", "Dutch"),
        LanguageItem("pl", "Polish"),
        LanguageItem("ro", "Romanian"),
        LanguageItem("zh-Hant-TW", "Chinese (Traditional, Taiwan)"),
        LanguageItem("zh-Hant-HK", "Chinese (Traditional, Hong Kong)"),
        LanguageItem("ms", "Malay"),
        LanguageItem("he", "Hebrew"),
        LanguageItem("cs", "Czech"),
        LanguageItem("sw", "Swahili"),
        LanguageItem("uk", "Ukrainian"),
        LanguageItem("th", "Thai"),
        LanguageItem("zh", "Chinese (Simplified, China)"),
        LanguageItem("hu", "Hungarian"),
        LanguageItem("sk", "Slovak"),
        LanguageItem("pt-PT", "Portuguese (Portugal)"),
        LanguageItem("pa", "Punjabi"),
        LanguageItem("ta", "Tamil"),
        LanguageItem("te", "Telugu"),
        LanguageItem("ml", "Malayalam"),
        LanguageItem("kn", "Kannada"),
        LanguageItem("af", "Afrikaans"),
        LanguageItem("sq", "Albanian"),
        LanguageItem("az", "Azerbaijani (Latin)"),
        LanguageItem("bg", "Bulgarian"),
        LanguageItem("ca", "Catalan"),
        LanguageItem("hr", "Croatian"),
        LanguageItem("da", "Danish"),
        LanguageItem("et", "Estonian"),
        LanguageItem("fil", "Filipino"),
        LanguageItem("fi", "Finnish"),
        LanguageItem("el", "Greek"),
        LanguageItem("ja", "Japanese"),
        LanguageItem("kk", "Kazakh"),
        LanguageItem("ko", "Korean"),
        LanguageItem("lo", "Lao"),
        LanguageItem("lv", "Latvian"),
        LanguageItem("lt", "Lithuanian"),
        LanguageItem("mk", "Macedonian"),
        LanguageItem("no", "Norwegian"),
        LanguageItem("sr", "Serbian"),
        LanguageItem("sl", "Slovenian"),
        LanguageItem("sv", "Swedish"),
        LanguageItem("uz", "Uzbek"),
        LanguageItem("vi", "Vietnamese"),
        LanguageItem("ga", "Irish")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLanguagePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentLang = Lingver.getInstance().getLanguage()

        val adapter = LanguageAdapter(languageList, currentLang) { selected ->
            // Save to SharedPreferences
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("lang", selected.code).apply()

            // Apply the new locale
            Lingver.getInstance().setLocale(requireContext(), selected.code)

            // Recreate to apply the language immediately
            requireActivity().recreate()
        }

        binding.languageRecyclerView.adapter = adapter
        binding.languageRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.appLanguageBackIcon.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun updateTexts() {

        binding.appLanguageTitleTxt.text = getString(R.string.app_language)
        // add more view updates here if needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}