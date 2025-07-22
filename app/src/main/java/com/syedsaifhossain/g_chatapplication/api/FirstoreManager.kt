package com.syedsaifhossain.g_chatapplication.api

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.models.Message
import com.syedsaifhossain.g_chatapplication.models.User
import com.syedsaifhossain.g_chatapplication.models.Chats
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object FirestoreManager {
    private const val TAG = "FirestoreManager"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    object UserManager {
        private val usersCollection = firestore.collection("users")

        // Create a new user
        suspend fun createUser(user: User) {
            try {
                usersCollection.document(user.uid).set(user).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create user", e)
                throw e
            }
        }

        // Get user info
        suspend fun getUser(userId: String): User? {
            return try {
                val document = usersCollection.document(userId).get().await()
                document.toObject(User::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get user info", e)
                null
            }
        }

        // Update user info
        suspend fun updateUser(userId: String, updates: Map<String, Any>) {
            try {
                usersCollection.document(userId).update(updates).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update user info", e)
                throw e
            }
        }

        // Update user online status
        suspend fun updateUserStatus(userId: String, isOnline: Boolean) {
            val updates = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            updateUser(userId, updates)
        }

        // Get all users
        suspend fun getAllUsers(): List<User> {
            return try {
                val snapshot = usersCollection.get().await()
                snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get all users", e)
                emptyList()
            }
        }

        // Get users by friend IDs
        suspend fun getUsersByFriendIds(friendIds: List<String>): List<User> {
            return try {
                if (friendIds.isEmpty()) return emptyList()

                val users = mutableListOf<User>()
                // Firestore doesn't support 'in' queries with more than 10 items
                // So we need to batch the requests
                friendIds.chunked(10).forEach { chunk ->
                    val snapshot = usersCollection.whereIn("uid", chunk).get().await()
                    users.addAll(snapshot.documents.mapNotNull { it.toObject(User::class.java) })
                }
                users
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get users by friend IDs", e)
                emptyList()
            }
        }
    }

    object ChatManager {
        private val chatsCollection = firestore.collection("chats")
        private val messagesCollection = firestore.collection("messages")

        // Send a message
        suspend fun sendMessage(message: Message) {
            try {
                val messageId = UUID.randomUUID().toString()
                val messageWithId = message.copy(messageId = messageId)
                messagesCollection.document(messageId).set(messageWithId).await()

                val chatId = getChatId(message.senderId, message.receiverId)
                val chatUpdates = mapOf(
                    "lastMessage" to message.content,
                    "lastMessageTime" to message.timestamp,
                    "lastMessageSenderId" to message.senderId,
                    "senderId" to message.senderId,
                    "receiverId" to message.receiverId
                )
                chatsCollection.document(chatId).set(chatUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                throw e
            }
        }

        // Get all messages between two users
        suspend fun getMessages(userId1: String, userId2: String, callback: (List<Message>) -> Unit) {
            try {
                val messages = messagesCollection
                    .whereIn("senderId", listOf(userId1, userId2))
                    .whereIn("receiverId", listOf(userId1, userId2))
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Message::class.java) }
                    .filter {
                        (it.senderId == userId1 && it.receiverId == userId2) ||
                                (it.senderId == userId2 && it.receiverId == userId1)
                    }
                callback(messages)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get messages", e)
                callback(emptyList())
            }
        }

        // Get all chats for the current user
        fun getUserChats(callback: (List<Chats>) -> Unit) {
            val currentUserId = auth.currentUser?.uid ?: return

            // Listen for real-time updates
            chatsCollection
                .whereEqualTo("senderId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Failed to get chat list", error)
                        callback(emptyList())
                        return@addSnapshotListener
                    }

                    val chats = mutableListOf<Chats>()
                    snapshot?.documents?.forEach { document ->
                        val chat = document.toObject(Chats::class.java)
                        if (chat != null) {
                            chats.add(chat)
                        }
                    }

                    // Also get chats where current user is receiver
                    chatsCollection
                        .whereEqualTo("receiverId", currentUserId)
                        .get()
                        .addOnSuccessListener { receiverSnapshot ->
                            receiverSnapshot.documents.forEach { document ->
                                val chat = document.toObject(Chats::class.java)
                                if (chat != null) {
                                    chats.add(chat)
                                }
                            }
                            callback(chats)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to get receiver chats", e)
                            callback(chats)
                        }
                }
        }

        // Upload media file (image, audio, video)
        suspend fun uploadMedia(uri: Uri, type: String): String {
            return withContext(Dispatchers.IO) {
                try {
                    val extension = when (type) {
                        "image" -> "jpg"
                        "audio" -> "3gp"
                        "video" -> "mp4"
                        else -> "dat"
                    }
                    val storageRef = storage.reference
                        .child("chat_media/${type}s/${UUID.randomUUID()}.$extension")
                    storageRef.putFile(uri).await()
                    storageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload media file", e)
                    throw e
                }
            }
        }

        // Generate a unique chat ID for two users
        private fun getChatId(userId1: String, userId2: String): String {
            return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
        }
    }

    object AuthManager {
        // Sign in
        suspend fun signIn(email: String, password: String): Result<User> {
            return try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    val userData = UserManager.getUser(user.uid)
                    if (userData != null) {
                        Result.success(userData)
                    } else {
                        Result.failure(Exception("User data not found"))
                    }
                } else {
                    Result.failure(Exception("Sign in failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed", e)
                Result.failure(e)
            }
        }

        // Sign up
        suspend fun signUp(email: String, password: String, name: String): Result<User> {
            return try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    val newUser = User(
                        uid = user.uid,
                        name = name,
                        email = email
                    )
                    UserManager.createUser(newUser)
                    Result.success(newUser)
                } else {
                    Result.failure(Exception("Sign up failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign up failed", e)
                Result.failure(e)
            }
        }

        // Sign out
        fun signOut() {
            auth.signOut()
        }
    }

    object GroupManager {
        private val groupsCollection = firestore.collection("groups")

        // Get groups where user is a member
        suspend fun getUserGroups(userId: String): List<Map<String, Any>> {
            return try {
                Log.d(TAG, "开始获取用户 $userId 的群组...")

                // 首先尝试从Firestore获取（支持数组和对象两种格式）
                Log.d(TAG, "尝试从Firestore获取群组...")

                // 尝试数组格式的查询
                val arraySnapshot = groupsCollection
                    .whereArrayContains("members", userId)
                    .get()
                    .await()

                Log.d(TAG, "Firestore数组查询结果: ${arraySnapshot.documents.size} 个文档")

                if (arraySnapshot.documents.isNotEmpty()) {
                    Log.d(TAG, "从Firestore数组格式获取到 ${arraySnapshot.documents.size} 个群组")
                    return arraySnapshot.documents.map { document ->
                        val data = document.data ?: emptyMap()
                        Log.d(TAG, "群组 ${document.id}: ${data}")
                        // 添加groupId字段
                        data.toMutableMap().apply {
                            put("groupId", document.id)
                        }
                    }
                }

                // 如果数组查询没有结果，尝试获取所有群组并手动检查
                Log.d(TAG, "数组查询无结果，尝试获取所有群组...")
                val allGroupsSnapshot = groupsCollection.get().await()
                Log.d(TAG, "Firestore总群组数: ${allGroupsSnapshot.documents.size}")

                val groupsFromFirestore = mutableListOf<Map<String, Any>>()
                for (document in allGroupsSnapshot.documents) {
                    val data = document.data ?: continue
                    val members = data["members"]
                    Log.d(TAG, "群组 ${document.id} 的members字段: $members (类型: ${members?.javaClass?.simpleName})")

                    var isMember = false
                    if (members is List<*>) {
                        // 数组格式
                        isMember = members.contains(userId)
                    } else if (members is Map<*, *>) {
                        // 对象格式
                        isMember = members.containsKey(userId)
                    }

                    if (isMember) {
                        Log.d(TAG, "用户 $userId 是群组 ${document.id} 的成员")
                        val groupMap = data.toMutableMap()
                        groupMap["groupId"] = document.id
                        groupsFromFirestore.add(groupMap)
                    }
                }

                if (groupsFromFirestore.isNotEmpty()) {
                    Log.d(TAG, "从Firestore手动检查获取到 ${groupsFromFirestore.size} 个群组")
                    return groupsFromFirestore
                }

                // 如果Firestore没有数据，尝试从Realtime Database获取
                Log.d(TAG, "Firestore中没有群组数据，尝试从Realtime Database获取")
                val rtdb = FirebaseDatabase.getInstance()
                val groupsSnapshot = rtdb.getReference("groups").get().await()

                Log.d(TAG, "Realtime Database查询结果: ${groupsSnapshot.childrenCount} 个群组")

                val groups = mutableListOf<Map<String, Any>>()
                for (groupSnap in groupsSnapshot.children) {
                    val groupId = groupSnap.key ?: continue
                    val groupData = groupSnap.value as? Map<String, Any> ?: continue
                    val members = groupSnap.child("members").children.mapNotNull { it.key }

                    Log.d(TAG, "检查群组 $groupId: 成员列表 = $members")

                    if (members.contains(userId)) {
                        Log.d(TAG, "用户 $userId 是群组 $groupId 的成员")
                        val groupMap = groupData.toMutableMap()
                        groupMap["groupId"] = groupId
                        groups.add(groupMap)
                    } else {
                        Log.d(TAG, "用户 $userId 不是群组 $groupId 的成员")
                    }
                }

                Log.d(TAG, "从Realtime Database获取到 ${groups.size} 个群组")
                groups

            } catch (e: Exception) {
                Log.e(TAG, "Failed to get user groups", e)
                emptyList()
            }
           }
        }
}