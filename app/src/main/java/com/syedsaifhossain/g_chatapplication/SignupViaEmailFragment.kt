package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignupViaEmailBinding

class SignupViaEmailFragment : Fragment() {

    private var _binding: FragmentSignupViaEmailBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupViaEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.signupViaEmailBackArrow.setOnClickListener {
            findNavController().popBackStack()
        }

        // Listener for the "Register" button
        binding.signupViaEmailVerificationNextButton.setOnClickListener {
            val email = binding.signupViaEmailEdtEmail.text.toString().trim()
            val password = binding.signupViaEmailEdtPassword.text.toString().trim()

            if (validateInput(email, password)) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            // 注册成功，写入数据库
                            writeUserToDatabase(password)
                            Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_signupViaEmailFragment_to_profileSettingFragment)
                        } else {
                            // 注册失败，不写入数据库
                            Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(requireContext(), "Please enter valid email and password.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 注册成功后写入数据库（请在注册成功的回调里调用此逻辑）
    fun writeUserToDatabase(password: String) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
            val nickname = "G-Chat User"
            val userInfo = com.syedsaifhossain.g_chatapplication.models.User(
                uid = user.uid,
                name = nickname,
                phone = user.phoneNumber ?: "",
                email = user.email ?: "",
                password = password,
                avatarUrl = user.photoUrl?.toString() ?: "",
                status = "Hey there! I'm using G-Chat",
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
            dbRef.child(user.uid).setValue(userInfo)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(requireContext(), "User info saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    android.widget.Toast.makeText(requireContext(), "Failed to save user info: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.signupViaEmailEdtEmail.error = "Email is required"
            binding.signupViaEmailEdtEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signupViaEmailEdtEmail.error = "Enter a valid email address"
            binding.signupViaEmailEdtEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.signupViaEmailEdtPassword.error = "Password is required"
            binding.signupViaEmailEdtPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.signupViaEmailEdtPassword.error = "Password must be at least 6 characters"
            binding.signupViaEmailEdtPassword.requestFocus()
            return false
        }
        // You can add more complex password validation if needed
        return true
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.sendEmailVerification()
                    Toast.makeText(
                        requireContext(),
                        "Please check your email inbox and click the verification link before logging in.",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_signupViaEmailFragment_to_loginPage)
                } else {
                    try {
                        throw task.exception!!
                    } catch (e: FirebaseAuthWeakPasswordException) {
                        binding.signupViaEmailEdtPassword.error = "Password is too weak. Please use a stronger password."
                        binding.signupViaEmailEdtPassword.requestFocus()
                        Log.w("RegisterUser", "Weak password", e)
                    } catch (e: FirebaseAuthInvalidCredentialsException) {
                        binding.signupViaEmailEdtEmail.error = "Invalid email format"
                        binding.signupViaEmailEdtEmail.requestFocus()
                        Log.w("RegisterUser", "Invalid credentials", e)
                    } catch (e: FirebaseAuthUserCollisionException) {
                        binding.signupViaEmailEdtEmail.error = "This email is already registered"
                        binding.signupViaEmailEdtEmail.requestFocus()
                        Log.w("RegisterUser", "User collision", e)
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Registration failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("RegisterUser", "Registration failed", e)
                    }
                }
            }
    }

    private fun navigateToHomePage() {
        // Check if fragment is still added and if current destination is correct before navigating
        if (isAdded && findNavController().currentDestination?.id == R.id.signupViaEmailFragment) {
            try {
                findNavController().navigate(R.id.action_signupViaEmailFragment_to_homeFragment)
            } catch (e: Exception) {
                Log.e("NavigationError", "Failed to navigate from SignupViaEmailFragment", e)
                Toast.makeText(requireContext(), "Navigation error. Please try logging in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}