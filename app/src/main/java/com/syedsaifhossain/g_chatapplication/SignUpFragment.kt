package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignUpBinding
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.google.i18n.phonenumbers.NumberParseException
import java.util.concurrent.TimeUnit

class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private val countryCodeMap: Map<String, String> = mapOf(
        "Bangladesh" to "+880",
        "India" to "+91",
        "United States" to "+1",
        // Add more countries and their respective codes
    )

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
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        binding.sendCode.text = "Send the code"
    }

    private fun setupClickListeners() {
        // Open SelectRegionFragment when regionEdt is clicked

        binding.countryEdit.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_selectRegionFragment)
        }

        // Listen for selected country result
        parentFragmentManager.setFragmentResultListener("regionSelection", viewLifecycleOwner) { _, bundle ->
            val selectedCountry = bundle.getString("selectedCountry", "")

            binding.countryEdit.setText(selectedCountry)

            // Auto-fill country code
            val countryCode = countryCodeMap[selectedCountry]
            binding.edtPhoneEmail.setText(countryCode)
        }

        // Send code button
        binding.sendCode.setOnClickListener {
            val phoneNumber = binding.edtPhoneEmail.text.toString().trim()

            // Validate phone number
            if (isValidPhoneNumber(phoneNumber)) {
                if (!isTimerRunning) {
                    // Disable the button to prevent multiple clicks
                    disableButtons(true)

                    // Send the verification code
                    sendVerificationCode(phoneNumber)

                    // Show toast confirming that the code is being sent
                    Toast.makeText(requireContext(), "Code has been sent", Toast.LENGTH_SHORT).show()

                    // Start the resend timer
                    startResendTimer()
                } else {
                    // If the timer is running, show a message indicating the user needs to wait
                    Toast.makeText(requireContext(), "Please wait before resending the code", Toast.LENGTH_SHORT).show()
                }
            } else {
                // If the phone number is invalid, show a toast
                Toast.makeText(requireContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }


        // Verify button
        binding.nextButton.setOnClickListener {
            val code = binding.verificationEdt.text.toString().trim()
            if (code.length == 6 && verificationId != null) {
                disableButtons(true)
                verifyCode(code)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Use libphonenumber to check the validity of the phone number
        return try {
            val phoneNumberUtil = PhoneNumberUtil.getInstance()
            val number: PhoneNumber = phoneNumberUtil.parse(phone, "")
            phoneNumberUtil.isValidNumber(number)
        } catch (e: NumberParseException) {
            false
        }
    }

    private fun disableButtons(disable: Boolean) {
        binding.sendCode.isEnabled = !disable
        binding.nextButton.isEnabled = !disable
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        var timeLeft = 60
        binding.sendCode.isEnabled = false
        isTimerRunning = true

        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.sendCode.text = "${timeLeft}s"
                timeLeft--
            }

            override fun onFinish() {
                isTimerRunning = false
                binding.sendCode.isEnabled = true
                binding.sendCode.text = "Resend the code"

                // Add animation effect to notify user
                binding.sendCode.alpha = 0.7f
                binding.sendCode.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction {
                        binding.sendCode.alpha = 1f
                    }
                    .start()
            }
        }.start()
    }

    private fun handleError(exception: Exception) {
        disableButtons(false)
        isTimerRunning = false
        binding.sendCode.text = "Send the code"

        when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_PHONE_NUMBER" -> {
                        Toast.makeText(requireContext(), "Invalid phone number format", Toast.LENGTH_SHORT).show()
                    }
                    "ERROR_INVALID_VERIFICATION_CODE" -> {
                        Toast.makeText(requireContext(), "Invalid verification code", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(requireContext(), "Verification failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
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
        // Use libphonenumber to parse and validate the phone number
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        val parsedNumber: PhoneNumber = phoneNumberUtil.parse(phoneNumber, "")

        val countryCode = parsedNumber.countryCode
        val regionCode = phoneNumberUtil.getRegionCodeForCountryCode(countryCode)

        // Format the phone number to include the country code, e.g., +880 for Bangladesh
        val formattedPhoneNumber = if (!phoneNumber.startsWith("+$countryCode")) "+$countryCode$phoneNumber" else phoneNumber

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhoneNumber)
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
                    disableButtons(false)
                    binding.nextButton.isEnabled = true
                    Toast.makeText(requireContext(), "Verification code sent successfully", Toast.LENGTH_SHORT).show()
                    startResendTimer()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
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
                    findNavController().navigate(R.id.signUpToSignupNext)
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