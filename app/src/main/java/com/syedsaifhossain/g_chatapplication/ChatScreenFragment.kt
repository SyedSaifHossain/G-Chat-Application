package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.adapter.ChatMessageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatScreenBinding
import com.syedsaifhossain.g_chatapplication.models.ChatModel
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import com.vanniktech.emoji.EmojiImageView
import com.vanniktech.emoji.emoji.Emoji
import java.util.UUID

class ChatScreenFragment : Fragment(), AddOptionsBottomSheet.AddOptionClickListener {

    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private val chatList = mutableListOf<ChatModel>()
    private lateinit var currentUserId: String
    private lateinit var emojiPopup: EmojiPopup

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImageToFirebase(uri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

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

        // Emoji Popup
        emojiPopup = EmojiPopup.Builder.fromRootView(binding.root)
            .setOnEmojiPopupDismissListener {
                binding.imoButton.setImageResource(R.drawable.sticker)
            }
            .setOnEmojiPopupShownListener {
                binding.imoButton.setImageResource(R.drawable.keyboard)
            }
            .setOnEmojiClickListener { _: EmojiImageView, emoji: Emoji ->
                sendMessageToFirebase(emoji.unicode)
                emojiPopup.dismiss()
            }
            .build(binding.chatMessageInput)

        // Toggle emoji popup
        binding.imoButton.setOnClickListener {
            if (emojiPopup.isShowing) emojiPopup.dismiss()
            else emojiPopup.toggle()
        }

        listenForMessages()
        handleMessageSendOnEnter()

        binding.chatMessageBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.chatAddButton.setOnClickListener {
            AddOptionsBottomSheet(this@ChatScreenFragment)
                .show(parentFragmentManager, "AddOptionsBottomSheet")
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

                chatList.sortBy { it.timestamp }
                chatMessageAdapter.notifyDataSetChanged()
                binding.chatScreenRecyclerView.scrollToPosition(chatList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Interface implementation (👇 These methods solve your error)
    override fun onAlbumClicked() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // For Android 13 and above, use READ_MEDIA_IMAGES
                checkAndRequestPermission(Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                // For Android 12 and below, use READ_EXTERNAL_STORAGE
                checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkAndRequestPermission(permission: String) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}")
        
        binding.progressBar.visibility = View.VISIBLE // Add a progress bar to your layout
        
        imageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Send the image URL as a message
                    sendMessageToFirebase("📷 " + downloadUrl.toString())
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun onCameraClicked() {
        Toast.makeText(requireContext(), "Camera Clicked", Toast.LENGTH_SHORT).show()
        // TODO: Launch camera
    }

    override fun onVideoCallClicked() {
        Toast.makeText(requireContext(), "Video Call Clicked", Toast.LENGTH_SHORT).show()
        // TODO: Start video call
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}