package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
// enableEdgeToEdge might need careful handling with locale changes,
// but we'll keep it for now as per "no UI change" instruction.
import androidx.activity.enableEdgeToEdge
// No longer directly inheriting from AppCompatActivity
// import androidx.appcompat.app.AppCompatActivity // This line is no longer needed

// Inherit from BaseActivity instead of AppCompatActivity
class MainActivity : BaseActivity() { // <--- This is the change

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Keep this for now

        // This line remains the same, setting the layout
        setContentView(R.layout.activity_main)
    }
}