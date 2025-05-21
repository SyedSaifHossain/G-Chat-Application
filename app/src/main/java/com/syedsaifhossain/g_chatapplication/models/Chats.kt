package com.syedsaifhossain.g_chatapplication.models

import androidx.annotation.DrawableRes

/**
 * Data class representing an item in the chat list.
 */
data class Chats(
    @DrawableRes val imageRes: Int = 0,      // Optional, for local avatar resource
    val name: String = "",                   // Other user's name
    val message: String = "",                // Last message preview
    val otherUserId: String = "",            // ID of the other user
    val otherUserAvatarUrl: String? = null,  // URL string for the other user's avatar

    // --- Add these fields for FirebaseManager compatibility ---
    val senderId: String = "",               // Who sent the last message
    val receiverId: String = "",             // Who received the last message
    val lastMessageTime: Long = 0,           // Timestamp of last message
    val lastMessageSenderId: String = ""     // Last message sender's ID
)