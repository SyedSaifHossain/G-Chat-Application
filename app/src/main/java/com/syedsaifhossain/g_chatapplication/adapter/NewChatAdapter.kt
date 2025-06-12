package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.NewChatsListBinding
import com.syedsaifhossain.g_chatapplication.models.NewChatItem

class NewChatAdapter(private val chatList: ArrayList<NewChatItem>) : RecyclerView.Adapter<NewChatAdapter.NewChatViewHolder>() {

    // ViewHolder class to bind the views using ViewBinding
    inner class NewChatViewHolder(private val binding: NewChatsListBinding) : RecyclerView.ViewHolder(binding.root) {

        // Bind method to update the views with the data
        fun bind(chatItem: NewChatItem) {
            binding.friendName.text = chatItem.name
            binding.newChatsImg.setImageResource(chatItem.avatarResId)
        }
    }

    // Create ViewHolder by inflating the item layout using ViewBinding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewChatViewHolder {
        val binding = NewChatsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewChatViewHolder(binding)
    }

    // Bind the data to the ViewHolder
    override fun onBindViewHolder(holder: NewChatViewHolder, position: Int) {
        val chatItem = chatList[position]
        holder.bind(chatItem)
    }

    // Return the size of the dataset
    override fun getItemCount(): Int = chatList.size

    // Method to update the data in the ArrayList
    fun updateChatList(newList: ArrayList<NewChatItem>) {
        chatList.clear()
        chatList.addAll(newList)
        notifyDataSetChanged()
    }
}