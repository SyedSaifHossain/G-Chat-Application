package com.syedsaifhossain.g_chatapplication.utils

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

object GroupDataChecker {
    private const val TAG = "GroupDataChecker"
    
    fun checkGroupData() {
        val auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            Log.e(TAG, "用户未登录")
            return
        }
        
        Log.d(TAG, "开始检查群组数据...")
        
        // 检查Realtime Database
        val rtdb = FirebaseDatabase.getInstance()
        rtdb.getReference("groups").get().addOnSuccessListener { snapshot ->
            Log.d(TAG, "Realtime Database检查结果:")
            if (snapshot.exists()) {
                val groupCount = snapshot.childrenCount
                Log.d(TAG, "✅ 找到 $groupCount 个群组")
                
                for (groupSnap in snapshot.children) {
                    val groupId = groupSnap.key
                    val groupName = groupSnap.child("name").getValue(String::class.java) ?: "未知"
                    val members = groupSnap.child("members").children.mapNotNull { it.key }
                    val isMember = members.contains(currentUserId)
                    
                    Log.d(TAG, "  群组: $groupId ($groupName)")
                    Log.d(TAG, "    成员数: ${members.size}")
                    Log.d(TAG, "    当前用户是成员: $isMember")
                    
                    // 检查消息
                    val messageKeys = groupSnap.children.mapNotNull { child ->
                        if (child.key != "name" && 
                            child.key != "description" && 
                            child.key != "createdBy" && 
                            child.key != "createdAt" && 
                            child.key != "members" &&
                            child.key != "admin" &&
                            child.key != "type") {
                            child.key
                        } else null
                    }
                    
                    Log.d(TAG, "    消息数: ${messageKeys.size}")
                }
            } else {
                Log.d(TAG, "❌ 没有找到群组数据")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Realtime Database检查失败", e)
        }
        
        // 检查Firestore
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("groups").get().addOnSuccessListener { snapshot ->
            Log.d(TAG, "Firestore检查结果:")
            Log.d(TAG, "✅ 找到 ${snapshot.size()} 个群组")
            
            for (document in snapshot.documents) {
                val groupId = document.id
                val data = document.data
                val groupName = data?.get("name") as? String ?: "未知"
                val members = (data?.get("members") as? Map<String, Any>)?.keys?.toList() ?: emptyList()
                val isMember = members.contains(currentUserId)
                
                Log.d(TAG, "  群组: $groupId ($groupName)")
                Log.d(TAG, "    成员数: ${members.size}")
                Log.d(TAG, "    当前用户是成员: $isMember")
                
                // 检查members字段的类型
                val membersField = data?.get("members")
                Log.d(TAG, "    members字段类型: ${membersField?.javaClass?.simpleName}")
                Log.d(TAG, "    members字段内容: $membersField")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Firestore检查失败", e)
        }
        
        // 检查Firestore中的消息
        firestore.collection("messages").get().addOnSuccessListener { snapshot ->
            Log.d(TAG, "Firestore消息检查结果:")
            Log.d(TAG, "✅ 找到 ${snapshot.size()} 条消息")
            
            val groupMessages = snapshot.documents.filter { doc ->
                val data = doc.data
                data?.get("groupId") != null || data?.get("messageType") == "group"
            }
            
            Log.d(TAG, "✅ 其中 ${groupMessages.size} 条是群组消息")
            
            if (groupMessages.isNotEmpty()) {
                for (i in 0 until minOf(3, groupMessages.size)) {
                    val doc = groupMessages[i]
                    val data = doc.data
                    Log.d(TAG, "  群组消息示例: groupId=${data?.get("groupId")}, text=${data?.get("text")?.toString()?.substring(0, minOf(50, data.get("text").toString().length))}...")
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Firestore消息检查失败", e)
        }
    }
} 