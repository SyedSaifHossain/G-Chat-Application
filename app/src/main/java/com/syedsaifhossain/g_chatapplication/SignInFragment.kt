package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {
    private lateinit var binding : FragmentSignInBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSignInBinding.inflate(inflater, container, false)


        return binding.root
    }

}