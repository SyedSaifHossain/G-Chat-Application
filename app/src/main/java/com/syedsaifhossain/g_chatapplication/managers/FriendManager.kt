package com.syedsaifhossain.g_chatapplication.managers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.syedsaifhossain.g_chatapplication.models.FriendRequest
import com.syedsaifhossain.g_chatapplication.models.User

class FriendManager {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val friendRequestsRef = database.getReference("friend_requests")
    private val usersRef = database.getReference("users")

    fun sendFriendRequest(receiverId: String, onComplete: (Boolean, String) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(false, "用户未登录")
        
        // 检查是否已经发送过请求
        friendRequestsRef
            .orderByChild("senderId")
            .equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(FriendRequest::class.java)
                        if (request?.receiverId == receiverId && request.status == "pending") {
                            onComplete(false, "已经发送过好友请求")
                            return
                        }
                    }
                    
                    // 获取发送者信息
                    usersRef.child(currentUser.uid).get().addOnSuccessListener { userSnapshot ->
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null) {
                            // 创建新的好友请求
                            val requestId = friendRequestsRef.push().key ?: return@addOnSuccessListener
                            val request = FriendRequest(
                                requestId = requestId,
                                senderId = currentUser.uid,
                                receiverId = receiverId,
                                senderName = user.name,
                                senderProfileImage = user.avatarUrl
                            )
                            
                            // 保存请求
                            friendRequestsRef.child(requestId).setValue(request)
                                .addOnSuccessListener {
                                    onComplete(true, "好友请求已发送")
                                }
                                .addOnFailureListener {
                                    onComplete(false, "发送好友请求失败")
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(false, "数据库错误")
                }
            })
    }

    fun acceptFriendRequest(requestId: String, onComplete: (Boolean, String) -> Unit) {
        friendRequestsRef.child(requestId).get().addOnSuccessListener { snapshot ->
            val request = snapshot.getValue(FriendRequest::class.java)
            if (request != null) {
                // 更新请求状态
                friendRequestsRef.child(requestId).child("status").setValue("accepted")
                    .addOnSuccessListener {
                        // 在双方的好友列表中添加对方
                        val currentUser = auth.currentUser ?: return@addOnSuccessListener
                        
                        // 添加好友关系到双方的用户数据中
                        usersRef.child(currentUser.uid).child("friends").child(request.senderId).setValue(true)
                        usersRef.child(request.senderId).child("friends").child(currentUser.uid).setValue(true)
                            .addOnSuccessListener {
                                onComplete(true, "已接受好友请求")
                            }
                            .addOnFailureListener {
                                onComplete(false, "接受好友请求失败")
                            }
                    }
                    .addOnFailureListener {
                        onComplete(false, "更新请求状态失败")
                    }
            } else {
                onComplete(false, "请求不存在")
            }
        }
    }

    fun rejectFriendRequest(requestId: String, onComplete: (Boolean, String) -> Unit) {
        friendRequestsRef.child(requestId).child("status").setValue("rejected")
            .addOnSuccessListener {
                onComplete(true, "已拒绝好友请求")
            }
            .addOnFailureListener {
                onComplete(false, "拒绝好友请求失败")
            }
    }

    fun getPendingFriendRequests(onComplete: (List<FriendRequest>) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(emptyList())
        
        friendRequestsRef
            .orderByChild("receiverId")
            .equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = mutableListOf<FriendRequest>()
                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(FriendRequest::class.java)
                        if (request?.status == "pending") {
                            requests.add(request)
                        }
                    }
                    onComplete(requests)
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(emptyList())
                }
            })
    }
} 