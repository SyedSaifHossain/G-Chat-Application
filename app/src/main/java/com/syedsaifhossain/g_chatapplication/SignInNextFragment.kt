package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignInNextBinding

class SignInNextFragment : Fragment() {
private lateinit var binding : FragmentSignInNextBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSignInNextBinding.inflate(inflater,container,false)

        binding.signPageeLoginButton.setOnClickListener {

            val phoneNumber = binding.signInPagePhoneEdt.text.toString().trim()

            val password = binding.passwordEdtSignInNext.text.toString().trim()
            // Validate the inputs
            if (phoneNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please provide the cridential",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else if (phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            } else if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }


            if (!isPasswordValid(password)) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 8 characters long and contain both letters and numbers",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            } else {
                Toast.makeText(requireContext(), "Sign Up Successful!", Toast.LENGTH_SHORT).show()

                // For now, just clear the fields
                binding.signInPagePhoneEdt.text.clear()
                binding.passwordEdtSignInNext.text.clear()
            }
            findNavController().navigate(R.id.signInNextFragment_to_homeFragment)

        }
        return binding.root
    }

    private fun isPasswordValid(password: String): Boolean {
        // Check password length
        if (password.length < 8) {
            return false
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        return hasLetter && hasDigit
    }

}