package com.syedsaifhossain.g_chatapplication.models

data class User(
    val name: String? = null,
    val phone: Long? = null,
    val uid: String? = null,
    val profilePicUrl: String? = null,
    val lastMessage: String? = null
)