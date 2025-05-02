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

    private var _binding: FragmentSignupPageVerificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupPageVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        auth = FirebaseAuth.getInstance()

        binding.resendCodeVerify.visibility = View.INVISIBLE

        arguments?.let {
            val phoneNumber = it.getString("phoneNumberWithCode")
            val countryName = it.getString("countryName")

            binding.singupPageCountryEdit.setText(countryName)
            binding.singupVerificationEdtPhoneEmail.setText(phoneNumber)
        }

        binding.singupPageCountryEdit.setOnClickListener {
            findNavController().navigate(R.id.action_signupPageFragment_to_selectRegionFragment)
        }


        binding.sendCodeVerify.setOnClickListener {
            val phone = binding.singupVerificationEdtPhoneEmail.text.toString().trim()
            val country = binding.singupPageCountryEdit.text.toString().trim()

            if (phone.isNotEmpty() && phone.startsWith("+")) {
                Toast.makeText(requireContext(), "Sending OTP to $country $phone", Toast.LENGTH_SHORT).show()

                binding.resendCodeVerify.visibility = View.VISIBLE
                binding.resendCodeVerify.isEnabled = false
                startPhoneNumberVerification(phone)
            }
            else {
                Toast.makeText(
                    requireContext(),
                    "Phone number must include country code, e.g., +8801234567890",
                    Toast.LENGTH_LONG
                ).show()

                // ✅ Hide resend button if phone is invalid
                binding.resendCodeVerify.visibility = View.INVISIBLE
                binding.timeCount.text = ""
            }
        }


        binding.resendCodeVerify.setOnClickListener {
            val phone = binding.singupVerificationEdtPhoneEmail.text.toString()

            if (resendToken != null) {
                startTimer() // Start the countdown immediately
                resendVerificationCode(phone, resendToken!!)
            } else {
                Toast.makeText(requireContext(), "Resend token not available yet", Toast.LENGTH_SHORT).show()
            }
        }



        binding.verificationNextButton.setOnClickListener {
            val code = binding.verificationEdt.text.toString()
            if (code.isNotEmpty() && storedVerificationId != null) {
                verifyPhoneNumberWithCode(storedVerificationId!!, code)
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
                binding.verificationEdt.setText(code)
                verifyPhoneNumberWithCode(storedVerificationId!!, code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            if (!isAdded) return
            binding.resendCodeVerify.isEnabled = true
            binding.timeCount.text = ""

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
        binding.resendCodeVerify.isEnabled = false
        binding.resendCodeVerify.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timeCount.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.timeCount.text = ""
                binding.resendCodeVerify.isEnabled = true
            }
        }.start()
    }



    private fun navigateToHomePage() {
        findNavController().navigate(R.id.action_signupPageVerificationFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        countDownTimer?.cancel()
    }

}