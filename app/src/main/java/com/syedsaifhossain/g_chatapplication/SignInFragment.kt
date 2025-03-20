package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {
    private lateinit var binding : FragmentSignInBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSignInBinding.inflate(inflater, container, false)

binding.signInButton.setOnClickListener{
    findNavController().navigate(R.id.signInFragment_to_signInNextFragment)
}
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Region Selection Click Event
        binding.arrowImg.setOnClickListener {

            Toast.makeText(requireContext(), "Select Region Clicked", Toast.LENGTH_SHORT).show()
        }

        // Login via Email Click Event
        binding.loginViaEmailTxt.setOnClickListener {
            Toast.makeText(requireContext(), "Login via Email Clicked", Toast.LENGTH_SHORT).show()
        }

        // Next Button Click Event
        binding.signInButton.setOnClickListener {
            val phoneNumber = binding.phoneNumberEdt.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your phone number", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Proceeding with phone: $phoneNumber", Toast.LENGTH_SHORT).show()

            }
        }
    }

}