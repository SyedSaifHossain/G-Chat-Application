package com.syedsaifhossain.g_chatapplication.models

data class User(
    val name: String? = null,
    val phone: String? = null,
    val uid: String? = null,
    val profilePicUrl: Int? = null,
    val lastMessage: String? = null
)