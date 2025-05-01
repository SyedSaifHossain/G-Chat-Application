package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignupPageVerificationBinding
import java.util.concurrent.TimeUnit

class SignupPageVerificationFragment : Fragment() {
    private lateinit var binding: FragmentSignupPageVerificationBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var resendTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var phoneNumber: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupPageVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Retrieve phone number and country name from arguments
        arguments?.let {
            phoneNumber = it.getString("phoneNumber")
            val countryName = it.getString("countryName")
            binding.singupPageCountryEdit.setText(countryName)
            binding.singupVerificationEdtPhoneEmail.setText(phoneNumber)
        }

        setupClickListeners()
        startResendTimer() // Initial start of the timer
    }

    private fun setupClickListeners() {
        binding.sendCode.setOnClickListener {
            if (!isTimerRunning && phoneNumber != null) {
                disableButtons(true)
                sendVerificationCode(phoneNumber!!)
                startResendTimer()
            } else {
                Toast.makeText(requireContext(), "Please wait before resending the code", Toast.LENGTH_SHORT).show()
            }
        }

        binding.signupVerificationNextButton.setOnClickListener {
            val code = binding.verificationEdt.text.toString().trim()
            if (code.length == 6 && verificationId != null) {
                disableButtons(true)
                verifyCode(code)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        binding.resendCode.setOnClickListener {
            if (!isTimerRunning && phoneNumber != null) {
                disableButtons(true)
                resendVerificationCode(phoneNumber!!)
                startResendTimer()
            } else {
                Toast.makeText(requireContext(), "Please wait before resending the code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableButtons(disable: Boolean) {
        binding.sendCode.isEnabled = !disable
        binding.signupVerificationNextButton.isEnabled = !disable
        binding.resendCode.isEnabled = !disable
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        var timeLeft = 60
        binding.sendCode.isEnabled = false
        binding.resendCode.isEnabled = false
        isTimerRunning = true

        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.sendCode.text = "${timeLeft}s"
                timeLeft--
            }

            override fun onFinish() {
                isTimerRunning = false
                binding.sendCode.isEnabled = true
                binding.resendCode.isEnabled = true
                binding.sendCode.text = "Send"
            }
        }.start()
    }

    private fun handleError(exception: Exception) {
        disableButtons(false)
        isTimerRunning = false
        binding.sendCode.text = "Send"

        when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(requireContext(), "Invalid credentials. Please try again", Toast.LENGTH_SHORT).show()
            }
            is FirebaseTooManyRequestsException -> {
                Toast.makeText(requireContext(), "Too many requests. Please try again later", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    disableButtons(false)
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    disableButtons(false)
                    handleError(e)
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken = token
                    disableButtons(false)
                    Toast.makeText(requireContext(), "OTP sent to your phone", Toast.LENGTH_SHORT).show()
                    startResendTimer()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(phoneNumber: String) {
        resendToken?.let { token ->
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        disableButtons(false)
                        signInWithCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        disableButtons(false)
                        handleError(e)
                    }

                    override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                        verificationId = id
                        resendToken = token
                        disableButtons(false)
                        Toast.makeText(requireContext(), "OTP resent to your phone", Toast.LENGTH_SHORT).show()
                        startResendTimer()
                    }
                })
                .setForceResendingToken(token)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } ?: run {
            Toast.makeText(requireContext(), "Cannot resend code at this time.", Toast.LENGTH_SHORT).show()
            disableButtons(false)
        }
    }

    private fun verifyCode(code: String) {
        verificationId?.let {
            val credential = PhoneAuthProvider.getCredential(it, code)
            signInWithCredential(credential)
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                disableButtons(false)
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Authentication successful!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_signupPageVerificationFragment_to_homeFragment) // Replace with your actual home navigation action
                } else {
                    task.exception?.let { handleError(it) }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendTimer?.cancel()
    }
}