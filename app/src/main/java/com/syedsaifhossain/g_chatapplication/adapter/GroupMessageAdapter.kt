package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.models.GroupMessage

class GroupMessageAdapter(
    private val messageList: List<GroupMessage>
) : RecyclerView.Adapter<GroupMessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        val isMine = message.senderId == FirebaseAuth.getInstance().currentUser?.uid

        holder.messageText.text = message.message

        val layoutParams = holder.messageText.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = if (isMine) 50 else 10
        layoutParams.marginEnd = if (isMine) 10 else 50
        holder.messageText.layoutParams = layoutParams

        holder.messageText.background = ContextCompat.getDrawable(
            holder.itemView.context,
            if (isMine) R.drawable.bg_message_mine else R.drawable.bg_message_other
        )
        holder.messageText.textAlignment = if (isMine)
            View.TEXT_ALIGNMENT_VIEW_END else View.TEXT_ALIGNMENT_VIEW_START
    }




    override fun getItemCount(): Int = messageList.size
}