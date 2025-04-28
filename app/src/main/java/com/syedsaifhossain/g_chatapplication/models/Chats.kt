package com.syedsaifhossain.g_chatapplication.models

import androidx.annotation.DrawableRes // Import needed for DrawableRes

/**
 * Data class representing an item in the chat list.
 */
data class Chats(
    // Existing fields:
    @DrawableRes val imageRes: Int,      // TODO: Replace with otherUserAvatarUrl ideally
    val name: String,                   // Use this as otherUserName for navigation
    val message: String,                // Last message preview

    // --- ADDED FIELDS ---
    val otherUserId: String,            // Required: ID of the other user
    val otherUserAvatarUrl: String? = null // Optional: URL string for the other user's avatar
)