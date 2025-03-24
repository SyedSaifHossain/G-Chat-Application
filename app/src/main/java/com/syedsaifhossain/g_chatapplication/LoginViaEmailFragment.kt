package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginViaEmailBinding

class LoginViaEmailFragment : Fragment() {

    // Declare ViewBinding variable
    private lateinit var binding: FragmentLoginViaEmailBinding
    private lateinit var auth: FirebaseAuth  // FirebaseAuth instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using ViewBinding
        binding = FragmentLoginViaEmailBinding.inflate(inflater, container, false)

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Set up listeners or any interaction logic
        setupUI()

        return binding.root
    }

    private fun setupUI() {
        // Set click listener for the button
        binding.completeButton.setOnClickListener {
            val email = binding.emailInputEdt.text.toString().trim()
            val password = binding.passwordInputEdt.text.toString().trim()

            if (isValidEmail(email) && isValidPassword(password)) {
                // Proceed with login logic using Firebase Authentication
                loginUser(email, password)
            }
        }
    }

    // Validate email using regex pattern
    private fun isValidEmail(email: String): Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                binding.emailInputEdt.error = "Email is required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailInputEdt.error = "Invalid email format"
                false
            }
            else -> true
        }
    }

    // Validate password (minimum length 8, contains both letters and numbers)
    private fun isValidPassword(password: String): Boolean {
        return when {
            TextUtils.isEmpty(password) -> {
                binding.passwordInputEdt.error = "Password is required"
                false
            }
            password.length < 8 -> { // Enforcing a minimum length of 8 characters
                binding.passwordInputEdt.error = "Password must be at least 8 characters"
                false
            }
            !password.matches(".*[A-Za-z].*".toRegex()) -> { // Ensuring it contains at least one letter
                binding.passwordInputEdt.error = "Password must contain at least one letter"
                false
            }
            !password.matches(".*[0-9].*".toRegex()) -> { // Ensuring it contains at least one number
                binding.passwordInputEdt.error = "Password must contain at least one number"
                false
            }
            else -> true
        }
    }

    // Login logic with Firebase Authentication
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Login success
                    val user = auth.currentUser

                    // Check if the user is not null and get the email
                    if (user != null) {
                        val email = user.email
                        Toast.makeText(activity, "Logged in as $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(activity, "No user is logged in", Toast.LENGTH_SHORT).show()
                    }

                    // Navigate to the home screen
                    findNavController().navigate(R.id.action_loginViaEmailFragment_to_homeFragment)
                } else {
                    // If login fails, show error
                    Toast.makeText(activity, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}