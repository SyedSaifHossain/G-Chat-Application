package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
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

        // Listen for selected country result from the SelectRegionFragment
        parentFragmentManager.setFragmentResultListener("regionSelection", viewLifecycleOwner) { _, bundle ->
            val selectedCountry = bundle.getString("selectedCountry", "")
            binding.regionEdt.setText(selectedCountry)

            // Do not set country code automatically when country is selected
            // This is the change compared to the previous logic
        }

        // 🔥 Click on arrowImg to insert country code into phoneNumberEdt with a space
        binding.arrowImg.setOnClickListener {
            val selectedCountry = binding.regionEdt.text.toString().trim()

            if (countryCodeMap.containsKey(selectedCountry)) {
                val countryCode = countryCodeMap[selectedCountry]
                val enteredPhoneNumber = binding.phoneNumberEdt.text.toString().trim()

                // If phone number is empty, just set the country code with a space
                if (enteredPhoneNumber.isEmpty()) {
                    binding.phoneNumberEdt.setText("$countryCode ")
                } else {
                    // If phone number is entered, append it after the country code with a space
                    binding.phoneNumberEdt.setText("$countryCode $enteredPhoneNumber")
                }
            } else {
                Toast.makeText(requireContext(), "Please select a country first", Toast.LENGTH_SHORT).show()
            }

            // Move focus to phoneNumberEdt and open the keyboard
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

            // Check if the phone number is empty or just the country code (with space)
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

    // 🔥 Load country list dynamically using libphonenumber
    private fun loadCountryList() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val countryList = phoneUtil.supportedRegions.map { regionCode ->
            val countryName = Locale("", regionCode).displayCountry // Get country name
            val countryCode = "+${phoneUtil.getCountryCodeForRegion(regionCode)}" // Get country code
            countryCodeMap[countryName] = countryCode // Store in map
            countryName
        }.sorted()

        // **✅ Set adapter for regionEdt so users can see country name suggestions**
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryList)
        binding.regionEdt.setAdapter(adapter)
    }
}
