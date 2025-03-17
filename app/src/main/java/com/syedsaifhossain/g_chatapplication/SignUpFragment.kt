package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignUpBinding
import java.util.concurrent.TimeUnit

class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Send OTP Button
        binding.sendCode.setOnClickListener {
            val phoneNumber = binding.edtPhoneEmail.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                sendVerificationCode(phoneNumber)
            } else {
                Toast.makeText(requireContext(), "Enter phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Resend Code Button
        binding.resendCodeButton.setOnClickListener {
            val phoneNumber = binding.edtPhoneEmail.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                sendVerificationCode(phoneNumber)
            } else {
                Toast.makeText(requireContext(), "Enter phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Verify OTP Button
        binding.nextButton.setOnClickListener {
            val code = binding.verificationEdt.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                verifyCode(code)
            } else {
                Toast.makeText(requireContext(), "Enter valid verification code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to send OTP
    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(requireContext(), "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    Toast.makeText(requireContext(), "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Function to verify the OTP code
    private fun verifyCode(code: String) {
        verificationId?.let {
            val credential = PhoneAuthProvider.getCredential(it, code)
            signInWithCredential(credential)
        }
    }

    // Function to sign in with OTP credential
    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Authentication Successful!", Toast.LENGTH_SHORT).show()
                    // Navigate to the next screen (e.g., home page)
                    findNavController().navigate(R.id.signUpToSignupNext)
                } else {
                    Toast.makeText(requireContext(), "Authentication Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
