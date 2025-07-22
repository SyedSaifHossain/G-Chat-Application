package com.syedsaifhossain.g_chatapplication.models

import com.google.firebase.Timestamp

data class Moment(
    val id: String = "",
    val userId: String = "",
    val day: String = "",
    val month: String = "",
    val imageResId: Int = 0,
    val imageUrl: String? = null,
    val momentText: String = "",
    val timestamp: Timestamp? = null
)