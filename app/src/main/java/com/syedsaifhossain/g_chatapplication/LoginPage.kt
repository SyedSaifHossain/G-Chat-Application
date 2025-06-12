package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginPageBinding
import com.syedsaifhossain.g_chatapplication.models.User

class LoginPage : Fragment() {

    private var _binding: FragmentLoginPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化 Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            val phoneOrEmail = binding.phoneLoginEdt.text.toString().trim()
            val password = binding.passwordLoginEdt.text.toString().trim()

            if (validateInput(phoneOrEmail, password)) {
                // 尝试登录
                auth.signInWithEmailAndPassword(phoneOrEmail, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (_binding == null) return@addOnCompleteListener
                        if (task.isSuccessful) {
                            // 登录成功
                            val user = auth.currentUser
                            if (user != null) {
                                // 检查邮箱是否已验证
                                if (!user.isEmailVerified) {
                                    // 发送验证邮件
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { verificationTask ->
                                            if (_binding == null) return@addOnCompleteListener
                                            if (verificationTask.isSuccessful) {
                                                Toast.makeText(requireContext(), "Please check your email inbox and click the verification link before logging in.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(requireContext(), "Failed to send verification email: ${verificationTask.exception?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    // 退出登录
                                    auth.signOut()
                                    return@addOnCompleteListener
                                }
                                
                                // 更新用户在线状态
                                updateUserStatus(user.uid, true)
                                
                                // 获取用户信息
                                getUserInfo(user.uid) { userInfo ->
                                    if (_binding == null) return@getUserInfo
                                    if (userInfo != null) {
                                        Log.d("LoginPage", "User logged in successfully: ${userInfo.name}")
                                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                                        findNavController().navigate(R.id.action_loginPage_to_homeFragment)
                                    } else {
                                        Log.e("LoginPage", "Failed to get user info")
                                        Toast.makeText(requireContext(), "Failed to get user info", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            // 登录失败
                            val errorMessage = when {
                                task.exception?.message?.contains("no user record") == true -> "User does not exist"
                                task.exception?.message?.contains("password is invalid") == true -> "Incorrect password"
                                task.exception?.message?.contains("badly formatted") == true -> "Invalid email format"
                                task.exception?.message?.contains("malformed") == true -> "Invalid login credential format"
                                else -> "Login failed: ${task.exception?.message}"
                            }
                            Log.e("LoginPage", "Login failed: ${task.exception?.message}", task.exception)
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                        }
                        
                        binding.loginButton.text = "Log In"
                    }
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
            binding.phoneLoginEdt.error = "Please enter phone or email"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(phoneOrEmail).matches() &&
            !phoneOrEmail.matches(Regex("^[+]?[0-9]{10,13}\$"))) {
            binding.phoneLoginEdt.error = "Please enter a valid email address or phone number"
            return false
        }

        if (password.isEmpty()) {
            binding.passwordLoginEdt.error = "Please enter password"
            return false
        }

        if (password.length < 6) {
            binding.passwordLoginEdt.error = "Password must be at least 6 characters"
            return false
        }

        return true
    }

    private fun updateUserStatus(userId: String, isOnline: Boolean) {
        val userStatusRef = database.getReference("users").child(userId)
        val updates = mapOf(
            "isOnline" to isOnline,
            "lastSeen" to System.currentTimeMillis()
        )
        userStatusRef.updateChildren(updates)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                Log.d("LoginPage", "User status updated successfully")
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Log.e("LoginPage", "Failed to update user status", e)
            }
    }

    private fun getUserInfo(userId: String, callback: (User?) -> Unit) {
        database.getReference("users").child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                val user = snapshot.getValue(User::class.java)
                callback(user)
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                Log.e("LoginPage", "Failed to get user info", e)
                callback(null)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
