package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignInBinding
import java.util.Locale

class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding
    private val countryCodeMap = mutableMapOf<String, String>() // Holds country name -> code mapping

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCountryList() // Load country names and codes dynamically

        // Open SelectRegionFragment when regionEdt is clicked
        binding.regionEdt.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_selectRegionFragment)
        }

        // Listen for selected country result
        parentFragmentManager.setFragmentResultListener("regionSelection", viewLifecycleOwner) { _, bundle ->
            val selectedCountry = bundle.getString("selectedCountry", "")
            binding.regionEdt.setText(selectedCountry)

            // Auto-fill country code
            val countryCode = countryCodeMap[selectedCountry]
            binding.phoneNumberEdt.setText(countryCode)
        }

        // Move focus to phoneNumberEdt when clicking arrowImg
        binding.arrowImg.setOnClickListener {
            binding.phoneNumberEdt.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.phoneNumberEdt, InputMethodManager.SHOW_IMPLICIT)
        }

        // Next Button Click Event
        binding.signInButton.setOnClickListener {
            val selectedCountry = binding.regionEdt.text.toString().trim()
            val phoneNumber = binding.phoneNumberEdt.text.toString().trim()

            if (!countryCodeMap.containsKey(selectedCountry)) {
                Toast.makeText(requireContext(), "Please select a valid country", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phoneNumber.isEmpty() || phoneNumber == countryCodeMap[selectedCountry]) {
                Toast.makeText(requireContext(), "Please enter your phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Proceeding with phone: $phoneNumber", Toast.LENGTH_SHORT).show()
            val bundle = Bundle()
            bundle.putString("phoneNumber", phoneNumber)
            findNavController().navigate(R.id.signInNextFragment, bundle)
        }
    }

    // Load country list dynamically using libphonenumber
    private fun loadCountryList() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val countryList = phoneUtil.supportedRegions.map { regionCode ->
            val countryName = Locale("", regionCode).displayCountry // Get country name
            val countryCode = "+${phoneUtil.getCountryCodeForRegion(regionCode)}" // Get country code
            countryCodeMap[countryName] = countryCode // Store in map
            countryName
        }.sorted()
    }
}