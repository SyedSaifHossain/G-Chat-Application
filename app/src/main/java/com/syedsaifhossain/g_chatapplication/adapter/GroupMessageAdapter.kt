package com.syedsaifhossain.g_chatapplication.adapter

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.databinding.ItemMessageBinding
import com.syedsaifhossain.g_chatapplication.databinding.ItemMessageSentBinding
import com.syedsaifhossain.g_chatapplication.databinding.ItemMessageReceivedBinding
import com.syedsaifhossain.g_chatapplication.models.GroupMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import com.syedsaifhossain.g_chatapplication.VideoPlayerActivity


class GroupMessageAdapter(
    private val messages: List<GroupMessage>,
    private val currentUserId: String,
    private val onMessageLongClick: ((GroupMessage, View) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val TAG = "GroupChatFlow"
    }

    private var voicePlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentPlayingPosition = -1
    private var playingPosition: Int = -1
    private var playingHolder: RecyclerView.ViewHolder? = null
    private lateinit var context: Context

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    inner class SentMessageViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            Log.d(TAG, "DISPLAY [SENT, position $adapterPosition]: Binding message with type: ${message.type}. ID: ${message.messageId}")
            when (message.type) {
                "text" -> {
                    Log.d(TAG, "SENT: Handling as text.")
                    binding.sentMessageText.text = message.text
                    binding.sentMessageText.visibility = View.VISIBLE
                    binding.sentMessageImage.visibility = View.GONE
                    binding.sentMessageVideo.visibility = View.GONE
                    binding.sentMessageVoice.visibility = View.GONE
                    binding.sentMessageFile.visibility = View.GONE
                }
                "image" -> {
                    Log.d(TAG, "DISPLAY [SENT, position $adapterPosition]: Handling as IMAGE. URL: ${message.imageUrl}")
                    binding.sentMessageImage.visibility = View.VISIBLE
                    binding.sentMessageText.visibility = View.GONE
                    binding.sentMessageVideo.visibility = View.GONE
                    binding.sentMessageVoice.visibility = View.GONE
                    binding.sentMessageFile.visibility = View.GONE
                    
                    Glide.with(binding.root.context)
                        .load(message.imageUrl)
                        .placeholder(R.drawable.default_image)
                        .into(binding.sentMessageImage)
                }
                "video" -> {
                    Log.d(TAG, "DISPLAY [SENT, position $adapterPosition]: Handling as VIDEO. URL: ${message.videoUrl}")
                    binding.sentVideoContainer.visibility = View.VISIBLE
                    binding.sentMessageText.visibility = View.GONE
                    binding.sentMessageImage.visibility = View.GONE
                    binding.sentMessageVoice.visibility = View.GONE
                    binding.sentMessageFile.visibility = View.GONE

                    Glide.with(binding.root.context)
                        .load(message.videoUrl)
                        .placeholder(R.drawable.default_image)
                        .into(binding.sentMessageVideo)

                    binding.sentVideoContainer.setOnClickListener {
                        message.videoUrl?.let { url ->
                            try {
                                Log.d(TAG, "VIDEO CLICK: Attempting to play video URL: $url")
                                
                                // 验证URL格式
                                if (url.isBlank()) {
                                    Log.e(TAG, "VIDEO CLICK: URL is blank")
                                    Toast.makeText(binding.root.context, "视频链接无效", Toast.LENGTH_SHORT).show()
                                    return@let
                                }
                                
                                // 启动内置视频播放器
                                val intent = Intent(binding.root.context, VideoPlayerActivity::class.java)
                                intent.putExtra("video_url", url)
                                binding.root.context.startActivity(intent)
                                Log.d(TAG, "VIDEO CLICK: Successfully launched built-in video player")
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "VIDEO CLICK: Error launching video player", e)
                                Toast.makeText(binding.root.context, "播放视频时发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                "voice" -> {
                    Log.d(TAG, "SENT: Handling as voice.")
                    binding.sentMessageVoice.visibility = View.VISIBLE
                    binding.sentMessageText.visibility = View.GONE
                    binding.sentMessageImage.visibility = View.GONE
                    binding.sentMessageVideo.visibility = View.GONE
                    binding.sentMessageFile.visibility = View.GONE
                    binding.qroupChatSendTextBox.visibility = View.GONE
                    binding.sentMessageTime.visibility = View.VISIBLE
                    
                    binding.sentVoiceDuration.text = "${message.duration}s"
                    if (isPlaying && playingPosition == adapterPosition) {
                        binding.sentVoicePlayButton.setImageResource(android.R.drawable.ic_media_pause)
                    } else {
                        binding.sentVoicePlayButton.setImageResource(R.drawable.ic_play_circle)
                    }
                    binding.sentVoicePlayButton.setOnClickListener {
                        handleVoiceClick(message, adapterPosition, this)
                    }
                }
                "file" -> {
                    Log.d(TAG, "SENT: Handling as file.")
                    binding.sentMessageFile.visibility = View.VISIBLE
                    binding.sentMessageText.visibility = View.GONE
                    binding.sentMessageImage.visibility = View.GONE
                    binding.sentMessageVideo.visibility = View.GONE
                    binding.sentMessageVoice.visibility = View.GONE
                    
                    binding.sentFileName.text = message.fileName ?: "File"
                }
            }
            
            if (!message.senderAvatarUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(message.senderAvatarUrl)
                    .placeholder(R.drawable.profileimage)
                    .into(binding.profileSend)
            } else {
                binding.profileSend.setImageResource(R.drawable.profileimage)
            }
            
            // 显示时间戳
            message.timestamp?.let { timestamp ->
                val date = Date(timestamp)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.sentMessageTime.text = sdf.format(date)
            }
            
            // 显示编辑状态
            if (message.isEdited) {
                binding.sentMessageText.append(" (已编辑)")
            }
            
            // 长按菜单
            binding.root.setOnLongClickListener { view ->
                onMessageLongClick?.invoke(message, view)
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            Log.d(TAG, "DISPLAY [RECEIVED, position $adapterPosition]: Binding message with type: ${message.type}. ID: ${message.messageId}")
            // 显示发送者名称（群聊特有）
            binding.receivedSenderName.text = message.senderName ?: "Unknown"
            binding.receivedSenderName.visibility = View.VISIBLE
            
            // 显示发送者头像
            if (!message.senderAvatarUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(message.senderAvatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .into(binding.receivedSenderAvatar)
            } else {
                binding.receivedSenderAvatar.setImageResource(R.drawable.default_avatar)
            }
            
            when (message.type) {
                "text" -> {
                    Log.d(TAG, "RECEIVED: Handling as text.")
                    binding.receivedMessageText.text = message.text
                    binding.receivedMessageText.visibility = View.VISIBLE
                    binding.receivedMessageImage.visibility = View.GONE
                    binding.receivedMessageVideo.visibility = View.GONE
                    binding.receivedMessageVoice.visibility = View.GONE
                    binding.receivedMessageFile.visibility = View.GONE
                }
                "image" -> {
                    Log.d(TAG, "DISPLAY [RECEIVED, position $adapterPosition]: Handling as IMAGE. URL: ${message.imageUrl}")
                    binding.receivedMessageImage.visibility = View.VISIBLE
                    binding.receivedMessageText.visibility = View.GONE
                    binding.receivedMessageVideo.visibility = View.GONE
                    binding.receivedMessageVoice.visibility = View.GONE
                    binding.receivedMessageFile.visibility = View.GONE
                    
                    Glide.with(binding.root.context)
                        .load(message.imageUrl)
                        .placeholder(R.drawable.default_image)
                        .into(binding.receivedMessageImage)
                }
                "video" -> {
                    Log.d(TAG, "DISPLAY [RECEIVED, position $adapterPosition]: Handling as VIDEO. URL: ${message.videoUrl}")
                    binding.receivedVideoContainer.visibility = View.VISIBLE
                    binding.receivedMessageText.visibility = View.GONE
                    binding.receivedMessageImage.visibility = View.GONE
                    binding.receivedMessageVoice.visibility = View.GONE
                    binding.receivedMessageFile.visibility = View.GONE

                    Glide.with(binding.root.context)
                        .load(message.videoUrl)
                        .placeholder(R.drawable.default_image)
                        .into(binding.receivedMessageVideo)

                    binding.receivedVideoContainer.setOnClickListener {
                        message.videoUrl?.let { url ->
                            try {
                                Log.d(TAG, "VIDEO CLICK: Attempting to play video URL: $url")
                                
                                // 验证URL格式
                                if (url.isBlank()) {
                                    Log.e(TAG, "VIDEO CLICK: URL is blank")
                                    Toast.makeText(binding.root.context, "视频链接无效", Toast.LENGTH_SHORT).show()
                                    return@let
                                }
                                
                                // 启动内置视频播放器
                                val intent = Intent(binding.root.context, VideoPlayerActivity::class.java)
                                intent.putExtra("video_url", url)
                                binding.root.context.startActivity(intent)
                                Log.d(TAG, "VIDEO CLICK: Successfully launched built-in video player")
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "VIDEO CLICK: Error launching video player", e)
                                Toast.makeText(binding.root.context, "播放视频时发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                "voice" -> {
                    Log.d(TAG, "RECEIVED: Handling as voice.")
                    binding.receivedMessageVoice.visibility = View.VISIBLE
                    binding.receivedMessageText.visibility = View.GONE
                    binding.receivedMessageImage.visibility = View.GONE
                    binding.receivedMessageVideo.visibility = View.GONE
                    binding.receivedMessageFile.visibility = View.GONE
                    
                    binding.receivedVoiceDuration.text = "${message.duration}s"
                    if (isPlaying && playingPosition == adapterPosition) {
                        binding.receivedVoicePlayButton.setImageResource(android.R.drawable.ic_media_pause)
                    } else {
                        binding.receivedVoicePlayButton.setImageResource(R.drawable.ic_play_circle)
                    }
                    binding.receivedVoicePlayButton.setOnClickListener {
                        handleVoiceClick(message, adapterPosition, this)
                    }
                }
                "file" -> {
                    Log.d(TAG, "RECEIVED: Handling as file.")
                    binding.receivedMessageFile.visibility = View.VISIBLE
                    binding.receivedMessageText.visibility = View.GONE
                    binding.receivedMessageImage.visibility = View.GONE
                    binding.receivedMessageVideo.visibility = View.GONE
                    binding.receivedMessageVoice.visibility = View.GONE
                    
                    binding.receivedFileName.text = message.fileName ?: "File"
                }
            }
            
            // 显示时间戳
            message.timestamp?.let { timestamp ->
                val date = Date(timestamp)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.receivedMessageTime.text = sdf.format(date)
            }
            
            // 显示编辑状态
            if (message.isEdited) {
                binding.receivedMessageText.append(" (已编辑)")
            }
            
            // 长按菜单
            binding.root.setOnLongClickListener { view ->
                onMessageLongClick?.invoke(message, view)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
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
    
    private fun handleVoiceClick(message: GroupMessage, position: Int, holder: RecyclerView.ViewHolder) {
        if (isPlaying && playingPosition == position) {
            stopVoiceMessage()
            notifyItemChanged(position)
        } else {
            if (isPlaying) {
                val old = playingPosition
                stopVoiceMessage()
                notifyItemChanged(old)
            }
            playVoiceMessage(message, position, holder)
            notifyItemChanged(position)
        }
    }
    
    private fun playVoiceMessage(message: GroupMessage, position: Int, holder: RecyclerView.ViewHolder) {
        stopVoiceMessage()
        message.audioUrl?.let { audioUrl ->
            try {
                voicePlayer = MediaPlayer()
                voicePlayer?.setDataSource(audioUrl)
                voicePlayer?.prepare()
                voicePlayer?.start()
                isPlaying = true
                playingPosition = position
                playingHolder = holder
                voicePlayer?.setOnCompletionListener {
                    stopVoiceMessage()
                    notifyItemChanged(position)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "无法播放语音消息", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun stopVoiceMessage() {
        voicePlayer?.stop()
        voicePlayer?.release()
        voicePlayer = null
        isPlaying = false
        playingPosition = -1
        playingHolder = null
    }
    
    fun onDestroy() {
        stopVoiceMessage()
    }
}