package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginPageBinding


class LoginPage : Fragment() {

    private var _binding: FragmentLoginPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val phoneOrEmail = binding.phoneLoginEdt.text.toString().trim()
            val password = binding.passwordLoginEdt.text.toString().trim()

            if (validateInput(phoneOrEmail, password)) {
                Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginPage_to_homeFragment)
            }
        }

        binding.backToSignup.setOnClickListener {
           findNavController().navigate(R.id.action_loginPage_to_signupPageFragment)
        }

        binding.loginBackArrow.setOnClickListener {
            findNavController().popBackStack()
        }
    }


    private fun validateInput(phoneOrEmail: String, password: String): Boolean {
        if (phoneOrEmail.isEmpty()) {
            binding.phoneLoginEdt.error = "Phone or Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(phoneOrEmail).matches() &&
            !phoneOrEmail.matches(Regex("^[+]?[0-9]{10,13}\$"))) {
            binding.phoneLoginEdt.error = "Enter a valid email or phone number"
            return false
        }

        if (password.isEmpty()) {
            binding.passwordLoginEdt.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            binding.passwordLoginEdt.error = "Password must be at least 6 characters"
            return false
        }

        val passwordPattern = Regex("^(?=.*[A-Za-z])(?=.*\\d).{6,}$")
        if (!password.matches(passwordPattern)) {
            binding.passwordLoginEdt.error = "Password must include at least one letter and one digit"
            return false
        }
        return true
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
