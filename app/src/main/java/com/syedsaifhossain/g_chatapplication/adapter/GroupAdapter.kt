package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.SelectgroupListItemBinding
import com.syedsaifhossain.g_chatapplication.models.GroupItem

class GroupAdapter(private val groupList: ArrayList<GroupItem>, private val itemClickListener: OnItemClickListener) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    // Interface to handle click events
    interface OnItemClickListener {
        fun onGroupItemClick(groupItem: GroupItem)
    }

    inner class GroupViewHolder(private val binding: SelectgroupListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        // Bind the data to the views
        fun bind(groupItem: GroupItem) {
            // Set image resource for ImageView
            binding.selectgroupChatsImg.setImageResource(groupItem.selectImg)

            // Set title and description text for TextViews
            binding.selectgroupTitle.text = groupItem.title
            binding.selectgroupDescreption.text = groupItem.description

            // Set click listener on the root view
            binding.root.setOnClickListener {
                // Notify the listener when an item is clicked
                itemClickListener.onGroupItemClick(groupItem)
            }
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

    // Optional: You can add methods to update or modify the list if needed
    fun updateList(newGroupList: ArrayList<GroupItem>) {
        groupList.clear()
        groupList.addAll(newGroupList)
        notifyDataSetChanged()
    }

    fun addGroup(groupItem: GroupItem) {
        groupList.add(groupItem)
        notifyItemInserted(groupList.size - 1)
    }

    fun removeGroup(position: Int) {
        if (position >= 0 && position < groupList.size) {
            groupList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}