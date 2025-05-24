package com.syedsaifhossain.g_chatapplication.api

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.models.Message
import com.syedsaifhossain.g_chatapplication.models.User
import com.syedsaifhossain.g_chatapplication.models.Chats
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    object UserManager {
        private val usersRef = database.getReference("users")

        // Create a new user
        suspend fun createUser(user: User) {
            try {
                usersRef.child(user.uid).setValue(user).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create user", e)
                throw e
            }
        }

        // Get user info
        suspend fun getUser(userId: String): User? {
            return try {
                usersRef.child(userId).get().await().getValue(User::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get user info", e)
                null
            }
        }

        // Update user info
        suspend fun updateUser(userId: String, updates: Map<String, Any>) {
            try {
                usersRef.child(userId).updateChildren(updates).await()
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
    }

    object ChatManager {
        private val chatsRef = database.getReference("chats")
        private val messagesRef = database.getReference("messages")

        // Send a message
        suspend fun sendMessage(message: Message) {
            try {
                val messageId = UUID.randomUUID().toString()
                val messageWithId = message.copy(messageId = messageId)
                messagesRef.child(messageId).setValue(messageWithId).await()
                val chatId = getChatId(message.senderId, message.receiverId)
                val updates = mapOf(
                    "lastMessage" to message.content,
                    "lastMessageTime" to message.timestamp,
                    "lastMessageSenderId" to message.senderId,
                    "senderId" to message.senderId,
                    "receiverId" to message.receiverId
                )
                chatsRef.child(chatId).updateChildren(updates).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                throw e
            }
        }

        // Get all messages between two users
        suspend fun getMessages(userId1: String, userId2: String, callback: (List<Message>) -> Unit) {
            try {
                val chatId = getChatId(userId1, userId2)
                messagesRef.orderByChild("timestamp")
                    .get()
                    .await()
                    .children
                    .mapNotNull { it.getValue(Message::class.java) }
                    .filter {
                        (it.senderId == userId1 && it.receiverId == userId2) ||
                                (it.senderId == userId2 && it.receiverId == userId1)
                    }
                    .also { callback(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get messages", e)
                callback(emptyList())
            }
        }

        // Get all chats for the current user
        fun getUserChats(callback: (List<Chats>) -> Unit) {
            val currentUserId = auth.currentUser?.uid ?: return
            chatsRef.orderByChild("lastMessageTime")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val chats = mutableListOf<Chats>()
                        for (chatSnapshot in snapshot.children) {
                            val chat = chatSnapshot.getValue(Chats::class.java)
                            if (chat != null && (chat.senderId == currentUserId || chat.receiverId == currentUserId)) {
                                chats.add(chat)
                            }
                        }
                        callback(chats)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Failed to get chat list", error.toException())
                        callback(emptyList())
                    }
                })
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
}