package com.syedsaifhossain.g_chatapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messageList: List<Chats>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatsImage: ImageView = itemView.findViewById(R.id.chatsImg)
        val chatName: TextView = itemView.findViewById(R.id.nameId)
        val chatText: TextView = itemView.findViewById(R.id.messageId)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.chat_item_list, parent, false)
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position]



        holder.chatsImage.setImageResource(message.imageRes)
        holder.chatName.text = message.name
        holder.chatText.text = message.message

    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}
