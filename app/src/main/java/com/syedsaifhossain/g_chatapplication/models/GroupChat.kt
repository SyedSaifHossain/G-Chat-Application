package com.syedsaifhossain.g_chatapplication.models

data class GroupChat(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val members: List<String> = emptyList()
) 