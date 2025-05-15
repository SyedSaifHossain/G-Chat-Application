package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.databinding.UserLayoutBinding
import com.syedsaifhossain.g_chatapplication.models.User

class UserAdapter(
    private val userList: ArrayList<User>,
    private val onUserClicked: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: UserLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.userName.text = user.name ?: user.name ?: "Unknown"
            binding.tvLastMessage.text = user.lastMessage ?: "No message yet"

            // Load profile image with Glide
            Glide.with(binding.userImage.context)
                .load(user.profilePicUrl)
                .placeholder(R.drawable.default_avatar)
                .into(binding.userImage)

            binding.root.setOnClickListener {
                onUserClicked(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size
}