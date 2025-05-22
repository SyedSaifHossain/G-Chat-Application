package com.syedsaifhossain.g_chatapplication.models

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = "",
    val senderProfileImage: String = ""
) 