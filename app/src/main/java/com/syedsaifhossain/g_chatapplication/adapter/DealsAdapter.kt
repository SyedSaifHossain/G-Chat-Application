package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R // 确保导入 R
import com.syedsaifhossain.g_chatapplication.databinding.ItemDealBinding
import com.syedsaifhossain.g_chatapplication.models.Deal

// 添加一个 lambda 函数作为参数，用于处理收藏点击事件
class DealsAdapter(
    private val onFavoriteClicked: (Deal) -> Unit // 回调函数，当收藏状态改变时调用
) : RecyclerView.Adapter<DealsAdapter.DealViewHolder>() {

    private var deals = listOf<Deal>()

    inner class DealViewHolder(private val binding: ItemDealBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(deal: Deal) {
            binding.apply {
                tvDealTitle.text = deal.title
                tvDealDescription.text = deal.description
                tvDealPrice.text = "$${deal.price}"
                tvDealOriginalPrice.text = "$${deal.originalPrice}"

                // --- 开始添加代码 ---
                // 为原价设置删除线
                tvDealOriginalPrice.paintFlags = tvDealOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                // --- 结束添加代码 ---


                Glide.with(itemView.context)
                    .load(deal.imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivDealImage)

                updateFavoriteIcon(deal.isFavorite)

                btnFavorite.setOnClickListener {
                    deal.isFavorite = !deal.isFavorite
                    updateFavoriteIcon(deal.isFavorite)
                    onFavoriteClicked(deal)
                }
            }
        }
        // ... (updateFavoriteIcon 方法保持不变) ...


        // --- 新增方法 ---
        // 辅助方法，用于更新收藏按钮的图标
        private fun updateFavoriteIcon(isFavorite: Boolean) {
            binding.btnFavorite.setImageResource(
                if (isFavorite) R.drawable.ic_favorite_filled // 已收藏，显示实心图标
                else R.drawable.ic_favorite_border           // 未收藏，显示空心图标
            )
        }
        // --- 结束新增 ---
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DealViewHolder {
        val binding = ItemDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DealViewHolder, position: Int) {
        holder.bind(deals[position])
    }

    override fun getItemCount() = deals.size

    fun submitList(newDeals: List<Deal>) {
        deals = newDeals
        notifyDataSetChanged() // 注意：实际项目中建议使用 DiffUtil 提高效率
    }
}