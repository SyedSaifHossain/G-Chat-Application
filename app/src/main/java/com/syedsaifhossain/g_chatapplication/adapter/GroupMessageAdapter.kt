package com.syedsaifhossain.g_chatapplication.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.databinding.ItemMessageBinding
import com.syedsaifhossain.g_chatapplication.databinding.ItemMessageSentBinding
import com.syedsaifhossain.g_chatapplication.databinding.ItemMessageReceivedBinding
import com.syedsaifhossain.g_chatapplication.models.GroupMessage


class GroupMessageAdapter(
    private val messages: List<GroupMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    inner class SentMessageViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            binding.sentMessageText.text = message.text
            // 你可以在这里绑定图片、时间等
        }
    }

    inner class ReceivedMessageViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            binding.receivedMessageText.text = message.text
            // 你可以在这里绑定图片、时间等
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size
}