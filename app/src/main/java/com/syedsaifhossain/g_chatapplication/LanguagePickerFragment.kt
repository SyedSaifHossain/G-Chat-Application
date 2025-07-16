package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.LanguageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLanguagePickerBinding
import com.syedsaifhossain.g_chatapplication.models.LanguageItem
import com.yariksoffice.lingver.Lingver

class LanguagePickerFragment : Fragment() {
    private var _binding: FragmentLanguagePickerBinding? = null
    private val binding get() = _binding!!
    private val languageList = listOf(

    LanguageItem("English (US)", "English"),
    LanguageItem("Español", "Spanish"),
    LanguageItem("Português – Brasil", "Portuguese (Brazil)"),
    LanguageItem("Русский", "Russian"),
    LanguageItem("Bahasa Indonesia", "Indonesian"),
    LanguageItem("العربية", "Arabic"),
    LanguageItem("Français", "French"),
    LanguageItem("Deutsch", "German"),
    LanguageItem("Türkçe", "Turkish"),
    LanguageItem("Italiano", "Italian"),
    LanguageItem("हिन्दी", "Hindi"),
    LanguageItem("বাংলা", "Bengali"),
    LanguageItem("मराठी", "Marathi"),
    LanguageItem("اردو – پاکستان", "Urdu (Pakistan)"),
    LanguageItem("ગુજરાતી", "Gujarati"),
    LanguageItem("فارسی", "Persian"),
    LanguageItem("Nederlands", "Dutch"),
    LanguageItem("Polski", "Polish"),
    LanguageItem("Română", "Romanian"),
    LanguageItem("中文 (繁體) – 台灣", "Chinese (Traditional, Taiwan)"),
    LanguageItem("中文 (繁體) – 香港", "Chinese (Traditional, Hong Kong)"),
    LanguageItem("Bahasa Melayu", "Malay"),
    LanguageItem("עברית", "Hebrew"),
    LanguageItem("Czech", "Czech"),
    LanguageItem("Swahili", "Swahili"),
    LanguageItem("Українська", "Ukrainian"),
    LanguageItem("ไทย", "Thai"),
    LanguageItem("中文 (简体) – 中国", "Chinese (Simplified, China)"),
    LanguageItem("Magyar", "Hungarian"),
    LanguageItem("Slovenčina", "Slovak"),
    LanguageItem("Português – Portugal", "Portuguese (Portugal)"),
    LanguageItem("ਪੰਜਾਬੀ", "Punjabi"),
    LanguageItem("தமிழ்", "Tamil"),
    LanguageItem("తెలుగు", "Telugu"),
    LanguageItem("മലയാളം", "Malayalam"),
    LanguageItem("ಕನ್ನಡ", "Kannada"),
    LanguageItem("Afrikaans", "Afrikaans"),
    LanguageItem("Shqip", "Albanian"),
    LanguageItem("Azərbaycan (latın)", "Azerbaijani (Latin)"),
    LanguageItem("Български", "Bulgarian"),
    LanguageItem("Català", "Catalan"),
    LanguageItem("Hrvatski", "Croatian"),
    LanguageItem("Dansk", "Danish"),
    LanguageItem("Eesti", "Estonian"),
    LanguageItem("Filipino", "Filipino"),
    LanguageItem("Suomi", "Finnish"),
    LanguageItem("Ελληνικά", "Greek"),
    LanguageItem("日本語", "Japanese"),
    LanguageItem("Қазақ", "Kazakh"),
    LanguageItem("한국어", "Korean"),
    LanguageItem("Lao", "Lao"),
    LanguageItem("Latviešu", "Latvian"),
    LanguageItem("Lietuvių", "Lithuanian"),
    LanguageItem("Македонски", "Macedonian"),
    LanguageItem("Norsk", "Norwegian"),
    LanguageItem("Srpski (ћирилица/latinica)", "Serbian (Cyrillic/Latin)"),
    LanguageItem("Slovenščina", "Slovenian"),
    LanguageItem("Svenska", "Swedish"),
    LanguageItem("O‘zbek", "Uzbek"),
    LanguageItem("Tiếng Việt", "Vietnamese"),
    LanguageItem("Gaeilge", "Irish")

    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguagePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentLang = Lingver.getInstance().getLanguage()

        val adapter = LanguageAdapter(languageList, currentLang) { selected ->
            Lingver.getInstance().setLocale(requireContext(), selected.code)
            requireActivity().recreate() // restart to apply new locale
        }

        binding.languageRecyclerView.adapter = adapter
        binding.languageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}