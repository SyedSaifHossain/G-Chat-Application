package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.adapter.GroupChatMessageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentGroupChatBinding
import com.syedsaifhossain.g_chatapplication.models.ChatModel

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var groupChatMessageAdapter: GroupChatMessageAdapter
    private val chatList = mutableListOf<ChatModel>()

    private lateinit var currentUserId: String
    private lateinit var groupId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Get current user ID
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        // 2. Get groupId from arguments
        groupId = arguments?.getString("groupId") ?: "default_group"

        // 3. Set up RecyclerView with adapter
        groupChatMessageAdapter = GroupChatMessageAdapter(chatList, currentUserId)
        binding.groupChatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = groupChatMessageAdapter
        }

        // 4. Load messages and setup input
        listenForMessages()
        handleSendMessage()

        // 5. Back button
        binding.groupchatBackImg.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun handleSendMessage() {
        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val messageText = binding.messageInput.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    sendMessageToFirebase(messageText)
                    binding.messageInput.setText("")
                }
                true
            } else {
                false
            }
        }
    }

    private fun sendMessageToFirebase(messageText: String) {
        val chatRef = FirebaseDatabase.getInstance().getReference("group_chats").child(groupId)
        val messageId = chatRef.push().key ?: return
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val timestamp = System.currentTimeMillis().toString()

        val message = ChatModel(senderId, messageText, timestamp)
        chatRef.child(messageId).setValue(message)
    }

    private fun listenForMessages() {
        val chatRef = FirebaseDatabase.getInstance().getReference("group_chats").child(groupId)

        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatModel::class.java)
                    message?.let { chatList.add(it) }
                }

                chatList.sortBy { it.timestamp }
                groupChatMessageAdapter.notifyDataSetChanged()
                binding.groupChatRecyclerView.scrollToPosition(chatList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}