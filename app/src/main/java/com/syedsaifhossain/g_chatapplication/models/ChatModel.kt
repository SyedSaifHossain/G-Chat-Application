package com.syedsaifhossain.g_chatapplication.models

data class ChatModel(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long? = null,
    val imageUrl: String? = null,
    val type: String = "text",
    val duration: Int = 0
)