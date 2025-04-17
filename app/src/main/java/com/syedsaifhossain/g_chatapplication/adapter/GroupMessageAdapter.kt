package com.syedsaifhossain.g_chatapplication.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.databinding.ItemMessageBinding
import com.syedsaifhossain.g_chatapplication.models.GroupMessage


class GroupMessageAdapter(
    private val messages: List<GroupMessage>,
    private val currentUserId: String

) : RecyclerView.Adapter<GroupMessageAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: GroupMessage) {
            binding.messageText.text = message.text

            val layoutParams = binding.messageText.layoutParams as LinearLayout.LayoutParams
            layoutParams.gravity =
                if (message.senderId == currentUserId) Gravity.END else Gravity.START
            binding.messageText.layoutParams = layoutParams

            // Optional: Different background per sender
            if (message.senderId == currentUserId) {
                binding.messageText.setBackgroundResource(R.drawable.bubble_bg_me)
            } else {
                binding.messageText.setBackgroundResource(R.drawable.bubble_bg_other)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}