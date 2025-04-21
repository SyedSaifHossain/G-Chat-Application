package com.syedsaifhossain.g_chatapplication.models

data class ChatModel(
    val senderId: String = "",
    val message: String = "",
    val imageUrl: String? = null, // for stickers/images
    val timestamp: Long = 0L
)