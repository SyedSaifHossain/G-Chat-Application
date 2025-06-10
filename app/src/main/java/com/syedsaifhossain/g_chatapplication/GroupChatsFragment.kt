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

class GroupChatsFragment : Fragment() {

    private lateinit var adapter: GroupChatAdapter
    private var allGroups: List<GroupChat> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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