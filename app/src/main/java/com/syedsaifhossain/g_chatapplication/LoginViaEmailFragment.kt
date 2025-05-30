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
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginViaEmailBinding

class LoginViaEmailFragment : Fragment() {

    private var _binding: FragmentLoginViaEmailBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginViaEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.loginViaEmailBackArrow.setOnClickListener {
            findNavController().popBackStack()
        }

        // Listener for the "Register" button
        binding.loginViaEmailVerificationNextButton.setOnClickListener {
            val email = binding.loginViaEmailEdtEmail.text.toString().trim()
            val password = binding.loginViaEmailEdtPassword.text.toString().trim()

            if (validateInput(email, password)) {
                // Consider showing a progress bar here
                registerUser(email, password)
                writeUserToDatabase(password)
            }
        }
    }

    // 注册成功后写入数据库（请在注册成功的回调里调用此逻辑）
    fun writeUserToDatabase(password: String) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
            val nickname = "G-Chat User" // 你可以让用户输入昵称后再写入
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
                    android.widget.Toast.makeText(requireContext(), "用户信息写入成功", android.widget.Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    android.widget.Toast.makeText(requireContext(), "写入失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.loginViaEmailEdtEmail.error = "Email is required"
            binding.loginViaEmailEdtEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginViaEmailEdtEmail.error = "Enter a valid email address"
            binding.loginViaEmailEdtEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.loginViaEmailEdtPassword.error = "Password is required"
            binding.loginViaEmailEdtPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.loginViaEmailEdtPassword.error = "Password must be at least 6 characters"
            binding.loginViaEmailEdtPassword.requestFocus()
            return false
        }
        // You can add more complex password validation if needed
        return true
    }

    private fun registerUser(email: String, password: String) {
        // binding.yourProgressBar.visibility = View.VISIBLE // Show progress

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                // binding.yourProgressBar.visibility = View.GONE // Hide progress

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Registration successful. Please check your email for verification.",
                                    Toast.LENGTH_LONG
                                ).show()
                                navigateToHomePage()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to send verification email: ${verificationTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.e(
                                    "EmailVerification",
                                    "sendEmailVerification failed",
                                    verificationTask.exception
                                )
                            }
                        }
                } else {
                    // Handle errors
                    try {
                        throw task.exception!!
                    } catch (e: FirebaseAuthWeakPasswordException) {
                        binding.loginViaEmailEdtPassword.error = "Password is too weak. Please use a stronger password."
                        binding.loginViaEmailEdtPassword.requestFocus()
                        Log.w("RegisterUser", "Weak password", e)
                    } catch (e: FirebaseAuthInvalidCredentialsException) {
                        // This can be for malformed email or if Firebase rejects the email format
                        binding.loginViaEmailEdtEmail.error = "Invalid email format or other credential issue."
                        binding.loginViaEmailEdtEmail.requestFocus()
                        Log.w("RegisterUser", "Invalid credentials", e)
                    } catch (e: FirebaseAuthUserCollisionException) {
                        binding.loginViaEmailEdtEmail.error = "This email address is already in use."
                        binding.loginViaEmailEdtEmail.requestFocus()
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
        if (isAdded && findNavController().currentDestination?.id == R.id.loginViaEmailFragment) {
            try {
                findNavController().navigate(R.id.action_loginViaEmailFragment_to_profileSettingFragment)
            } catch (e: Exception) {
                Log.e("NavigationError", "Failed to navigate from LoginViaEmailFragment", e)
                Toast.makeText(requireContext(), "Navigation error. Please try logging in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}