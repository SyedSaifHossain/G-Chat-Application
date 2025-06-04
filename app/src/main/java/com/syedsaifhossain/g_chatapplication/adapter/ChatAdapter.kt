package com.syedsaifhossain.g_chatapplication.adapter

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.ChatItemListBinding
import com.syedsaifhossain.g_chatapplication.models.Chats

class ChatAdapter(
    private val messageList: ArrayList<Chats>,
    private val onItemClick: (Chats) -> Unit // Callback for item click
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_VOICE = 1
    }

    // ViewHolder for Text Messages
    inner class TextMessageViewHolder(private val binding: ChatItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chats) {
            binding.nameId.text = chat.name
            binding.messageId.text = chat.content // Display text message content
            binding.chatsImg.setImageResource(chat.imageRes) // Display profile image

            // Set onClick listener for the item
            binding.root.setOnClickListener { onItemClick(chat) }
        }
    }

    // ViewHolder for Voice Messages
    inner class VoiceMessageViewHolder(private val binding: ChatItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var mediaPlayer: MediaPlayer? = null

        fun bind(chat: Chats) {
            binding.nameId.text = chat.name
            binding.messageId.text = "Voice Message" // Placeholder for voice messages
            binding.chatsImg.setImageResource(chat.imageRes) // Display profile image

            // Handle play button for the voice message
            binding.root.setOnClickListener {
                chat.content?.let { url ->
                    mediaPlayer?.release() // Release any previous media player instance
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(url) // Set the audio URL of the voice message
                        prepare()
                        start()
                    }
                    mediaPlayer?.setOnCompletionListener { it.release() } // Release once done
                }
            }
        }
    }

    // Determine the type of the message (text or voice)
    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].type == "voice") TYPE_VOICE else TYPE_TEXT
    }

    // Create the appropriate ViewHolder based on the message type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ChatItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == TYPE_TEXT) {
            TextMessageViewHolder(binding)
        } else {
            VoiceMessageViewHolder(binding)
        }
    }

    // Bind the data to the appropriate ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = messageList[position]
        if (holder is TextMessageViewHolder) {
            holder.bind(chat)
        } else if (holder is VoiceMessageViewHolder) {
            holder.bind(chat)
        }
    }

    override fun getItemCount(): Int = messageList.size

}