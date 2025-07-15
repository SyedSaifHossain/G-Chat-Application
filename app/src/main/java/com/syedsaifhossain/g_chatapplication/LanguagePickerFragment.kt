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
        LanguageItem("English", "en"),
        LanguageItem("বাংলা", "bn"),
        LanguageItem("Español", "es"),
        LanguageItem("Français", "fr"),
        LanguageItem("Deutsch", "de"),
        LanguageItem("中文", "zh"),
        LanguageItem("हिन्दी", "hi"),
        LanguageItem("Русский", "ru"),
        LanguageItem("日本語", "ja"),
        LanguageItem("한국어", "ko"),
        // Add more as needed
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