package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
    private val otherUserAvatarUrl: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // View types remain the same
    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        // Logic remains the same
        return if (chatList[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // --- MODIFIED: Inflate the NEW layout files ---
        return if (viewType == VIEW_TYPE_SENT) {
            val view = inflater.inflate(R.layout.item_chat_sent, parent, false) // Use item_chat_sent
            SentMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_received, parent, false) // Use item_chat_received
            ReceivedMessageViewHolder(view)
        }
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = chatList[position]
        // --- MODIFIED: Pass avatar URLs to bind methods ---
        if (holder is SentMessageViewHolder) {
            holder.bind(message, myAvatarUrl) // Pass my avatar URL
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message, otherUserAvatarUrl) // Pass other user's avatar URL
        }
    }

    // --- MODIFIED: Sent Message ViewHolder ---
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- Find views using IDs from item_chat_sent.xml ---
        private val messageText: TextView = itemView.findViewById(R.id.tv_message)
        private val timeText: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val voiceLayout: View? = itemView.findViewById(R.id.voice_layout)
        private val voiceIcon: ImageView? = itemView.findViewById(R.id.voice_icon)
        private val voiceDuration: TextView? = itemView.findViewById(R.id.voice_duration)

        // --- Modified bind method to accept avatar URL ---
        fun bind(message: ChatModel, avatarUrl: String?) {
            timeText.text = formatTime(message.timestamp)
            if (message.type == "voice") {
                messageText.visibility = View.GONE
                voiceLayout?.visibility = View.VISIBLE
                voiceDuration?.text = "${message.duration}\""
                val playListener = View.OnClickListener {
                    try {
                        val mediaPlayer = android.media.MediaPlayer()
                        mediaPlayer.setDataSource(message.message)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                voiceLayout?.setOnClickListener(playListener)
                voiceIcon?.setOnClickListener(playListener)
                voiceDuration?.setOnClickListener(playListener)
            } else {
                messageText.visibility = View.VISIBLE
                voiceLayout?.visibility = View.GONE
                messageText.text = message.message
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
        private val avatarImage: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val voiceLayout: View? = itemView.findViewById(R.id.voice_layout)
        private val voiceIcon: ImageView? = itemView.findViewById(R.id.voice_icon)
        private val voiceDuration: TextView? = itemView.findViewById(R.id.voice_duration)
        // TODO: Add handling for image messages if needed later

        // --- Modified bind method to accept avatar URL ---
        fun bind(message: ChatModel, avatarUrl: String?) {
            timeText.text = formatTime(message.timestamp)
            if (message.type == "voice") {
                messageText.visibility = View.GONE
                voiceLayout?.visibility = View.VISIBLE
                voiceDuration?.text = "${message.duration}\""
                val playListener = View.OnClickListener {
                    try {
                        val mediaPlayer = android.media.MediaPlayer()
                        mediaPlayer.setDataSource(message.message)
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                voiceLayout?.setOnClickListener(playListener)
                voiceIcon?.setOnClickListener(playListener)
                voiceDuration?.setOnClickListener(playListener)
            } else {
                messageText.visibility = View.VISIBLE
                voiceLayout?.visibility = View.GONE
                messageText.text = message.message
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