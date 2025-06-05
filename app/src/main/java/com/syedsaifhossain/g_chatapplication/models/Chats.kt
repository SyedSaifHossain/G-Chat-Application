package com.syedsaifhossain.g_chatapplication.models

import androidx.annotation.DrawableRes

/**
 * Data class representing an item in the chat list.
 */
data class Chats(
    @DrawableRes val imageRes: Int = 0,      // Optional, for local avatar resource
    val name: String = "",                   // Other user's name
    val message: String = "",                // Last message preview
    val type: String = "text",               // 新增，消息类型：text/voice
    val otherUserId: String = "",            // ID of the other user
    val otherUserAvatarUrl: String? = null,  // URL string for the other user's avatar
    val isGroup: Boolean = false,             // 新增

    // --- Add these fields for FirebaseManager compatibility ---
    val senderId: String = "",               // Who sent the last message
    val receiverId: String = "",             // Who received the last message
    val lastMessageTime: Long = 0,           // Timestamp of last message
    val lastMessageSenderId: String = ""     // Last message sender's ID
)