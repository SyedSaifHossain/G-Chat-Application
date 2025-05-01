package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentWelcomeBinding


class WelcomeFragment : Fragment() {

    private lateinit var binding: FragmentWelcomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        binding.signUpButton.setOnClickListener{
            findNavController().navigate(R.id.action_welcomeFragment_to_signupPageFragment)
        }


        binding.signInButton.setOnClickListener{

            findNavController().navigate(R.id.welcomeFragmentToSignIn)
        }
        return binding.root
    }
}