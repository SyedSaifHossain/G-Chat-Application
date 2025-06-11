package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.ChatItemListBinding // Correct ViewBinding import
import com.syedsaifhossain.g_chatapplication.models.Chats
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R

class ChatAdapter(
    private val messageList: ArrayList<Chats>,
    private val onItemClick: (Chats) -> Unit // Now passes chat info if needed
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(private val binding: ChatItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chats) {
            if (chat.isGroup) {
                binding.chatsImg.setImageResource(R.drawable.addcontacticon)
            } else {
                if (!chat.otherUserAvatarUrl.isNullOrBlank()) {
                    Glide.with(binding.chatsImg.context)
                        .load(chat.otherUserAvatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .into(binding.chatsImg)
                } else {
                    binding.chatsImg.setImageResource(R.drawable.default_avatar)
                }
            }
            binding.nameId.text = chat.name

            // 语音消息显示逻辑
            if (chat.type == "voice") {
                binding.messageId.visibility = android.view.View.GONE
                binding.voiceLayout.visibility = android.view.View.VISIBLE
                binding.voiceDuration.text = "${chat.duration}\""
                binding.voiceLayout.setOnClickListener {
                    // 播放语音
                    try {
                        val mediaPlayer = android.media.MediaPlayer()
                        mediaPlayer.setDataSource(chat.message)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                binding.messageId.visibility = android.view.View.VISIBLE
                binding.voiceLayout.visibility = android.view.View.GONE
                binding.messageId.text = chat.message
            }

            binding.root.setOnClickListener {
                onItemClick(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size
}