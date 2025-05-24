package com.syedsaifhossain.g_chatapplication.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val avatarUrl: String = "",
    val status: String = "Hey there! I'm using G-Chat",
    val lastSeen: Long = 0,
    val isOnline: Boolean = false,
    val fcmToken: String = ""
)