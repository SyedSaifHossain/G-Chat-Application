package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSplashScreenBinding

class SplashScreenFragment : Fragment() {
    private lateinit var binding: FragmentSplashScreenBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSplashScreenBinding.inflate(inflater, container, false)

        // Set a delay of 3 seconds (3000 milliseconds)
        Handler(Looper.getMainLooper()).postDelayed({
            // Navigate to the WelcomeFragment after 3 seconds
            findNavController().navigate(R.id.action_splashScreenFragment_to_welcomeFragment)
        }, 3000)

        return binding.root
    }
}