package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.N) // Requires API 24+
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCountryList() // Load country names and codes dynamically

        // Set event when user selects a country
        binding.regionEdt.setOnItemClickListener { _, _, position, _ ->
            val selectedCountry = binding.regionEdt.adapter.getItem(position).toString()
            val countryCode = countryCodeMap[selectedCountry]
            binding.phoneNumberEdt.setText(countryCode)
        }

        // Auto-fill country code when typing country name
        binding.regionEdt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputText = s.toString().trim()
                if (countryCodeMap.containsKey(inputText)) {
                    binding.phoneNumberEdt.setText(countryCodeMap[inputText])
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

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

    // Load country list dynamically
    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadCountryList() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val countryList = phoneUtil.supportedRegions.map { regionCode ->
            val countryName = Locale("", regionCode).displayCountry // Get country name
            val countryCode = "+${phoneUtil.getCountryCodeForRegion(regionCode)}" // Get country code
            countryCodeMap[countryName] = countryCode // Store in map
            countryName
        }.sorted()

        // Set adapter to dropdown
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryList)
        binding.regionEdt.setAdapter(adapter)
    }
}