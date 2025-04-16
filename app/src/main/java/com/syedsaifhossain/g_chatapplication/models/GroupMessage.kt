package com.syedsaifhossain.g_chatapplication.models

data class GroupMessage(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0
)