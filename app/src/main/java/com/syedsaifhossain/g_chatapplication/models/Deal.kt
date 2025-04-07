package com.syedsaifhossain.g_chatapplication.models

import android.os.Parcelable // 导入 Parcelable
import kotlinx.parcelize.Parcelize // 导入 Parcelize

@Parcelize // 添加 @Parcelize 注解
data class Deal(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val originalPrice: Double,
    val imageUrl: String,
    var isFavorite: Boolean = false
) : Parcelable // 实现 Parcelable 接口