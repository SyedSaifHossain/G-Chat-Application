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
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignupPageVerificationBinding
import com.syedsaifhossain.g_chatapplication.models.User
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
            binding.resendCodeVerify.visibility = View.VISIBLE
            binding.resendCodeVerify.isEnabled = false
            startPhoneNumberVerification(phone)
        }

        binding.resendCodeVerify.setOnClickListener {
            val phone = binding.singupVerificationEdtPhoneEmail.text.toString().trim()
            if (resendToken != null) {
                startTimer()
                resendVerificationCode(phone, resendToken!!)
            } else {
                Toast.makeText(requireContext(), "Resend token not available yet", Toast.LENGTH_SHORT).show()
            }
        }

        binding.verificationNextButton.setOnClickListener {
            val code = binding.verificationEdt.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.length < 6) {
                Toast.makeText(requireContext(), "OTP must be 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (storedVerificationId != null) {
                val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
                signInWithPhoneAuthCredential(credential)
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
                binding.verificationEdt.setText(code)
                verifyPhoneNumberWithCode(storedVerificationId!!, code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            if (!isAdded) return
            binding.resendCodeVerify.isEnabled = true
            binding.timeCount.text = ""

            val message = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Invalid request: ${e.message}"
                is FirebaseTooManyRequestsException -> "SMS quota exceeded."
                else -> "Verification failed: ${e.message}"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
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
                    val user = auth.currentUser
                    val phoneNumber = user?.phoneNumber
                    val uid = user?.uid
                    addUserToDatabase(phoneNumber, uid)
                    navigateToHomePage()
                } else {
                    Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addUserToDatabase(phoneNumber: String?, uid: String?) {
        if (uid == null || phoneNumber == null) {
            Toast.makeText(requireContext(), "User info missing", Toast.LENGTH_SHORT).show()
            return
        }

        val user = User(
            name = null,
            phone = phoneNumber.filter { it.isDigit() }.toLongOrNull(),
            uid = uid
        )

        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        usersRef.child("user").child(uid).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "User saved to Realtime Database", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save user: ${e.message}", Toast.LENGTH_LONG).show()
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