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
        val currentUser = auth.currentUser ?: return onComplete(false, "User not logged in")
        
        // 检查是否已经发送过请求
        friendRequestsRef
            .orderByChild("senderId")
            .equalTo(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(FriendRequest::class.java)
                        if (request?.receiverId == receiverId && request.status == "pending") {
                            onComplete(false, "already sent friend request")
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
                                    onComplete(true, "friend request sent")
                                }
                                .addOnFailureListener {
                                    onComplete(false, "failed to send friend request")
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(false, "database error")
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
                                onComplete(true, "accepted friend request")
                            }
                            .addOnFailureListener {
                                onComplete(false, "failed to accept friend request")
                            }
                    }
                    .addOnFailureListener {
                        onComplete(false, "failed to update request status")
                    }
            } else {
                onComplete(false, "request does not exist")
            }
        }
    }

    fun rejectFriendRequest(requestId: String, onComplete: (Boolean, String) -> Unit) {
        friendRequestsRef.child(requestId).child("status").setValue("rejected")
            .addOnSuccessListener {
                onComplete(true, "rejected friend request")
            }
            .addOnFailureListener {
                onComplete(false, "failed to reject friend request")
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