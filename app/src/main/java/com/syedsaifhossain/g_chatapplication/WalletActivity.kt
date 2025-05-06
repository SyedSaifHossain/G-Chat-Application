package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.syedsaifhossain.g_chatapplication.api.RetrofitClient
import com.syedsaifhossain.g_chatapplication.databinding.ActivityWalletBinding
import com.syedsaifhossain.g_chatapplication.models.AddMoneyRequest
import com.syedsaifhossain.g_chatapplication.models.WithdrawRequest
import kotlinx.coroutines.launch

class WalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalletBinding
    private val userId = "user123"
    private val stripe by lazy {
        Stripe(applicationContext, "pk_test_51RLbSP04tuQ3kToVdhQXXFrZyL516r9p5MzAUQWLhm26h3xpFtp2jfAMiv7gLufcwvSso2uoxfwiHxq1oY9kqJGE00fhlzQT93")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddMoney.setOnClickListener { transferIn() }
        binding.btnWithdraw.setOnClickListener { transferOut() }
        binding.btnGetBalance.setOnClickListener { fetchBalance() }
    }

    private fun transferIn() {
        lifecycleScope.launch {
            try {
                val amount = binding.etAmount.text.toString().toInt() * 100
                val response = RetrofitClient.api
                    .addMoney(AddMoneyRequest(amount, userId))
                val params = ConfirmPaymentIntentParams.create(
                    clientSecret = response.clientSecret,
                    paymentMethodType = PaymentMethod.Type.Card
                )
                stripe.confirmPayment(this@WalletActivity, params)
                Toast.makeText(this@WalletActivity, "Payment Confirmed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun transferOut() {
        lifecycleScope.launch {
            try {
                val amount = binding.etAmount.text.toString().toInt() * 100
                val response = RetrofitClient.api.withdraw(WithdrawRequest(amount, userId))
                if (response.success == true) {
                    Toast.makeText(this@WalletActivity, "Withdrawal successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@WalletActivity, "Error: ${response.error}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WalletActivity, "Withdraw failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchBalance() {
        lifecycleScope.launch {
            try {
                val balance = RetrofitClient.api.getBalance(userId).balance
                binding.tvBalance.text = "Balance: \$${balance / 100.0}"
            } catch (e: Exception) {
                Toast.makeText(this@WalletActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}