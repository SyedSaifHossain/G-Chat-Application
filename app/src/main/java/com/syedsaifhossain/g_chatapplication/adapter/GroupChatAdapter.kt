package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.models.GroupChat

class GroupChatAdapter(
    private var groupList: List<GroupChat>,
    private val onItemClick: (GroupChat) -> Unit
) : RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder>() {

    class GroupChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupAvatar: ImageView = itemView.findViewById(R.id.groupAvatar)
        val groupName: TextView = itemView.findViewById(R.id.groupName)
        val membersLayout: LinearLayout = itemView.findViewById(R.id.membersLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_chat, parent, false)
        return GroupChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupChatViewHolder, position: Int) {
        val group = groupList[position]
        holder.groupName.text = group.name
        // Set group avatar if available (TODO: load from url if needed)
        // holder.groupAvatar.setImageResource or use Glide/Picasso
        // Show member avatars (if you want to display them)
        holder.membersLayout.removeAllViews()
        // Example: just show up to 3 member avatars as placeholder
        val context = holder.itemView.context
        group.members.take(3).forEach { _ ->
            val avatar = ImageView(context)
            avatar.layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                rightMargin = 8
            }
            avatar.setImageResource(R.drawable.default_avatar)
            holder.membersLayout.addView(avatar)
        }
        holder.itemView.setOnClickListener { onItemClick(group) }
    }

    override fun getItemCount(): Int = groupList.size

    fun updateData(newList: List<GroupChat>) {
        groupList = newList
        notifyDataSetChanged()
    }
} 