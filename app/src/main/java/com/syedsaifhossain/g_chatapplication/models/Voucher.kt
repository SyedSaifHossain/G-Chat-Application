package com.syedsaifhossain.g_chatapplication.models

data class Voucher(
    val id: String,
    val title: String,
    val description: String,
    val validUntil: String,
    val status: String
)