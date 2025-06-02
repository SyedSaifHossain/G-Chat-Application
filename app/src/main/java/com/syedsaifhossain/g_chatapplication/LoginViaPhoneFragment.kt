package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginViaPhoneBinding
import java.util.concurrent.TimeUnit

class LoginViaPhoneFragment : Fragment() {

    // Declare ViewBinding variable
    private lateinit var binding: FragmentLoginViaPhoneBinding
    private lateinit var auth: FirebaseAuth  // FirebaseAuth instance

    private var verificationId: String? = null // Variable to hold the verification ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using ViewBinding
        binding = FragmentLoginViaPhoneBinding.inflate(inflater, container, false)

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Set up listeners or any interaction logic
        setupUI()

        return binding.root
    }

    private fun setupUI() {
        // Set click listener for the "Send" button
        binding.sendCode.setOnClickListener {
            val phoneNumber = binding.phoneInputViaPhoneEdt.text.toString().trim()

            if (isValidPhoneNumber(phoneNumber)) {
                // Send verification code using Firebase
                sendVerificationCode(phoneNumber)
            }
        }

        // Set click listener for the "Login" button
        binding.loginViaPhoneBtn.setOnClickListener {
            val verificationCode = binding.verifyInputViaPhoneEdt.text.toString().trim()

            if (verificationCode.isNotEmpty() && verificationId != null) {
                // Verify the entered verification code
                verifyPhoneNumberWithCode(verificationCode)
            } else {
                Toast.makeText(requireContext(), "Please enter verification code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Validate phone number using regex pattern
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return when {
            TextUtils.isEmpty(phoneNumber) -> {
                binding.phoneInputViaPhoneEdt.error = "Phone number is required"
                false
            }
            phoneNumber.length < 10 -> {
                binding.phoneInputViaPhoneEdt.error = "Phone number is too short"
                false
            }
            else -> true
        }
    }

    // Send verification code using Firebase Phone Authentication
    private fun sendVerificationCode(phoneNumber: String) {
        val options = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Automatically verify code if received
                verifyPhoneNumberWithCode(credential.smsCode!!)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // Save the verification ID for later verification
                super.onCodeSent(verificationId, token)
                this@LoginViaPhoneFragment.verificationId = verificationId
                Toast.makeText(requireContext(), "Verification code sent", Toast.LENGTH_SHORT).show()
            }
        }

        // Start phone number verification
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber, // Phone number to verify
            60, // Timeout duration
            TimeUnit.SECONDS, // Timeout unit
            requireActivity(), // Activity context
            options // Callbacks for verification process
        )
    }

    // Verify the code entered by the user
    private fun verifyPhoneNumberWithCode(verificationCode: String) {
        val verificationId = this.verificationId // Retrieve the verification ID from onCodeSent callback
        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)

            auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Login success
                        val user = auth.currentUser
                        if (user != null) {
                            val phoneNumber = user.phoneNumber ?: ""
                            // 写入用户信息到Realtime Database
                            val dbRef = FirebaseDatabase.getInstance().getReference("users")
                            val userInfo = com.syedsaifhossain.g_chatapplication.models.User(
                                uid = user.uid,
                                name = "G-Chat User", // 你可以让用户输入昵称后再写入
                                phone = phoneNumber,
                                email = user.email ?: "",
                                avatarUrl = user.photoUrl?.toString() ?: "",
                                status = "Hey there! I'm using G-Chat",
                                isOnline = true,
                                lastSeen = System.currentTimeMillis()
                            )
                            dbRef.child(user.uid).setValue(userInfo)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "User info saved successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Failed to save user info: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            Toast.makeText(requireContext(), "Logged in as $phoneNumber", Toast.LENGTH_SHORT).show()
                        }
                        // Navigate to the home screen after successful login
                        findNavController().navigate(R.id.action_loginViaPhoneFragment_to_homeFragment)
                    } else {
                        // If verification fails
                        Toast.makeText(requireContext(), "Verification Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "Verification ID not found", Toast.LENGTH_SHORT).show()
        }
    }
}