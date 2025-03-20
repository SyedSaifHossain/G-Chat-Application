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
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding

    // Country name to country code mapping
    private val countryCodeMap = mapOf(
        "Bangladesh" to "+88",
        "United States" to "+1",
        "United Kingdom" to "+44",
        "India" to "+91",
        "Canada" to "+1",
        "Australia" to "+61",
        "Germany" to "+49",
        "France" to "+33",
        "Italy" to "+39",
        "Japan" to "+81"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Convert country names to list for dropdown
        val countryNames = countryCodeMap.keys.toList()

        // Create an ArrayAdapter for the dropdown
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryNames)
        binding.regionEdt.setAdapter(adapter)

        // When user selects a country, set the phone number prefix
        binding.regionEdt.setOnItemClickListener { _, _, position, _ ->
            val selectedCountry = countryNames[position]
            val countryCode = countryCodeMap[selectedCountry]
            binding.phoneNumberEdt.setText(countryCode)
        }

        binding.arrowImg.setOnClickListener {
            binding.phoneNumberEdt.requestFocus()

            // Show the keyboard
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

            // Navigate to next fragment
            findNavController().navigate(R.id.signInFragment_to_signInNextFragment)
        }
    }

}