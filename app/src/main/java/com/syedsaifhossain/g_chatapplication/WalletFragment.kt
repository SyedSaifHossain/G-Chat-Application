package com.syedsaifhossain.g_chatapplication

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
        binding.btnGetBalance.setOnClickListener { fetchBalance() }

        binding.topUpBtn.setOnClickListener {
            findNavController().navigate(R.id.action_walletFragment_to_topUpFragment)
        }

        binding.withdrawBtn.setOnClickListener {
            findNavController().navigate(R.id.action_walletFragment_to_withdrawFragment)
        }

        binding.walletBackImg.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun transferIn() {
        lifecycleScope.launch {
            try {
                val amount = binding.etAmount.text.toString().toInt() * 100
                val response = RetrofitClient.api.addMoney(AddMoneyRequest(amount, userId))
                val params = ConfirmPaymentIntentParams.create(
                    clientSecret = response.clientSecret,
                    paymentMethodType = PaymentMethod.Type.Card
                )
                stripe.confirmPayment(requireActivity(), params)
                Toast.makeText(requireContext(), "Payment Confirmed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun transferOut() {
        lifecycleScope.launch {
            try {
                val amount = binding.etAmount.text.toString().toInt() * 100
                val response = RetrofitClient.api.withdraw(WithdrawRequest(amount, userId))
                if (response.success == true) {
                    Toast.makeText(requireContext(), "Withdrawal successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error: ${response.error}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Withdraw failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchBalance() {
        lifecycleScope.launch {
            try {
                val balance = RetrofitClient.api.getBalance(userId).balance
                binding.tvBalance.text = "Balance: \$${balance / 100.0}"
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}