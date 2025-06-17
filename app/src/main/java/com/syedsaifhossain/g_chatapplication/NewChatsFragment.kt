package com.syedsaifhossain.g_chatapplication

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.syedsaifhossain.g_chatapplication.adapter.NewChatAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentNewChatsBinding
import com.syedsaifhossain.g_chatapplication.models.NewChatItem

class NewChatsFragment : Fragment() {

    private lateinit var binding: FragmentNewChatsBinding
    private lateinit var chatList: ArrayList<NewChatItem>
    private lateinit var filteredList: ArrayList<NewChatItem>
    private lateinit var adapter: NewChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentNewChatsBinding.inflate(inflater, container, false)
        chatList = ArrayList<NewChatItem>().apply {
            add(NewChatItem("uid_1", "Friend 1", R.drawable.cityimg))
            add(NewChatItem("uid_2", "Friend 2", R.drawable.cityimg))
        }
        filteredList = ArrayList(chatList) // Initially set the filtered list to be the same as chatList
        adapter = NewChatAdapter(filteredList)
        binding.friendsList.layoutManager = LinearLayoutManager(requireContext())
        binding.friendsList.adapter = adapter

        // Set onClick listener for back button
        binding.backIcon.setOnClickListener {
            findNavController().popBackStack()
        }

        // Set onClick listener for Done button
        binding.doneButton.setOnClickListener {
            binding.doneButton.setBackgroundColor(Color.parseColor("#606060"))
            binding.doneButton.setTextColor(Color.parseColor("#000000"))

            val selectedUids = chatList.filter { it.isSelected }.map { it.uid }
            if (selectedUids.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Please select at least one friend", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showGroupNameDialog { groupName ->
                createGroupInFirebase(groupName, selectedUids)
            }
        }

        // Set onClick listener for select group
        binding.selectGroup.setOnClickListener{
            findNavController().navigate(R.id.action_newChatsFragment_to_selectGroupFragment)
        }

        // Add text watcher to the search bar for search functionality
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterGroups(s.toString()) // Filter groups based on the entered text
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFriends()
    }

    private fun loadFriends() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.child(currentUserUid).child("friends").get().addOnSuccessListener { snapshot ->
            val friendUids = snapshot.children.mapNotNull { it.key }

            if (friendUids.isEmpty()) {
                chatList.clear()
                adapter.notifyDataSetChanged()
                return@addOnSuccessListener
            }

            usersRef.get().addOnSuccessListener { usersSnap ->
                val friendList = mutableListOf<NewChatItem>()
                for (uid in friendUids) {
                    val userSnap = usersSnap.child(uid)
                    val name = userSnap.child("name").getValue(String::class.java) ?: "Unknown"
                    val avatarUrl = userSnap.child("profileImageUrl").getValue(String::class.java)
                        ?: userSnap.child("avatarUrl").getValue(String::class.java)
                        ?: ""
                    friendList.add(NewChatItem(uid, name, R.drawable.profileimage, avatarUrl))
                }

                // Sort and group
                val grouped = friendList.sortedBy { it.name.lowercase() }
                    .groupBy { it.name.first().uppercaseChar() }

                val newList = ArrayList<NewChatItem>()
                for ((letter, items) in grouped) {
                    // Add header with special uid
                    newList.add(NewChatItem("header_$letter", letter.toString(), 0))
                    newList.addAll(items)
                }

                chatList.clear()
                chatList.addAll(newList)
                filteredList.clear()
                filteredList.addAll(newList) // Update filtered list as well
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun filterGroups(query: String) {
        val filtered = if (query.isEmpty()) {
            ArrayList(chatList) // Return all items if the query is empty
        } else {
            chatList.filter { it.name.contains(query, ignoreCase = true) } as ArrayList<NewChatItem>
        }
        filteredList.clear()
        filteredList.addAll(filtered) // Update the filtered list
        adapter.notifyDataSetChanged() // Notify adapter about the data change
    }

    private fun showGroupNameDialog(onGroupNameEntered: (String) -> Unit) {
        val editText = android.widget.EditText(requireContext())
        editText.hint = "Enter group name"
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Group Name")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val groupName = editText.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    onGroupNameEntered(groupName)
                } else {
                    android.widget.Toast.makeText(requireContext(), "Group name cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createGroupInFirebase(groupName: String, memberUids: List<String>) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val groupRef = FirebaseDatabase.getInstance().getReference("groups").push()
        val groupId = groupRef.key ?: return
        val members = memberUids.associateWith { true }.toMutableMap()
        members[currentUserUid] = true // Add the current user to the group as well
        val groupData = mapOf(
            "name" to groupName,
            "owner" to currentUserUid,
            "members" to members,
            "createdAt" to System.currentTimeMillis()
        )
        groupRef.setValue(groupData)
            .addOnSuccessListener {
                val bundle = Bundle().apply { putString("groupId", groupId) }
                findNavController().navigate(R.id.groupChatFragment, bundle)
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(requireContext(), "Failed to create group", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
}