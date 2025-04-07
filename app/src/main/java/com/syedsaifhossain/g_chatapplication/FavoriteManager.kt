package com.syedsaifhossain.g_chatapplication

import android.content.Context
import android.content.SharedPreferences

object FavoriteManager {

    private const val PREFS_NAME = "FavoritePrefs" // SharedPreferences 文件名
    private const val FAVORITE_PREFIX = "favorite_" // Key 的前缀，用于区分

    // 获取 SharedPreferences 实例
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 检查指定的 Deal 是否已收藏
     * @param context Context
     * @param dealId Deal 的唯一 ID
     * @return true 如果已收藏，否则 false
     */
    fun isFavorite(context: Context, dealId: String): Boolean {
        // 确保 context 不为空
        val safeContext = context ?: return false // 如果 context 为空，直接返回 false
        val prefs = getPreferences(safeContext)
        return prefs.getBoolean(FAVORITE_PREFIX + dealId, false) // 默认返回 false (未收藏)
    }

    /**
     * 设置 Deal 的收藏状态
     * @param context Context
     * @param dealId Deal 的唯一 ID
     * @param isFavorite 新的收藏状态 (true 或 false)
     */
    fun setFavorite(context: Context, dealId: String, isFavorite: Boolean) {
        // 确保 context 不为空
        val safeContext = context ?: return // 如果 context 为空，直接返回
        val prefs = getPreferences(safeContext)
        prefs.edit().putBoolean(FAVORITE_PREFIX + dealId, isFavorite).apply()
    }

    /**
     * (可选) 获取所有收藏的 Deal ID 列表
     * @param context Context
     * @return 包含所有已收藏 Deal ID 的 Set
     */
    fun getAllFavoriteIds(context: Context): Set<String> {
        // 确保 context 不为空
        val safeContext = context ?: return emptySet() // 如果 context 为空，返回空 Set
        val prefs = getPreferences(safeContext)
        // 过滤所有 Key，找到以 FAVORITE_PREFIX 开头且值为 true 的条目
        return prefs.all.filterKeys { it.startsWith(FAVORITE_PREFIX) }
            .filterValues { it is Boolean && it == true } // 确保值是 true
            .map { it.key.removePrefix(FAVORITE_PREFIX) } // 移除前缀，得到 Deal ID
            .toSet()
    }
}