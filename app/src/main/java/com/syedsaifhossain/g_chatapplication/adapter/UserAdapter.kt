package com.syedsaifhossain.g_chatapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.models.User

class UserAdapter(
    private val userList: ArrayList<User>,
    private val onUserClicked: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.userName)
        val textLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val userImage: ImageView = itemView.findViewById(R.id.profileImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.textName.text = user.name ?: "No Name"
        holder.textLastMessage.text = user.lastMessage?: "No Message"

        // Load profile image with Glide or placeholder
        if (!user.profilePicUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.profilePicUrl)
                .placeholder(R.drawable.default_avatar)
                .into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.default_avatar)
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            onUserClicked(user)
        }

        // Load last message from Firebase
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val senderRoom = "$currentUid-${user.uid}"

        val databaseRef = FirebaseDatabase.getInstance().reference
        databaseRef.child("Chats").child(senderRoom).child("lastMessage")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastMsg = snapshot.getValue(String::class.java)
                    holder.textLastMessage.text = lastMsg ?: "Tap to chat"
                }

                override fun onCancelled(error: DatabaseError) {
                    holder.textLastMessage.text = "Tap to chat"
                }
            })
    }

    override fun getItemCount(): Int = userList.size
}