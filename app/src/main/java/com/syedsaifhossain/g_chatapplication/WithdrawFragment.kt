package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentWithdrawBinding

class WithdrawFragment : Fragment() {
private lateinit var binding : FragmentWithdrawBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentWithdrawBinding.inflate(inflater, container, false)
        binding.withdrawBackImg.setOnClickListener {
            findNavController().popBackStack()
        }
        return binding.root

    }
}