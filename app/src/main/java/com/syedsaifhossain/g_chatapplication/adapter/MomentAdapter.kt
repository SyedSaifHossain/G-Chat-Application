package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.databinding.MomentLayoutBinding
import com.syedsaifhossain.g_chatapplication.models.Moment

class MomentAdapter(private val momentList: List<Moment>) :
    RecyclerView.Adapter<MomentAdapter.MomentViewHolder>() {

    inner class MomentViewHolder(private val binding: MomentLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(moment: Moment) {
            binding.momentday.text = moment.day
            binding.momentMonth.text = moment.month
            binding.momentTxt.text = moment.momentText
            
            // 加载图片
            if (moment.imageUrl != null && moment.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(moment.imageUrl)

                    .placeholder(R.drawable.profilenew)
                    .into(binding.momentImg)
            } else if (moment.imageResId != 0) {
                binding.momentImg.setImageResource(moment.imageResId)
            } else {
                binding.momentImg.setImageResource(R.drawable.profilenew)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MomentViewHolder {
        val binding = MomentLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MomentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MomentViewHolder, position: Int) {
        holder.bind(momentList[position])
    }

    override fun getItemCount(): Int = momentList.size
}