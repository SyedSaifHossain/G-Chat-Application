package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.databinding.NewChatsListBinding
import com.syedsaifhossain.g_chatapplication.models.NewChatItem

class NewChatAdapter(private val chatList: ArrayList<NewChatItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    inner class HeaderViewHolder(val binding: com.syedsaifhossain.g_chatapplication.databinding.ItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewChatItem) {
            binding.headerText.text = item.name
        }
    }

    inner class ItemViewHolder(val binding: NewChatsListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewChatItem) {
            binding.friendName.text = item.name
            binding.newChatsImg.setImageResource(item.avatarResId)
            binding.root.isSelected = item.isSelected
            binding.root.setBackgroundColor(if (item.isSelected) 0x3381d8d0 else 0x00000000)
            binding.root.setOnClickListener {
                if (!item.uid.startsWith("header_")) {
                    item.isSelected = !item.isSelected
                    notifyItemChanged(adapterPosition)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].uid.startsWith("header_")) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = com.syedsaifhossain.g_chatapplication.databinding.ItemHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = NewChatsListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = chatList[position]
        if (holder is HeaderViewHolder) {
            holder.bind(chatItem)
        } else if (holder is ItemViewHolder) {
            holder.bind(chatItem)
        }
    }

    override fun getItemCount(): Int = chatList.size

    fun updateChatList(newList: ArrayList<NewChatItem>) {
        chatList.clear()
        chatList.addAll(newList)
        notifyDataSetChanged()
    }
}