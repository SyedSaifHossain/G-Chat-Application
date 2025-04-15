package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.SelectgroupListItemBinding
import com.syedsaifhossain.g_chatapplication.models.GroupItem

class GroupAdapter(
    private val groupList: ArrayList<GroupItem>,
    private val onItemClick: (GroupItem) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(private val binding: SelectgroupListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(groupItem: GroupItem) {
            binding.selectgroupChatsImg.setImageResource(groupItem.selectImg)
            binding.selectgroupTitle.text = groupItem.title
            binding.selectgroupDescreption.text = groupItem.description

            binding.root.setOnClickListener {
                onItemClick(groupItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = SelectgroupListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groupList[position])
    }

    override fun getItemCount(): Int = groupList.size
}