package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.ChatItemListBinding // Correct ViewBinding import
import com.syedsaifhossain.g_chatapplication.models.Chats
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

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

    inner class ChatViewHolder(val binding: ChatItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chats) {
            if (chat.isGroup) {
                binding.chatsImg.setImageResource(R.drawable.groupiconnew)
                binding.nameId.text = chat.name
            } else {
                // 实时从users节点获取头像和名字
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").document(chat.otherUserId).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val name = document.getString("name") ?: ""
                            val avatarUrl = document.getString("profileImageUrl")
                                ?: document.getString("avatarUrl")
                                ?: ""
                            binding.nameId.text = name
                            if (avatarUrl.isNotEmpty()) {
                                Glide.with(binding.chatsImg.context)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.profilenew)
                                    .error(R.drawable.profilenew)
                                    .centerCrop()
                                    .into(binding.chatsImg)
                            } else {
                                binding.chatsImg.setImageResource(R.drawable.profilenew)
                            }
                        } else {
                            // 如果用户不存在，使用默认数据
                            binding.nameId.text = chat.name
                            binding.chatsImg.setImageResource(R.drawable.profilenew)
                        }
                    }
                    .addOnFailureListener { e ->
                        // 如果查询失败，使用默认数据
                        binding.nameId.text = chat.name
                        binding.chatsImg.setImageResource(R.drawable.profilenew)
                    }
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
        val chat = messageList[position]
        holder.bind(chat)

        val chatItem = holder.binding.chatItemLayout
        val deleteBtn = holder.binding.deleteButton

        // Reset swipe state
        chatItem.translationX = 0f

        var downX = 0f
        var isSwiped = false

        holder.itemView.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0f
            var startY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        return false // Let other events process
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val diffX = event.x - startX
                        val diffY = event.y - startY

                        if (Math.abs(diffX) > Math.abs(diffY)) {
                            if (diffX < -100 && !isSwiped) {
                                chatItem.animate().translationX(-200f).setDuration(200).start()
                                isSwiped = true
                                return true
                            } else if (diffX > 100 && isSwiped) {
                                chatItem.animate().translationX(0f).setDuration(200).start()
                                isSwiped = false
                                return true
                            }
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        // Optional: add snap logic
                    }
                }
                return false
            }
        })

        deleteBtn.setOnClickListener {
            onItemClick(chat)
            chatItem.animate().translationX(0f).setDuration(200).start()
            isSwiped = false
        }

    }



    override fun getItemCount(): Int = messageList.size
}