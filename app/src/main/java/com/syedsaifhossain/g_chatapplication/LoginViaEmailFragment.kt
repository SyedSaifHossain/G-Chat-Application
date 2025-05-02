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
import com.google.firebase.auth.*
import com.syedsaifhossain.g_chatapplication.databinding.FragmentLoginViaEmailBinding
import java.util.concurrent.TimeUnit

class LoginViaEmailFragment : Fragment() {

    private var _binding: FragmentLoginViaEmailBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginViaEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        auth = FirebaseAuth.getInstance()

        binding.loginViaEmailResendCodeVerify.visibility = View.INVISIBLE

        var isLoginFlow = false

        arguments?.let {
            val phoneNumber = it.getString("phoneNumberWithCode")
            val countryName = it.getString("countryName")
            isLoginFlow = it.getBoolean("isLogin", false)

            binding.loginViaEmailCountryEdt.setText(countryName)
            binding.loginViaEmailEdtEmail.setText(phoneNumber)
        }


        binding.loginViaEmailCountryEdt.setOnClickListener {
            findNavController().navigate(R.id.action_loginViaEmailFragment_to_selectRegionFragment)
        }

        binding.loginViaEmailSendCodeVerify.setOnClickListener {
            val phone = binding.loginViaEmailEdtEmail.text.toString().trim()
            val country = binding.loginViaEmailCountryEdt.text.toString().trim()

            if (phone.isEmpty()) {
                Toast.makeText(requireContext(), "Phone number is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!phone.startsWith("+") || phone.length < 11) {
                Toast.makeText(requireContext(), "Enter a valid phone number with country code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (country.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a country", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Sending OTP to $country $phone", Toast.LENGTH_SHORT).show()
            binding.loginViaEmailResendCodeVerify.visibility = View.VISIBLE
            binding.loginViaEmailResendCodeVerify.isEnabled = false
            startPhoneNumberVerification(phone)
        }

        binding.loginViaEmailResendCodeVerify.setOnClickListener {
            val phone = binding.loginViaEmailEdtEmail.text.toString().trim()

            if (resendToken != null) {
                startTimer()
                resendVerificationCode(phone, resendToken!!)
            } else {
                Toast.makeText(requireContext(), "Resend token not available yet", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginViaEmailVerificationNextButton.setOnClickListener {

            val code = binding.loginViaEmailVerificationEdt.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.length < 6) {
                Toast.makeText(requireContext(), "OTP must be 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (storedVerificationId != null) {
                verifyPhoneNumberWithCode(storedVerificationId!!, code)
            } else {
                Toast.makeText(requireContext(), "Verification ID not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        startTimer()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(phoneNumber: String, token: PhoneAuthProvider.ForceResendingToken) {
        startTimer()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            if (!isAdded) return
            val code = credential.smsCode
            if (code != null) {
                binding.loginViaEmailVerificationEdt.setText(code)
                verifyPhoneNumberWithCode(storedVerificationId!!, code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            if (!isAdded) return
            binding.loginViaEmailResendCodeVerify.isEnabled = true
            binding.loginViaEmailTimeCount.text = ""

            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    Toast.makeText(requireContext(), "Invalid request: ${e.message}", Toast.LENGTH_LONG).show()
                }
                is FirebaseTooManyRequestsException -> {
                    Toast.makeText(requireContext(), "SMS quota exceeded.", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            storedVerificationId = verificationId
            resendToken = token
            Toast.makeText(requireContext(), "OTP Sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Verification successful", Toast.LENGTH_SHORT).show()
                    navigateToHomePage()
                } else {
                    Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        binding.loginViaEmailResendCodeVerify.isEnabled = false
        binding.loginViaEmailResendCodeVerify.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.loginViaEmailTimeCount.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.loginViaEmailTimeCount.text = ""
                binding.loginViaEmailTimeCount.isEnabled = true
            }
        }.start()
    }

    private fun navigateToHomePage() {
        findNavController().navigate(R.id.action_loginViaEmailFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        countDownTimer?.cancel()
    }

}