package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignUpBinding
import com.syedsaifhossain.g_chatapplication.databinding.FragmentWelcomeBinding

class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding

    private lateinit var selectedCountry: String
    private var countryCode: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        binding.nextButton.setOnClickListener{
            findNavController().navigate(R.id.signUpToSignupNext)
        }




        // Send code functionality
        binding.sendCode.setOnClickListener {
            val phoneOrEmail = binding.edtPhoneEmail.text.toString().trim()

            if (validateInputs(phoneOrEmail)) {
                // Check if input is a phone number or email
                if (isValidPhoneNumber(phoneOrEmail)) {
                    // If it's a phone number, prepend the country code
                    val fullPhoneNumber = "$countryCode$phoneOrEmail"
                    Toast.makeText(requireContext(), "Sending verification code to $fullPhoneNumber", Toast.LENGTH_SHORT).show()
                    // Call your API or service to send the verification code to the phone number
                } else if (isValidEmail(phoneOrEmail)) {
                    // If it's an email, send verification code to the email address
                    Toast.makeText(requireContext(), "Sending verification code to $phoneOrEmail", Toast.LENGTH_SHORT).show()
                    // Call your API or service to send the verification code to the email
                }
            }
        }

        // Resend code functionality
        binding.resendCodeButton.setOnClickListener {
            val phoneOrEmail = binding.edtPhoneEmail.text.toString().trim()

            if (validateInputs(phoneOrEmail)) {
                // Check if input is a phone number or email
                if (isValidPhoneNumber(phoneOrEmail)) {
                    // If it's a phone number, prepend the country code
                    val fullPhoneNumber = "$countryCode$phoneOrEmail"
                    Toast.makeText(requireContext(), "Resending verification code to $fullPhoneNumber", Toast.LENGTH_SHORT).show()
                    // Call your API or service to resend the verification code to the phone number
                } else if (isValidEmail(phoneOrEmail)) {
                    // If it's an email, resend verification code to the email address
                    Toast.makeText(requireContext(), "Resending verification code to $phoneOrEmail", Toast.LENGTH_SHORT).show()
                    // Call your API or service to resend the verification code to the email
                }
            }
        }

        return binding.root
    }

    // Function to update the country code based on the selected country
    private fun updateCountryCode(country: String) {
        when (country) {
            "USA" -> countryCode = "+1"
            "India" -> countryCode = "+91"
            "Canada" -> countryCode = "+1"
            "Australia" -> countryCode = "+61"
            // Add more countries and their country codes as needed
            else -> countryCode = ""
        }
    }

    // Basic validation of input fields
    private fun validateInputs(phoneOrEmail: String): Boolean {
        if (phoneOrEmail.isEmpty()) {
            binding.edtPhoneEmail.error = "Please enter your phone number or email"
            return false
        }
        return true
    }

    // Check if the input is a valid phone number
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^\\d{10}$")) // Simple check for 10-digit phone number
    }

    // Check if the input is a valid email address
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

















