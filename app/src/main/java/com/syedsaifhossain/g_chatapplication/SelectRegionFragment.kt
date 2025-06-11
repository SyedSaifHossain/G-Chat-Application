package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf // More concise way to create Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSelectRegionBinding
import java.util.Locale

// Data class to hold country information
data class CountryInfo(val name: String, val regionCode: String, val phoneCode: String)

class SelectRegionFragment : Fragment() {
    private lateinit var binding: FragmentSelectRegionBinding
    // Store the list of CountryInfo objects
    private lateinit var countryInfoList: List<CountryInfo>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectRegionBinding.inflate(inflater, container, false)

        // Fetch country list with details
        val phoneUtil = PhoneNumberUtil.getInstance()
        countryInfoList = phoneUtil.supportedRegions.mapNotNull { regionCode ->
            val countryName = Locale("", regionCode).displayCountry
            val phoneCodeInt = phoneUtil.getCountryCodeForRegion(regionCode)
            // Ensure we have a valid name and code
            if (countryName.isNotBlank() && phoneCodeInt != 0) {
                 // Format phone code with "+"
                CountryInfo(name = countryName, regionCode = regionCode, phoneCode = "+$phoneCodeInt")
            } else {
                null // Exclude regions without valid data
            }
        }.sortedBy { it.name } // Sort by country name

        // Create a list of strings for the adapter (e.g., "United States (+1)")
        val displayList = countryInfoList.map { "${it.name} (${it.phoneCode})" }

        // Setup adapter for ListView
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_country, R.id.countryText, displayList)

        binding.listView.adapter = adapter

        // Handle item selection
        binding.listView.setOnItemClickListener { _, _, position, _ ->
            // Get the selected CountryInfo object
            val selectedCountryInfo = countryInfoList[position]

            // Prepare the result bundle with both name and phone code
            val result = bundleOf(
                "selectedCountry" to selectedCountryInfo.name,
                "selectedPhoneCode" to selectedCountryInfo.phoneCode // Send phone code back
            )

            // Send result back using the key "regionSelection"
            parentFragmentManager.setFragmentResult("regionSelection", result)
            // Navigate back
            findNavController().navigateUp()
        }

        return binding.root
    }
}