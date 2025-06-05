package com.syedsaifhossain.g_chatapplication.models

data class NewChatItem(
    val uid: String,
    val name: String,
    val avatarResId: Int,
    var isSelected: Boolean = false
)
