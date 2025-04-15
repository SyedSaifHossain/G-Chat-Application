package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.adapter.ChatMessageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatScreenBinding
import com.syedsaifhossain.g_chatapplication.models.ChatModel

class ChatScreenFragment : Fragment() {

    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private val chatList = mutableListOf<ChatModel>()
    private lateinit var currentUserId: String
    private lateinit var groupId: String

    private val args: ChatScreenFragmentArgs by navArgs() // Make sure navArgs is set up in navigation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        groupId = args.groupId // Group ID passed from previous screen

        setupRecyclerView()
        listenForMessages()
        handleMessageSendOnEnter()
    }

    private fun setupRecyclerView() {
        chatMessageAdapter = ChatMessageAdapter(chatList, currentUserId)
        binding.chatScreenRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatMessageAdapter
        }
    }

    private fun handleMessageSendOnEnter() {
        binding.chatMessageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val message = binding.chatMessageInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessageToFirebase(message)
                    binding.chatMessageInput.setText("")
                }
                true
            } else {
                false
            }
        }
    }

    private fun sendMessageToFirebase(messageText: String) {
        val senderId = currentUserId
        val chatRef = FirebaseDatabase.getInstance()
            .getReference("groups")
            .child(groupId)
            .child("messages")

        val messageId = chatRef.push().key ?: return
        val message = ChatModel(senderId, messageText, System.currentTimeMillis().toString())

        chatRef.child(messageId).setValue(message)
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        val chatRef = FirebaseDatabase.getInstance()
            .getReference("groups")
            .child(groupId)
            .child("messages")

        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatModel::class.java)
                    message?.let { chatList.add(it) }
                }
                chatList.sortBy { it.timestamp }
                chatMessageAdapter.notifyDataSetChanged()
                binding.chatScreenRecyclerView.scrollToPosition(chatList.size - 1)
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