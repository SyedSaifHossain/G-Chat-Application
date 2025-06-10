package com.syedsaifhossain.g_chatapplication.models

data class Contact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val type: Int = TYPE_NORMAL
) {
    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_NEW_FRIENDS = 1
        const val TYPE_GROUP_CHATS = 2
        const val TYPE_SECTION = 3 // 字母分组
        // 可扩展更多类型
    }
}