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
import androidx.core.content.FileProvider
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.os.Environment

class ChatScreenFragment : Fragment(), AddOptionsBottomSheet.AddOptionClickListener {

    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private val chatList = mutableListOf<ChatModel>()
    private lateinit var currentUserId: String
    private lateinit var emojiPopup: EmojiPopup
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var selectedCameraPhotoUri: Uri? = null

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

    // Camera permission launcher
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val photoFile = File(path)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )
                selectedCameraPhotoUri = photoUri
                binding.chatMessageInput.setText("📷 Photo taken. Press Enter to send.")
            }
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
                if (selectedCameraPhotoUri != null) {
                    uploadImageToFirebase(selectedCameraPhotoUri!!)
                    selectedCameraPhotoUri = null
                    binding.chatMessageInput.setText("")
                } else if (selectedImageUri != null) {
                    uploadImageToFirebase(selectedImageUri!!)
                    selectedImageUri = null
                } else {
                    val message = binding.chatMessageInput.text.toString().trim()
                    if (message.isNotEmpty()) {
                        sendMessageToFirebase(message)
                        binding.chatMessageInput.setText("")
                    }
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
        val message = ChatModel(
            senderId = senderId,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Message sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        try {
            // Check if the URI is valid
            if (imageUri == Uri.EMPTY) {
                Toast.makeText(requireContext(), "Invalid image selected", Toast.LENGTH_SHORT).show()
                return
            }

            // Check if the file exists
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Toast.makeText(requireContext(), "Cannot access the selected image", Toast.LENGTH_SHORT).show()
                return
            }
            inputStream.close()

            // Compress the image
            val compressedImageUri = compressImage(imageUri)
            if (compressedImageUri == null) {
                Toast.makeText(requireContext(), "Failed to compress image", Toast.LENGTH_SHORT).show()
                return
            }

            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}")
            
            binding.progressBar.visibility = View.VISIBLE
            
            // Get the file extension
            val mimeType = requireContext().contentResolver.getType(imageUri)
            val extension = mimeType?.substringAfterLast("/") ?: "jpg"
            val finalImageRef = imageRef.child("image.$extension")
            
            // Create metadata
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()
            
            finalImageRef.putFile(compressedImageUri, metadata)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    binding.progressBar.progress = progress
                }
                .addOnSuccessListener { taskSnapshot ->
                    finalImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Create a chat message with the image URL
                        val chatRef = FirebaseDatabase.getInstance().getReference("chats")
                        val messageId = chatRef.push().key ?: return@addOnSuccessListener

                        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                        val message = ChatModel(
                            senderId = senderId,
                            message = "📷 Image",
                            imageUrl = downloadUrl.toString(),
                            timestamp = System.currentTimeMillis()
                        )

                        chatRef.child(messageId).setValue(message)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Image sent successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Failed to send image: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        binding.progressBar.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    val errorMessage = when {
                        e.message?.contains("permission") == true -> "Storage permission denied"
                        e.message?.contains("network") == true -> "Network error. Please check your connection"
                        else -> "Failed to upload image: ${e.message}"
                    }
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun compressImage(imageUri: Uri): Uri? {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Calculate new dimensions
            val maxDimension = 1024 // Maximum dimension for the compressed image
            val width = bitmap.width
            val height = bitmap.height
            var newWidth = width
            var newHeight = height

            if (width > height && width > maxDimension) {
                newWidth = maxDimension
                newHeight = (height * maxDimension) / width
            } else if (height > maxDimension) {
                newHeight = maxDimension
                newWidth = (width * maxDimension) / height
            }

            // Create compressed bitmap
            val compressedBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Create a temporary file for the compressed image
            val tempFile = java.io.File.createTempFile("compressed_", ".jpg", requireContext().cacheDir)
            val outputStream = java.io.FileOutputStream(tempFile)
            compressedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()

            // Clean up
            bitmap.recycle()
            compressedBitmap.recycle()

            return Uri.fromFile(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onCameraClicked() {
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                val photoFile = createImageFile()
                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(intent)
            } else {
                Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onVideoCallClicked() {
        findNavController().navigate(R.id.action_chatScreenFragment_to_videoCallFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}