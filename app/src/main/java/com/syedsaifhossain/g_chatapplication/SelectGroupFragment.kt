package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.syedsaifhossain.g_chatapplication.adapter.GroupAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentSelectGroupBinding
import com.syedsaifhossain.g_chatapplication.models.GroupItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SelectGroupFragment : Fragment(), GroupAdapter.OnItemClickListener {
    private lateinit var binding: FragmentSelectGroupBinding
    private lateinit var groupAdapter: GroupAdapter
    private val groupList = arrayListOf<GroupItem>()
    private val allGroupList = arrayListOf<GroupItem>() // 全量数据用于搜索

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSelectGroupBinding.inflate(inflater, container, false)

        groupAdapter = GroupAdapter(groupList, this)
        binding.selectGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        binding.selectGroupBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        // 搜索栏监听
        binding.etSearchGroup.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterGroups(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        loadGroupsFromFirebase()
        return binding.root
    }

    private fun loadGroupsFromFirebase() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val groupsRef = FirebaseDatabase.getInstance().getReference("groups")
        groupsRef.get().addOnSuccessListener { snapshot ->
            val newList = arrayListOf<GroupItem>()
            for (groupSnap in snapshot.children) {
                val groupId = groupSnap.key ?: continue
                val members = groupSnap.child("members").children.mapNotNull { it.key }
                if (members.contains(currentUserUid)) {
                    val name = groupSnap.child("name").getValue(String::class.java) ?: "Unnamed Group"
                    val desc = groupSnap.child("description").getValue(String::class.java) ?: ""
                    newList.add(GroupItem(groupId, R.drawable.cityimg, name, desc))
                }
            }
            allGroupList.clear()
            allGroupList.addAll(newList)
            groupList.clear()
            groupList.addAll(newList)
            groupAdapter.notifyDataSetChanged()
        }
    }

    private fun filterGroups(query: String) {
        val filtered = if (query.isBlank()) {
            allGroupList
        } else {
            allGroupList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        groupList.clear()
        groupList.addAll(filtered)
        groupAdapter.notifyDataSetChanged()
    }

    override fun onGroupItemClick(groupItem: GroupItem) {
        val bundle = Bundle().apply { putString("groupId", groupItem.groupId) }
        findNavController().navigate(R.id.action_selectGroupFragment_to_groupChatFragment, bundle)
    }
}