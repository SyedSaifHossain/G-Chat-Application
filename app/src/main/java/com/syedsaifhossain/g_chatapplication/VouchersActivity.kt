package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.VoucherAdapter
import com.syedsaifhossain.g_chatapplication.databinding.ActivityVouchersBinding
import com.syedsaifhossain.g_chatapplication.models.Voucher

class VouchersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVouchersBinding
    private lateinit var adapter: VoucherAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVouchersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置标题栏
        supportActionBar?.apply {
            title = "My Vouchers"
            setDisplayHomeAsUpEnabled(true)
        }

        setupRecyclerView()
        loadTestData()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = VoucherAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@VouchersActivity)
            adapter = this@VouchersActivity.adapter
        }
    }

    private fun loadTestData() {
        val testVouchers = listOf(
            Voucher(
                id = "1",
                title = "50% Off Restaurant Voucher",
                description = "Get 50% off on your next meal at selected restaurants",
                validUntil = "2024-12-31",
                status = "Active"
            ),
            Voucher(
                id = "2",
                title = "Hotel Discount Voucher",
                description = "20% discount on your next hotel booking",
                validUntil = "2024-12-31",
                status = "Active"
            ),
            Voucher(
                id = "3",
                title = "Spa Treatment Voucher",
                description = "Free spa treatment with any package booking",
                validUntil = "2024-12-31",
                status = "Active"
            )
        )

        // 如果有测试数据，显示RecyclerView，隐藏空状态提示
        if (testVouchers.isNotEmpty()) {
            binding.recyclerView.visibility = android.view.View.VISIBLE
            adapter.submitList(testVouchers)
        }
    }

    // 处理返回按钮点击
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}