package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.ChatItemListBinding // Correct ViewBinding import
import com.syedsaifhossain.g_chatapplication.models.Chats

class ChatAdapter(private val messageList: ArrayList<Chats>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // ViewHolder to bind each chat item view using ViewBinding
    inner class ChatViewHolder(private val binding: ChatItemListBinding) : RecyclerView.ViewHolder(binding.root) {

        // You can now access your views via the 'binding' object directly
        fun bind(chat: Chats) {
            binding.chatsImg.setImageResource(chat.imageRes)
            binding.nameId.text = chat.name
            binding.messageId.text = chat.message
        }
    }

    // Create and return the view holder for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    // Bind the data for each item to the view holder
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position]
        holder.bind(message) // Bind data using the bind method
    }

    // Return the total number of items
    override fun getItemCount(): Int {
        return messageList.size
    }
}
