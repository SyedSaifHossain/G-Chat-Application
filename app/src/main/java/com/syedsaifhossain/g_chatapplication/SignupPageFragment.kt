package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignupPageBinding
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

class SignupPageFragment : Fragment() {
    private lateinit var binding: FragmentSignupPageBinding
    private lateinit var auth: FirebaseAuth
    private var selectedCountryName: String? = null
    private var phoneNumberWithCode: String? = null
    private val countryRegionMap = mutableMapOf<String, String>()
    private val countryCodeMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        initializeViews()
        setupClickListeners()
        setupCountryList()

        binding.signupViaEmailTxt.setOnClickListener {
            findNavController().navigate(R.id.action_signupPageFragment_to_loginViaEmailFragment)
        }

    }

    private fun initializeViews() {
        binding.signupPageNextButton.text = "Next"
    }

    private fun setupClickListeners() {
        binding.signupPageCountryEdit.setOnClickListener {
            findNavController().navigate(R.id.action_signupPageFragment_to_selectRegionFragment)
        }

        parentFragmentManager.setFragmentResultListener("regionSelection", viewLifecycleOwner) { _, bundle ->
            selectedCountryName = bundle.getString("selectedCountry", "")
            binding.signupPageCountryEdit.setText(selectedCountryName)

            val code = getCountryCode(selectedCountryName!!)

            val currentPhoneNumber = binding.signupPagePhoneEdt.text.toString()
            if (!currentPhoneNumber.startsWith(code)) {
                binding.signupPagePhoneEdt.setText(code)
                binding.signupPagePhoneEdt.setSelection(code.length)
            }
        }

        binding.signupPageNextButton.setOnClickListener {
            phoneNumberWithCode = binding.signupPagePhoneEdt.text.toString().trim()

            if (selectedCountryName.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please select a country/region", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isValidPhoneNumber(phoneNumberWithCode!!)) {
                val bundle = Bundle()
                bundle.putString("countryName", selectedCountryName)
                bundle.putString("phoneNumberWithCode", phoneNumberWithCode)
                findNavController().navigate(R.id.action_signupPageFragment_to_signupPageVerificationFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid phone number with country code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return try {
            val phoneNumberUtil = PhoneNumberUtil.getInstance()
            val number: PhoneNumber = phoneNumberUtil.parse(phone, null)
            phoneNumberUtil.isValidNumber(number)
        } catch (e: NumberParseException) {
            false
        }
    }

    private fun getCountryCode(countryName: String): String {
        return countryCodeMap[countryName] ?: "+1"
    }

    private fun setupCountryList() {
        val phoneUtil = PhoneNumberUtil.getInstance()

        val countryList = phoneUtil.supportedRegions.map { regionCode ->
            val countryName = java.util.Locale("", regionCode).displayCountry

            val countryCode = if (countryName == "Bangladesh") {
                "+880"
            } else {
                "+${phoneUtil.getCountryCodeForRegion(regionCode)}"
            }

            countryRegionMap[countryName] = regionCode
            countryCodeMap[countryName] = countryCode
            countryName
        }.sorted()

        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryList)
        binding.signupPageCountryEdit.setAdapter(adapter)

        binding.signupPageCountryEdit.setOnItemClickListener { _, _, position, _ ->
            selectedCountryName = countryList[position]
            val code = countryCodeMap[selectedCountryName] ?: "+1"
            binding.signupPagePhoneEdt.setText(code)
            binding.signupPagePhoneEdt.setSelection(code.length)
        }
    }

}