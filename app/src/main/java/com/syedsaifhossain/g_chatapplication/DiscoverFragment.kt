package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.syedsaifhossain.g_chatapplication.adapter.DealsAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentDiscoverBinding
import com.syedsaifhossain.g_chatapplication.models.Deal // 确保导入 Deal

// 确保导入 FavoriteManager
import com.syedsaifhossain.g_chatapplication.FavoriteManager

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private lateinit var dealsAdapter: DealsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupRecyclerView()
        loadDealsForCategory("All")
    }

    private fun setupTabs() {
        // ... (不变) ...
    }

    private fun setupRecyclerView() {
        dealsAdapter = DealsAdapter { deal ->
            handleFavoriteClick(deal)
        }
        binding.dealsRecyclerView.apply {
            this.layoutManager = GridLayoutManager(context, 2)
            this.adapter = dealsAdapter
        }
    }

    // 处理收藏点击的回调
    private fun handleFavoriteClick(deal: Deal) {
        // --- 开始修改 ---
        // 保存新的收藏状态到 SharedPreferences
        // 确保 requireContext() 不为空
        context?.let { ctx ->
            FavoriteManager.setFavorite(ctx, deal.id, deal.isFavorite)

            // (可选) 显示 Toast 消息
            val message = if (deal.isFavorite) {
                "Added ${deal.title} to favorites"
            } else {
                "Removed ${deal.title} from favorites"
            }
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
            Log.d("DiscoverFragment", "Favorite status saved for ${deal.id}: ${deal.isFavorite}")
        } ?: run {
            Log.e("DiscoverFragment", "Context is null, cannot save favorite status.")
        }
        // --- 结束修改 ---
    }


    private fun loadDealsForCategory(category: String) {
        Log.d("DiscoverFragment", "Loading deals for category: $category")
        // --- 开始修改 ---
        // 获取原始测试数据
        val rawTestData = getRawTestData()

        // 从 SharedPreferences 加载收藏状态并更新数据
        val updatedTestData = rawTestData.map { deal ->
            // 确保 requireContext() 不为空
            val isFav = context?.let { FavoriteManager.isFavorite(it, deal.id) } ?: false
            deal.copy(isFavorite = isFav) // 创建副本并更新 isFavorite 状态
        }
        // --- 结束修改 ---

        // 根据分类过滤并提交给 Adapter
        dealsAdapter.submitList(updatedTestData.filter { category == "All" || it.title.contains(category, ignoreCase = true) })
    }

    // 将原始数据和状态加载分开
    private fun getRawTestData(): List<Deal> {
        // 这里只返回基础数据，不包含 isFavorite 的初始状态（默认为 false 或由 Deal 类定义）
        return listOf(
            Deal("1", "Luxury Buffet", "Amazing buffet experience", 199.0, 399.0, "https://via.placeholder.com/300/FF0000/FFFFFF?text=Buffet"),
            Deal("2", "Premium SPA Package", "Relax and rejuvenate", 299.0, 599.0, "https://via.placeholder.com/300/00FF00/FFFFFF?text=SPA"),
            Deal("3", "Seafood Hotpot", "Fresh seafood hotpot", 258.0, 458.0, "https://via.placeholder.com/300/0000FF/FFFFFF?text=Hotpot"),
            Deal("4", "City Center Hotel", "Comfortable stay downtown", 150.0, 250.0, "https://via.placeholder.com/300/FFFF00/000000?text=Hotel"),
            Deal("5", "Beauty Salon Discount", "Discount on beauty treatments", 50.0, 100.0, "https://via.placeholder.com/300/FF00FF/FFFFFF?text=Beauty")
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}