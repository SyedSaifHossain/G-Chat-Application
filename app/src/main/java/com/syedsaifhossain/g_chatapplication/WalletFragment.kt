package com.syedsaifhossain.g_chatapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.syedsaifhossain.g_chatapplication.api.RetrofitClient
import com.syedsaifhossain.g_chatapplication.databinding.FragmentWalletBinding
import com.syedsaifhossain.g_chatapplication.models.AddMoneyRequest
import com.syedsaifhossain.g_chatapplication.models.WithdrawRequest
import kotlinx.coroutines.launch

class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val userId = "user123"
    private lateinit var stripe: Stripe

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        stripe = Stripe(requireContext(), "pk_test_51RLbSP04tuQ3kToVdhQXXFrZyL516r9p5MzAUQWLhm26h3xpFtp2jfAMiv7gLufcwvSso2uoxfwiHxq1oY9kqJGE00fhlzQT93")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //binding.btnAddMoney.setOnClickListener { transferIn() }
       // binding.btnWithdraw.setOnClickListener { transferOut() }
       // binding.btnGetBalance.setOnClickListener { fetchBalance() }

        binding.topUpBtn.setOnClickListener {
            binding.topUpBtn.setBackgroundColor(Color.parseColor("#81d8d0"))
            binding.topUpBtn.setTextColor(Color.parseColor("#000000"))
            findNavController().navigate(R.id.action_walletFragment_to_topUpFragment)
        }

        binding.withdrawBtn.setOnClickListener {
            binding.withdrawBtn.setBackgroundColor(Color.parseColor("#81d8d0"))
            binding.withdrawBtn.setTextColor(Color.parseColor("#81d8d0"))
            findNavController().navigate(R.id.action_walletFragment_to_withdrawFragment)
        }

        binding.walletBackImg.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}