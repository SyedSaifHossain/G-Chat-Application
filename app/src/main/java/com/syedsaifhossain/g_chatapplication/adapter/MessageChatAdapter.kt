package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.models.MessageChat

class MessageChatAdapter(val messageList : ArrayList<MessageChat>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    val ITEM_RECEIVE = 1
    val ITEM_SEND = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int, ): RecyclerView.ViewHolder {
        if(viewType == 1){
            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_receive_message, parent, false)
            return ReceiveViewHolder(view)
        }
        else{

            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_sent_message, parent, false)
            return SentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, ) {
        val currentMessage = messageList[position]
        if(holder.javaClass == SentViewHolder::class.java){
            val viewHolder = holder as SentViewHolder
            holder.sendMessage.text = currentMessage.messageChat
        }
        else{
            val viewHolder = holder as ReceiveViewHolder
            holder.receiveMessage.text = currentMessage.messageChat
        }
    }


    override fun getItemViewType(position: Int): Int {

        val currentMessage = messageList[position]
        if(FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)){
            return ITEM_SEND
        }
        else{
            return ITEM_RECEIVE
        }
    }
    override fun getItemCount(): Int {
        return messageList.size
    }


    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var sendMessage = itemView.findViewById<TextView>(R.id.tvSendMessage)
    }

    inner class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var receiveMessage = itemView.findViewById<TextView>(R.id.chatReceiveMessage)
    }
}