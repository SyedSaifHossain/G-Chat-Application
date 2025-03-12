package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignUpBinding
import com.syedsaifhossain.g_chatapplication.databinding.FragmentWelcomeBinding

class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        binding.nextButton.setOnClickListener{
            findNavController().navigate(R.id.signUpToSignupNext)
        }
        return binding.root
    }
}