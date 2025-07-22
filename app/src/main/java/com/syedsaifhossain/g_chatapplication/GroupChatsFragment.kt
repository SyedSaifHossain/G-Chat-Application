package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.syedsaifhossain.g_chatapplication.adapter.GroupChatAdapter
import com.syedsaifhossain.g_chatapplication.models.GroupChat
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.syedsaifhossain.g_chatapplication.utils.GroupDataChecker

class GroupChatsFragment : Fragment() {

    private lateinit var adapter: GroupChatAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var allGroups: List<GroupChat> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        val view = inflater.inflate(R.layout.fragment_group_chats, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.groupChatsRecyclerView)
        val searchEditText = view.findViewById<EditText>(R.id.searchGroupEditText)

        adapter = GroupChatAdapter(emptyList()) { group ->
            // Navigate to group chat screen
            val bundle = Bundle().apply {
                putString("groupId", group.id)
                putString("groupName", group.name)
            }
            findNavController().navigate(R.id.groupChatFragment, bundle)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // 检查群组数据
        GroupDataChecker.checkGroupData()
        
        // Load all groups from Firebase or local
        loadGroups { groups ->
            allGroups = groups
            adapter.updateData(groups)
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                val filtered = if (query.isEmpty()) allGroups
                               else allGroups.filter { it.name.contains(query, ignoreCase = true) }
                adapter.updateData(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun loadGroups(callback: (List<GroupChat>) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            callback(emptyList())
            return
        }
        
        // 从Firestore加载用户所在的群组
        firestore.collection("groups")
            .whereArrayContains("members", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val groupList = mutableListOf<GroupChat>()
                for (document in snapshot.documents) {
                    val id = document.id
                    val data = document.data
                    val name = data?.get("name") as? String ?: "Unnamed"
                    val avatarUrl = data?.get("avatarUrl") as? String
                    val members = (data?.get("members") as? Map<String, Any>)?.keys?.toList() ?: emptyList()
                    groupList.add(GroupChat(id, name, avatarUrl, members))
                }
                callback(groupList)
            }
            .addOnFailureListener { e ->
                // 如果Firestore查询失败，尝试从Realtime Database加载
                val groupsRef = FirebaseDatabase.getInstance().getReference("groups")
                groupsRef.get().addOnSuccessListener { snapshot ->
                    val groupList = mutableListOf<GroupChat>()
                    for (groupSnap in snapshot.children) {
                        val id = groupSnap.key ?: continue
                        val name = groupSnap.child("name").getValue(String::class.java) ?: "Unnamed"
                        val avatarUrl = groupSnap.child("avatarUrl").getValue(String::class.java)
                        val members = groupSnap.child("members").children.mapNotNull { it.key }
                        groupList.add(GroupChat(id, name, avatarUrl, members))
                    }
                    callback(groupList)
                }
            }
    }
} 