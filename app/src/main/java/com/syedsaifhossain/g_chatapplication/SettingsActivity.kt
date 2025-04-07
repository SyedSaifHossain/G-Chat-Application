package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.syedsaifhossain.g_chatapplication.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置标题栏
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
        }

        setupSettingsItems()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSettingsItems() {
        // 这里添加设置项的点击事件
        binding.apply {
            layoutLanguage.setOnClickListener {
                // 处理语言设置点击
            }

            layoutNotifications.setOnClickListener {
                // 处理通知设置点击
            }

            layoutPrivacy.setOnClickListener {
                // 处理隐私设置点击
            }

            layoutAbout.setOnClickListener {
                // 处理关于点击
            }
        }
    }

    // 处理返回按钮点击
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}