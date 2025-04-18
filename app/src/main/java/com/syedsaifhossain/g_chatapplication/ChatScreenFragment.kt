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
import com.syedsaifhossain.g_chatapplication.adapter.ChatMessageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatScreenBinding
import com.syedsaifhossain.g_chatapplication.models.ChatModel
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.EmojiEditText
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import android.view.inputmethod.InputMethodManager
import android.content.Context
import com.vanniktech.emoji.EmojiImageView
import com.vanniktech.emoji.emoji.Emoji

class ChatScreenFragment : Fragment() {

    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private val chatList = mutableListOf<ChatModel>()
    private lateinit var currentUserId: String
    private lateinit var emojiPopup: EmojiPopup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize EmojiManager with Google emojis
        EmojiManager.install(GoogleEmojiProvider())

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        chatMessageAdapter = ChatMessageAdapter(chatList, currentUserId)
        binding.chatScreenRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatMessageAdapter
        }

        // Initialize emoji popup with all emojis
        emojiPopup = EmojiPopup.Builder.fromRootView(binding.root)
            .setOnEmojiPopupDismissListener { 
                binding.imoButton.setImageResource(R.drawable.sticker)
            }
            .setOnEmojiPopupShownListener { 
                binding.imoButton.setImageResource(R.drawable.keyboard)
            }
            .setOnEmojiClickListener { imageView: EmojiImageView, emoji: Emoji ->
                // Send the emoji immediately
                sendMessageToFirebase(emoji.unicode)
                // Hide emoji picker
                emojiPopup.dismiss()
            }
            .setPageTransformer { page, position -> }  // Add page transformer for smooth scrolling
            .build(binding.chatMessageInput)

        // Set up emoji button click listener
        binding.imoButton.setOnClickListener {
            if (emojiPopup.isShowing) {
                emojiPopup.dismiss()
            } else {
                emojiPopup.toggle()
            }
        }

        listenForMessages()
        handleMessageSendOnEnter()
        binding.chatMessageBackImg.setOnClickListener {
            findNavController().popBackStack()
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
        val chatRef = FirebaseDatabase.getInstance().getReference("chats")
        val messageId = chatRef.push().key ?: return

        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val message = ChatModel(senderId, messageText, System.currentTimeMillis().toString())

        chatRef.child(messageId).setValue(message)
    }

    private fun listenForMessages() {
        val chatRef = FirebaseDatabase.getInstance().getReference("chats")
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatModel::class.java)
                    message?.let { chatList.add(it) }
                }

                chatList.sortBy { it.timestamp } // Ensure proper order
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