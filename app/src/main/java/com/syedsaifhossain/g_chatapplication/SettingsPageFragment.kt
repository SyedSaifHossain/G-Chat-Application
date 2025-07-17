package com.syedsaifhossain.g_chatapplication

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.adapter.LanguageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSettingsPageBinding
import com.syedsaifhossain.g_chatapplication.models.LanguageItem
import com.yariksoffice.lingver.Lingver

class SettingsPageFragment : Fragment() {

    private var _binding: FragmentSettingsPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingsBackIcon.setOnClickListener {
            findNavController().navigate(R.id.action_settingsPageFragment_to_mePageFragment)
        }

        binding.settingsHelpLayout.setOnClickListener {
            findNavController().navigate(R.id.action_settingsPageFragment_to_helpPageFragment)
        }

        binding.settingsAppLanguageLayout.setOnClickListener {
            findNavController().navigate(R.id.action_settingsPageFragment_to_languagePickerFragment)
        }

    }

    override fun onResume() {
        super.onResume()
        (parentFragment as? HomeFragment)?.showBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}