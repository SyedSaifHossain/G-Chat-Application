package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSelectRegionBinding
import java.util.Locale

class SelectRegionFragment : Fragment() {
    private lateinit var binding: FragmentSelectRegionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectRegionBinding.inflate(inflater, container, false)

        // Fetch country list dynamically using libphonenumber
        val phoneUtil = PhoneNumberUtil.getInstance()
        val countryList = phoneUtil.supportedRegions.map { regionCode ->
            Locale("", regionCode).displayCountry // Get localized country name
        }.sorted()

        // Setup adapter for ListView
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, countryList)
        binding.listView.adapter = adapter

        // Handle item selection
        binding.listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCountry = countryList[position]

            // Send result back to SignInFragment
            val bundle = Bundle()
            bundle.putString("selectedCountry", selectedCountry)
            parentFragmentManager.setFragmentResult("regionSelection", bundle)
            findNavController().navigateUp() // Go back to SignInFragment
        }

        return binding.root
    }
}