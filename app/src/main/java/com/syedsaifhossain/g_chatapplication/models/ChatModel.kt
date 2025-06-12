package com.syedsaifhossain.g_chatapplication.models

data class ChatModel(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long? = null,
    val imageUrl: String? = null,
    val type: String = "text",
    val duration: Int = 0,
    val messageId: String = "", // Added: unique message ID
    val deleted: Boolean = false, // renamed from isDeleted
    val isEdited: Boolean = false, // Added: message edit status
    val editTimestamp: Long? = null // Added: edit timestamp
)