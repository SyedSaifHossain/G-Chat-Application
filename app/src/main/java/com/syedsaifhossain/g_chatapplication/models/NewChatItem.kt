package com.syedsaifhossain.g_chatapplication.models

data class NewChatItem(
    val name: String,
    val avatarResId: Int,
    var isSelected: Boolean = false
)
