package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginPageBinding

class LoginPage : Fragment() {

    private var _binding: FragmentLoginPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginPageBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener {
            val input = binding.phoneLoginEdt.text.toString().trim()
            val password = binding.passwordLoginEdt.text.toString().trim()

            if (input.isEmpty()) {
                showToast("Please enter phone or email")
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                showToast("Please enter your password")
                return@setOnClickListener
            }

            if (isValidEmail(input)) {
                loginWithEmail(input, password)
            } else if (isValidPhone(input)) {
                val fakeEmail = "$input@yourapp.com"  // 👈 use same format as sign-up
                loginWithEmail(fakeEmail, password)
            } else {
                showToast("Invalid phone or email format")
            }
        }

        binding.backToSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginPage_to_signupPageFragment)
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                showToast("Login successful")
                findNavController().navigate(R.id.action_loginPage_to_homeFragment)
            }
            .addOnFailureListener {
                showToast("Login failed: ${it.localizedMessage}")
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.startsWith("+") && phone.length >= 10
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}