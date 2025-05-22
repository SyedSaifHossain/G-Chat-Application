package com.syedsaifhossain.g_chatapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.R
import com.syedsaifhossain.g_chatapplication.managers.FriendManager
import com.syedsaifhossain.g_chatapplication.models.FriendRequest
import de.hdodenhof.circleimageview.CircleImageView

class FriendRequestAdapter(
    private var requests: List<FriendRequest>,
    private val friendManager: FriendManager
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: CircleImageView = view.findViewById(R.id.profileImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val rejectButton: Button = view.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        
        holder.nameTextView.text = request.senderName
        
        // 加载头像
        if (request.senderProfileImage.isNotEmpty()) {
            Glide.with(holder.profileImageView.context)
                .load(request.senderProfileImage)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(holder.profileImageView)
        }

        // 设置按钮点击事件
        holder.acceptButton.setOnClickListener {
            friendManager.acceptFriendRequest(request.requestId) { success, message ->
                Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
                if (success) {
                    // 从列表中移除已接受的请求
                    val updatedRequests = requests.toMutableList()
                    updatedRequests.removeAt(position)
                    requests = updatedRequests
                    notifyItemRemoved(position)
                }
            }
        }

        holder.rejectButton.setOnClickListener {
            friendManager.rejectFriendRequest(request.requestId) { success, message ->
                Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
                if (success) {
                    // 从列表中移除已拒绝的请求
                    val updatedRequests = requests.toMutableList()
                    updatedRequests.removeAt(position)
                    requests = updatedRequests
                    notifyItemRemoved(position)
                }
            }
        }
    }

    override fun getItemCount() = requests.size

    fun updateRequests(newRequests: List<FriendRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
} 