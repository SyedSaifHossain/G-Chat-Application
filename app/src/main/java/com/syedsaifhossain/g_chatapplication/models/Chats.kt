package com.syedsaifhossain.g_chatapplication.models

import androidx.annotation.DrawableRes

data class Chats(
    @DrawableRes val imageRes: Int = 0,      // Optional: Avatar image resource
    val name: String = "",                   // User's name
    val message: String = "",                // Preview of the last message
    val otherUserId: String = "",            // ID of the other user
    val otherUserAvatarUrl: String? = null,  // URL of the other user's avatar

    // --- Add these fields for Firebase compatibility ---
    val senderId: String = "",               // ID of the user who sent the message
    val receiverId: String = "",             // ID of the user who received the message
    val lastMessageTime: Long = 0,           // Timestamp of last message
    val lastMessageSenderId: String = "",    // ID of the sender of the last message

    // --- New Fields for Text and Voice Messages ---
    val type: String = "text",                // "text" or "voice"
    val content: String = "",                 // Message content (text or voice URL)
)