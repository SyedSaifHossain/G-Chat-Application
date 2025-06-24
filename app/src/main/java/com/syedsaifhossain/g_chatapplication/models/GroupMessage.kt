package com.syedsaifhossain.g_chatapplication.models

data class GroupMessage(
    val id: String? = null,
    val senderId: String? = null,
    val text: String? = null,
    var audioUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val timestamp: Long? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val type: String = "text",
    val duration: Int = 0,
    val messageId: String = "",
    val deleted: Boolean = false,
    val isEdited: Boolean = false,
    val editTimestamp: Long? = null,
    val senderName: String? = null,
    val senderAvatarUrl: String? = null
)