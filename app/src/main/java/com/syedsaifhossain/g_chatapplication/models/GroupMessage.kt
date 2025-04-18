package com.syedsaifhossain.g_chatapplication.models

data class GroupMessage(
    val id: String? = null,
    val senderId: String? = null,
    val text: String? = null,
    var audioUrl: String? = null,
    val timestamp: Long? = null
)