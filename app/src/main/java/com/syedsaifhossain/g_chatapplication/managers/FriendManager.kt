package com.syedsaifhossain.g_chatapplication.managers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.syedsaifhossain.g_chatapplication.models.FriendRequest
import com.syedsaifhossain.g_chatapplication.models.User

class FriendManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val friendRequestsCollection = firestore.collection("friend_requests")
    private val usersCollection = firestore.collection("users")

    fun sendFriendRequest(receiverId: String, onComplete: (Boolean, String) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(false, "User not logged in")
        
        // 检查是否已经发送过请求
        friendRequestsCollection
            .whereEqualTo("senderId", currentUser.uid)
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onComplete(false, "already sent friend request")
                    return@addOnSuccessListener
                }
                
                // 获取发送者信息
                usersCollection.document(currentUser.uid).get().addOnSuccessListener { userDoc ->
                    val user = userDoc.toObject(User::class.java)
                    if (user != null) {
                        // 创建新的好友请求
                        val requestId = friendRequestsCollection.document().id
                        val request = FriendRequest(
                            requestId = requestId,
                            senderId = currentUser.uid,
                            receiverId = receiverId,
                            senderName = user.name,
                            senderProfileImage = user.avatarUrl
                        )
                        
                        // 保存请求
                        friendRequestsCollection.document(requestId).set(request)
                            .addOnSuccessListener {
                                onComplete(true, "friend request sent")
                            }
                            .addOnFailureListener {
                                onComplete(false, "failed to send friend request")
                            }
                    }
                }
            }
            .addOnFailureListener {
                onComplete(false, "database error")
            }
    }

    fun acceptFriendRequest(requestId: String, onComplete: (Boolean, String) -> Unit) {
        friendRequestsCollection.document(requestId).get().addOnSuccessListener { document ->
            val request = document.toObject(FriendRequest::class.java)
            if (request != null) {
                // 更新请求状态
                friendRequestsCollection.document(requestId).update("status", "accepted")
                    .addOnSuccessListener {
                        // 在双方的好友列表中添加对方
                        val currentUser = auth.currentUser ?: return@addOnSuccessListener
                        
                        // 添加好友关系到双方的用户数据中
                        usersCollection.document(currentUser.uid).update("friends.${request.senderId}", true)
                        usersCollection.document(request.senderId).update("friends.${currentUser.uid}", true)
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
        friendRequestsCollection.document(requestId).update("status", "rejected")
            .addOnSuccessListener {
                onComplete(true, "rejected friend request")
            }
            .addOnFailureListener {
                onComplete(false, "failed to reject friend request")
            }
    }

    fun getPendingFriendRequests(onComplete: (List<FriendRequest>) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(emptyList())
        
        friendRequestsCollection
            .whereEqualTo("receiverId", currentUser.uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                val requests = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)
                }
                onComplete(requests)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
} 