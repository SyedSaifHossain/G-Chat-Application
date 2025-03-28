package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.SelectgroupListItemBinding
import com.syedsaifhossain.g_chatapplication.models.GroupItem

class GroupAdapter(private val groupList: List<GroupItem>) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(private val binding: SelectgroupListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        // Bind the data to the views
        fun bind(groupItem: GroupItem) {
            // Set image resource for ImageView
            binding.selectgroupChatsImg.setImageResource(groupItem.selectImg)

            // Set title and description text for TextViews
            binding.selectgroupTitle.text = groupItem.title
            binding.selectgroupDescreption.text = groupItem.description
        }
    }

    // Inflate the layout and return the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = SelectgroupListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    // Bind the data to the ViewHolder
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val groupItem = groupList[position]
        holder.bind(groupItem)
    }

    // Return the size of the dataset
    override fun getItemCount(): Int {
        return groupList.size
    }
}