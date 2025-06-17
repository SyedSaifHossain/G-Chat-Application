package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
    }
}