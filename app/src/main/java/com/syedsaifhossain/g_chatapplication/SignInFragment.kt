package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignInBinding
import java.util.Locale

class SignInFragment : Fragment() {

    private lateinit var binding: FragmentSignInBinding
    private val countryCodeMap = mutableMapOf<String, String>() // country name -> code

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCountryList()

        binding.regionEdt.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_selectRegionFragment)
        }

        parentFragmentManager.setFragmentResultListener("regionSelection", viewLifecycleOwner) { _, bundle ->
            val selectedCountry = bundle.getString("selectedCountry", "")
            binding.regionEdt.setText(selectedCountry)
            countryCodeMap[selectedCountry]?.let {
                binding.countryCodeTextView.text = it
            }
        }

        binding.arrowImg.setOnClickListener {
            val selectedCountry = binding.regionEdt.text.toString().trim()
            if (countryCodeMap.containsKey(selectedCountry)) {
                val enteredPhone = binding.phoneNumberEdt.text.toString().trim()
                binding.phoneNumberEdt.setText(enteredPhone)
            } else {
                Toast.makeText(requireContext(), "Please select a country first", Toast.LENGTH_SHORT).show()
            }

            binding.phoneNumberEdt.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.phoneNumberEdt, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.loginViaEmailTxt.setOnClickListener {

        }

        binding.signInButton.setOnClickListener {
            val selectedCountry = binding.regionEdt.text.toString().trim()
            val phone = binding.phoneNumberEdt.text.toString().trim()

            // --- Field Validations ---
            if (selectedCountry.isEmpty()) {
                binding.regionEdt.error = "Please select a country"
                binding.regionEdt.requestFocus()
                return@setOnClickListener
            }

            if (!countryCodeMap.containsKey(selectedCountry)) {
                Toast.makeText(requireContext(), "Invalid country selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                binding.phoneNumberEdt.error = "Phone number cannot be empty"
                binding.phoneNumberEdt.requestFocus()
                return@setOnClickListener
            }

            if (!phone.matches(Regex("^[0-9]{6,15}\$"))) {
                binding.phoneNumberEdt.error = "Enter a valid phone number"
                binding.phoneNumberEdt.requestFocus()
                return@setOnClickListener
            }

            val countryCode = countryCodeMap[selectedCountry]!!
            val fullPhoneNumber = "$countryCode $phone"

            Toast.makeText(requireContext(), "Using phone: $fullPhoneNumber", Toast.LENGTH_SHORT).show()

            // Passing phone to next fragment (optional)
            val bundle = Bundle().apply {
                putString("fullPhoneNumber", fullPhoneNumber)
            }
            // 注册成功后写入数据库
            writeUserToDatabase()
            findNavController().navigate(R.id.signInNextFragment, bundle)
        }
    }

    private fun loadCountryList() {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val countryList = phoneUtil.supportedRegions.map { regionCode ->
            val countryName = Locale("", regionCode).displayCountry
            val countryCode = "+${phoneUtil.getCountryCodeForRegion(regionCode)}"
            countryCodeMap[countryName] = countryCode
            countryName
        }.sorted()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryList)
        binding.regionEdt.setAdapter(adapter)
    }

    // 注册成功后写入数据库（请在注册成功的回调里调用此逻辑）
    fun writeUserToDatabase() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
            val nickname = "G-Chat User" // 你可以让用户输入昵称后再写入
            val userInfo = com.syedsaifhossain.g_chatapplication.models.User(
                uid = user.uid,
                name = nickname,
                phone = user.phoneNumber ?: "",
                email = user.email ?: "",
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
}