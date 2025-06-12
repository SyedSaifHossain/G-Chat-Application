package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.ChatItemListBinding // Correct ViewBinding import
import com.syedsaifhossain.g_chatapplication.models.Chats
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import android.util.TypedValue

class ChatAdapter(
    private val messageList: ArrayList<Chats>,
    private val onItemClick: (Chats) -> Unit // Now passes chat info if needed
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private fun dpToPx(dp: Int, context: android.content.Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    inner class ChatViewHolder(private val binding: ChatItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chats) {
            if (chat.isGroup) {
                binding.chatsImg.setImageResource(R.drawable.addcontacticon)
                binding.nameId.text = chat.name
            } else {
                // 实时从users节点获取头像和名字
                val usersRef = FirebaseDatabase.getInstance().getReference("users")
                usersRef.child(chat.otherUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val avatarUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                            ?: snapshot.child("avatarUrl").getValue(String::class.java)
                            ?: ""
                        binding.nameId.text = name
                        if (avatarUrl.isNotEmpty()) {
                            Glide.with(binding.chatsImg.context)
                                .load(avatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .centerCrop()
                                .into(binding.chatsImg)
                        } else {
                            binding.chatsImg.setImageResource(R.drawable.default_avatar)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

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