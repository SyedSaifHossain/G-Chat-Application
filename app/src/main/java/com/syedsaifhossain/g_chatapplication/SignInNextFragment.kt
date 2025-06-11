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
    private lateinit var binding: FragmentSignInNextBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInNextBinding.inflate(inflater, container, false)

        // Get phone number from Bundle
        val fullPhoneNumber = arguments?.getString("fullPhoneNumber")

        // Set phone number if available
        if (!fullPhoneNumber.isNullOrEmpty()) {
            binding.signInPagePhoneEdt.setText(fullPhoneNumber)
        }

        binding.signPageeLoginButton.setOnClickListener {//TODO 有漏洞需要补全
            val phoneNumber = binding.signInPagePhoneEdt.text.toString().trim()
            val password = binding.passwordEdtSignInNext.text.toString().trim()

            // Validate the inputs
            if (phoneNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please provide the credentials",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!isPasswordValid(password)) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 8 characters long and contain both letters and numbers",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Sign Up Successful!", Toast.LENGTH_SHORT).show()

            // Clear fields after sign-in
            binding.signInPagePhoneEdt.text.clear()
            binding.passwordEdtSignInNext.text.clear()

            findNavController().navigate(R.id.signInNextFragment_to_homeFragment)
        }

        return binding.root
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8 && password.any {
            it.isLetter() } && password.any { it.isDigit() }
    }
}