package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
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

    private var isLoginFlow = false

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
            val userName = it.getString("userName") ?: ""
            val password = it.getString("password") ?: ""
            
            binding.singupPageCountryEdit.setText(countryName)
            binding.singupVerificationEdtPhoneEmail.setText(phoneNumber)
            binding.singupVerificationEdtPhoneEmail.tag = userName // 存储用户名
            binding.singupVerificationEdtPhoneEmail.tag = password // 存储密码
        }

        binding.singupPageCountryEdit.setOnClickListener {
            findNavController().navigate(R.id.action_signupPageVerificationFragment_to_selectRegionFragment)
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

           // Toast.makeText(requireContext(), "Sending OTP to $country $phone", Toast.LENGTH_SHORT).show()
            binding.resendCodeVerify.visibility = View.VISIBLE
            binding.resendCodeVerify.isEnabled = false
            startPhoneNumberVerification(phone)
        }

        binding.resendCodeVerify.setOnClickListener {
            val phone = binding.singupVerificationEdtPhoneEmail.text.toString().trim()
            if (resendToken != null) {
                Toast.makeText(requireContext(), "Resending OTP...", Toast.LENGTH_SHORT).show()
                resendVerificationCode(phone, resendToken!!)
            } else {
                Toast.makeText(requireContext(), "Resend token not available yet", Toast.LENGTH_SHORT).show()
                Log.e("VERIFICATION", "Resend token is null")
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
                verifyPhoneNumberWithCode(storedVerificationId!!, code)
            } else {
                Toast.makeText(requireContext(), "Verification ID not found", Toast.LENGTH_SHORT).show()
            }
            navigateToHomePage()
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
        Log.d("VERIFICATION", "Started phone number verification for $phoneNumber")
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
        Log.d("VERIFICATION", "Resent verification code to $phoneNumber")
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            if (!isAdded) return
            val code = credential.smsCode
            Log.d("VERIFICATION", "Auto OTP received: $code")
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
                is FirebaseAuthInvalidCredentialsException -> "请求无效: ${e.message}"
                is FirebaseTooManyRequestsException -> "短信请求过于频繁，请稍后再试。"
                else -> "验证码发送失败: ${e.message}"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            Log.e("VERIFICATION", "onVerificationFailed: ${e.message}", e)
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            storedVerificationId = verificationId
            resendToken = token
            binding.resendCodeVerify.isEnabled = false
            startTimer()
            Log.d("VERIFICATION", "onCodeSent called. VerificationId: $verificationId")
            Toast.makeText(requireContext(), "验证码已发送", Toast.LENGTH_SHORT).show()
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
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                    dbRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            // 已注册用户，更新在线状态
                            dbRef.child("isOnline").setValue(true)
                            dbRef.child("lastSeen").setValue(System.currentTimeMillis())
                            // TODO: 跳转到主界面
                        } else {
                            // 新用户注册，写入完整信息
                            val phone = binding.singupVerificationEdtPhoneEmail.text.toString().trim()
                            val name = arguments?.getString("userName") ?: "G-Chat User"
                            val password = arguments?.getString("password") ?: ""
                            val user = User(
                                uid = uid,
                                name = name,
                                phone = phone,
                                email = "",
                                password = password,
                                avatarUrl = "",
                                status = "Hey there! I'm using G-Chat",
                                isOnline = true,
                                lastSeen = System.currentTimeMillis()
                            )
                            dbRef.setValue(user)
                            // TODO: 跳转到主界面
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "登录失败: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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

        findNavController().navigate(R.id.action_signupPageVerificationFragment_to_profileSettingFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        countDownTimer?.cancel()
    }
}