package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentTopUpBinding
import com.syedsaifhossain.g_chatapplication.databinding.FragmentWalletBinding


class TopUpFragment : Fragment() {

    private lateinit var binding: FragmentTopUpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentTopUpBinding.inflate(inflater, container, false)


        binding.topUpBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.topUpMoreOption.setOnClickListener {

        }
        return binding.root
    }
}