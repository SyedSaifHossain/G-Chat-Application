package com.syedsaifhossain.g_chatapplication.models

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val type: String = "text", // text, image, voice, video
    val status: String = "sent", // sent, delivered, read
    val mediaUrl: String = "",
    val isDeleted: Boolean = false
)