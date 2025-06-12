package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.databinding.UserLayoutBinding
import com.syedsaifhossain.g_chatapplication.models.User
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class UserAdapter(
    private val userList: ArrayList<User>,
    private val onUserClicked: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: UserLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            // 实时从users节点获取头像和名字
            val usersRef = FirebaseDatabase.getInstance().getReference("users")
            usersRef.child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val avatarUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                        ?: snapshot.child("avatarUrl").getValue(String::class.java)
                        ?: ""
                    binding.userName.text = name
                    Glide.with(binding.userImage.context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .into(binding.userImage)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
            binding.tvLastMessage.text = "" // User模型没有lastMessage字段，可以留空或自定义
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