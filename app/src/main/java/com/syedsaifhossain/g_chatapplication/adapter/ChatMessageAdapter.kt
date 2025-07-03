package com.syedsaifhossain.g_chatapplication.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.models.ChatModel
// Remove EmojiTextView import if not needed, our new layout uses standard TextView
// import com.vanniktech.emoji.EmojiTextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessageAdapter(
    private val chatList: List<ChatModel>,
    private val currentUserId: String,
    // --- ADDED: Avatar URLs ---
    private val myAvatarUrl: String?,
    private val otherUserAvatarUrl: String?,
    private val onMessageLongClick: (ChatModel, View) -> Unit // Added: long click callback
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_RECALL = 3
    }

    // Add a single MediaPlayer instance for the adapter
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var lastPlayingUrl: String? = null

    override fun getItemViewType(position: Int): Int {
        val message = chatList[position]
        return if (message.deleted) {
            VIEW_TYPE_RECALL
        } else if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_RECALL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recall_message, parent, false)
                RecallMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = chatList[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message, myAvatarUrl)
            is ReceivedMessageViewHolder -> holder.bind(message, otherUserAvatarUrl)
            is RecallMessageViewHolder -> holder.bind(message)
        }
    }

    inner class RecallMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recallMessageText: TextView = itemView.findViewById(R.id.tv_recall_message)
        fun bind(message: ChatModel) {
            recallMessageText.text = "This message was recalled"
        }
    }

    // --- MODIFIED: Sent Message ViewHolder ---
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- Find views using IDs from item_chat_sent.xml ---
        private val messageText: TextView = itemView.findViewById(R.id.tv_message)
        private val timeText: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val recallMessageText: TextView = itemView.findViewById(R.id.tv_recall_message)
        private val layoutContent: View = itemView.findViewById(R.id.layout_content)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val voiceLayout: View? = itemView.findViewById(R.id.voice_layout)
        private val voiceIcon: ImageView? = itemView.findViewById(R.id.voice_icon)
        private val voiceDuration: TextView? = itemView.findViewById(R.id.voice_duration)
        private val imageView: ImageView? = itemView.findViewById(R.id.iv_image)
        private val bubbleLayout: View = itemView.findViewById(R.id.layout_message_bubble)
        private val videoContainer: View? = itemView.findViewById(R.id.video_container)
        private val videoThumb: ImageView? = itemView.findViewById(R.id.iv_video_thumb)
        private val playBtn: ImageView? = itemView.findViewById(R.id.iv_play)

        // --- Modified bind method to accept avatar URL ---
        fun bind(message: ChatModel, avatarUrl: String?) {
            // Always reset all visibilities to avoid RecyclerView reuse bugs
            recallMessageText.visibility = View.GONE
            layoutContent.visibility = View.GONE
            messageText.visibility = View.GONE
            imageView?.visibility = View.GONE
            voiceLayout?.visibility = View.GONE
            videoContainer?.visibility = View.GONE
            bubbleLayout.visibility = View.GONE

            if (message.deleted) {
                recallMessageText.visibility = View.VISIBLE
                recallMessageText.text = "This message was recalled"
            } else {
                layoutContent.visibility = View.VISIBLE
                bubbleLayout.visibility = View.VISIBLE
                // Set long click listener
                itemView.setOnLongClickListener { view ->
                    if (message.senderId == currentUserId && !message.deleted) {
                        onMessageLongClick(message, view)
                        true
                    } else {
                        false
                    }
                }
                // Show correct content type
                when (message.type) {
                    "text" -> {
                        messageText.visibility = View.VISIBLE
                        messageText.text = if (message.isEdited) "${message.message} (edited)" else message.message
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    "image" -> {
                        imageView?.visibility = View.VISIBLE
                        bubbleLayout.visibility = View.GONE
                        imageView?.setImageResource(android.R.color.transparent)
                        if (!message.imageUrl.isNullOrEmpty()) {
                            Glide.with(itemView.context)
                                .load(message.imageUrl)
                                .placeholder(R.drawable.bg_gradient)
                                .error(R.drawable.bg_gradient)
                                .into(imageView!!)
                        }
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    "voice" -> {
                        voiceLayout?.visibility = View.VISIBLE
                        messageText.visibility = View.GONE
                        imageView?.visibility = View.GONE
                        videoContainer?.visibility = View.GONE
                        bubbleLayout.setBackgroundResource(R.drawable.bg_chat_sent)
                        voiceDuration?.text = "${message.duration}\""
                        val playListener = View.OnClickListener {
                            try {
                                // If already playing and same url, stop and do not replay
                                if (mediaPlayer != null && mediaPlayer!!.isPlaying && lastPlayingUrl == message.message) {
                                    mediaPlayer!!.stop()
                                    mediaPlayer!!.release()
                                    mediaPlayer = null
                                    lastPlayingUrl = null
                                    return@OnClickListener
                                }
                                // If already playing other url, stop and release
                                if (mediaPlayer != null) {
                                    mediaPlayer!!.stop()
                                    mediaPlayer!!.release()
                                    mediaPlayer = null
                                }
                                // Start new playback
                                mediaPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(message.message)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        release()
                                        mediaPlayer = null
                                        lastPlayingUrl = null
                                    }
                                }
                                lastPlayingUrl = message.message
                            } catch (e: Exception) {
                                e.printStackTrace()
                                mediaPlayer = null
                                lastPlayingUrl = null
                            }
                        }
                        voiceLayout?.setOnClickListener(playListener)
                        voiceIcon?.setOnClickListener(playListener)
                        voiceDuration?.setOnClickListener(playListener)
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    "video" -> {
                        videoContainer?.visibility = View.VISIBLE
                        messageText.visibility = View.GONE
                        imageView?.visibility = View.GONE
                        voiceLayout?.visibility = View.GONE
                        bubbleLayout.setBackgroundResource(0)
                        // 加载视频缩略图
                        Glide.with(itemView.context)
                            .load(message.imageUrl)
                            .frame(1000000) // 取第1秒帧
                            .placeholder(R.drawable.bg_gradient)
                            .error(R.drawable.bg_gradient)
                            .into(videoThumb!!)
                        playBtn?.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(android.net.Uri.parse(message.imageUrl), "video/*")
                            itemView.context.startActivity(intent)
                        }
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    else -> {
                        messageText.visibility = View.VISIBLE
                        messageText.text = message.message
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                }
            }
            if (avatarUrl != null) {
                Glide.with(itemView.context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(avatarImage)
            } else {
                avatarImage.setImageResource(R.drawable.profile)
            }
        }
    }

    // --- MODIFIED: Received Message ViewHolder ---
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- Find views using IDs from item_chat_received.xml ---
        private val messageText: TextView = itemView.findViewById(R.id.tv_message)
        private val timeText: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val recallMessageText: TextView = itemView.findViewById(R.id.tv_recall_message)
        private val layoutContent: View = itemView.findViewById(R.id.layout_content)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val voiceLayout: View? = itemView.findViewById(R.id.voice_layout)
        private val voiceIcon: ImageView? = itemView.findViewById(R.id.voice_icon)
        private val voiceDuration: TextView? = itemView.findViewById(R.id.voice_duration)
        private val imageView: ImageView? = itemView.findViewById(R.id.iv_image)
        private val bubbleLayout: View = itemView.findViewById(R.id.layout_message_bubble)
        private val videoContainer: View? = itemView.findViewById(R.id.video_container)
        private val videoThumb: ImageView? = itemView.findViewById(R.id.iv_video_thumb)
        private val playBtn: ImageView? = itemView.findViewById(R.id.iv_play)

        // --- Modified bind method to accept avatar URL ---
        fun bind(message: ChatModel, avatarUrl: String?) {
            // Always reset all visibilities to avoid RecyclerView reuse bugs
            recallMessageText.visibility = View.GONE
            layoutContent.visibility = View.GONE
            messageText.visibility = View.GONE
            imageView?.visibility = View.GONE
            voiceLayout?.visibility = View.GONE
            videoContainer?.visibility = View.GONE
            bubbleLayout.visibility = View.GONE
            avatarImage.visibility = View.VISIBLE

            if (message.deleted) {
                recallMessageText.visibility = View.VISIBLE
                recallMessageText.text = "This message was recalled"
                avatarImage.visibility = View.GONE
            } else {
                layoutContent.visibility = View.VISIBLE
                bubbleLayout.visibility = View.VISIBLE
                avatarImage.visibility = View.VISIBLE
                // Set long click listener
                itemView.setOnLongClickListener { view ->
                    if (message.senderId == currentUserId && !message.deleted) {
                        onMessageLongClick(message, view)
                        true
                    } else {
                        false
                    }
                }
                // Show correct content type
                when (message.type) {
                    "text" -> {
                        messageText.visibility = View.VISIBLE
                        messageText.text = if (message.isEdited) "${message.message} (edited)" else message.message
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    "image" -> {
                        imageView?.visibility = View.VISIBLE
                        imageView?.setImageResource(android.R.color.transparent)
                        if (!message.imageUrl.isNullOrEmpty()) {
                            Glide.with(itemView.context)
                                .load(message.imageUrl)
                                .placeholder(R.drawable.bg_gradient)
                                .error(R.drawable.bg_gradient)
                                .into(imageView!!)
                        }
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    "voice" -> {
                        voiceLayout?.visibility = View.VISIBLE
                        messageText.visibility = View.GONE
                        imageView?.visibility = View.GONE
                        videoContainer?.visibility = View.GONE
                        bubbleLayout.setBackgroundResource(R.drawable.bg_chat_received)
                        voiceDuration?.text = "${message.duration}\""
                        val playListener = View.OnClickListener {
                            try {
                                // If already playing and same url, stop and do not replay
                                if (mediaPlayer != null && mediaPlayer!!.isPlaying && lastPlayingUrl == message.message) {
                                    mediaPlayer!!.stop()
                                    mediaPlayer!!.release()
                                    mediaPlayer = null
                                    lastPlayingUrl = null
                                    return@OnClickListener
                                }
                                // If already playing other url, stop and release
                                if (mediaPlayer != null) {
                                    mediaPlayer!!.stop()
                                    mediaPlayer!!.release()
                                    mediaPlayer = null
                                }
                                // Start new playback
                                mediaPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(message.message)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        release()
                                        mediaPlayer = null
                                        lastPlayingUrl = null
                                    }
                                }
                                lastPlayingUrl = message.message
                            } catch (e: Exception) {
                                e.printStackTrace()
                                mediaPlayer = null
                                lastPlayingUrl = null
                            }
                        }
                        voiceLayout?.setOnClickListener(playListener)
                        voiceIcon?.setOnClickListener(playListener)
                        voiceDuration?.setOnClickListener(playListener)
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    "video" -> {
                        videoContainer?.visibility = View.VISIBLE
                        messageText.visibility = View.GONE
                        imageView?.visibility = View.GONE
                        voiceLayout?.visibility = View.GONE
                        bubbleLayout.setBackgroundResource(0)
                        Glide.with(itemView.context)
                            .load(message.imageUrl)
                            .frame(1000000)
                            .placeholder(R.drawable.bg_gradient)
                            .error(R.drawable.bg_gradient)
                            .into(videoThumb!!)
                        playBtn?.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(android.net.Uri.parse(message.imageUrl), "video/*")
                            itemView.context.startActivity(intent)
                        }
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                    else -> {
                        messageText.visibility = View.VISIBLE
                        messageText.text = message.message
                        timeText.visibility = View.VISIBLE
                        timeText.text = formatTime(message.timestamp)
                    }
                }
            }
            if (avatarUrl != null) {
                Glide.with(itemView.context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(avatarImage)
            } else {
                avatarImage.setImageResource(R.drawable.profile)
            }
        }
    }

    // --- formatTime method (ensure timestamp is Long) ---
    private fun formatTime(timestamp: Long?): String {
        if (timestamp == null) return "" // Handle null timestamp
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

}