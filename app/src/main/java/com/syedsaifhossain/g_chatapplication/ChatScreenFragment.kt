package com.syedsaifhossain.g_chatapplication

// Original Imports
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.os.Environment
// Added Imports
import android.util.Log

// Ensure the class declaration includes the interface implementation from your original
class ChatScreenFragment : Fragment(), AddOptionsBottomSheet.AddOptionClickListener {

    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    // Original Variables
    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private val chatList = mutableListOf<ChatModel>()
    private lateinit var currentUserId: String
    private lateinit var emojiPopup: EmojiPopup
    // Variables from original code for image/camera handling (kept)
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var selectedCameraPhotoUri: Uri? = null
    // Variables from original code for audio (kept)
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    // --- ADDED Member Variables for Arguments (Nullable) ---
    private var otherUserId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatarUrl: String? = null // Still needed for RecyclerView later
    private var myAvatarUrl: String? = null // Still needed for RecyclerView later
    // --- END Member Variables ---

    // Original ActivityResultLaunchers (kept)
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
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
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
    // --- END ActivityResultLaunchers ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatMessageInput.imeOptions = EditorInfo.IME_ACTION_SEND
        binding.chatMessageInput.setRawInputType(android.text.InputType.TYPE_CLASS_TEXT)
        // --- Receive Arguments (Revised) ---
        arguments?.let { bundle ->
            otherUserId = bundle.getString("otherUserId")
            otherUserName = bundle.getString("otherUserName")
            otherUserAvatarUrl = bundle.getString("otherUserAvatarUrl")
            myAvatarUrl = bundle.getString("myAvatarUrl")
            Log.d("ChatScreenFragment", "Arguments received: otherUserId=$otherUserId, otherUserName=$otherUserName, otherUserAvatarUrl=$otherUserAvatarUrl, myAvatarUrl=$myAvatarUrl")
        }

        // Check if essential arguments were received
        if (otherUserId == null || otherUserName == null) {
            Log.e("ChatScreenFragment", "Essential arguments (otherUserId or otherUserName) are missing!")
            Toast.makeText(requireContext(), "Error loading chat information.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack() // Go back if essential info is missing
            return // Stop further execution
        }
        // --- END Receive Arguments ---

        // Initialize EmojiManager (Original)
        EmojiManager.install(GoogleEmojiProvider())

        // Get current user ID (Original)
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        if (currentUserId == "anonymous") {
            Log.e("ChatScreenFragment", "Current user is anonymous!")
            // Consider stronger error handling
        }

        // --- MODIFIED: Setup RecyclerView Adapter to pass avatar URLs ---
        chatMessageAdapter = ChatMessageAdapter(chatList, currentUserId, myAvatarUrl, otherUserAvatarUrl)
        // --- END MODIFICATION ---

        binding.chatScreenRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatMessageAdapter
        }

        // Setup Emoji Popup (Original)
        emojiPopup = EmojiPopup.Builder.fromRootView(binding.root)
            .setOnEmojiPopupShownListener { /* Original listener if any */ }
            .setOnEmojiPopupDismissListener { /* Original listener if any */ }
            .setOnEmojiClickListener { _, emoji ->
                sendMessageToFirebase(emoji.unicode)
                emojiPopup.dismiss()
            }
            .build(binding.chatMessageInput)

        binding.imoButton.setOnClickListener {
            if (emojiPopup.isShowing) emojiPopup.dismiss()
            else emojiPopup.toggle()
        }

        // Start listening for messages (Original)
        listenForMessages()
        // Setup message input listener (Original)
        handleMessageSendOnEnter()

        // Setup back button listener (Original)
        binding.chatMessageBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        // Setup add button listener (Original)
        binding.chatAddButton.setOnClickListener {
            AddOptionsBottomSheet(this@ChatScreenFragment)
                .show(parentFragmentManager, "AddOptionsBottomSheet")
        }

        // Setup mic button listener (Original)
        binding.chatMicButton.setOnClickListener {
            if (checkAudioPermission()) {
                startRecording()
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            }
        }

        // Setup input action listener (Original - related to audio?)
        binding.chatMessageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                stopRecording()
                sendAudioMessage()
                true
            } else {
                false
            }
        }

        // --- Display Data in Toolbar (Name only) ---
        binding.tvToolbarUserName.text = otherUserName

    } // End of onViewCreated

    private fun handleMessageSendOnEnter() {
        binding.chatMessageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                if (mediaRecorder != null) {
                    stopRecording()
                    sendAudioMessage()
                } else if (selectedCameraPhotoUri != null) {
                    uploadImageToFirebase(selectedCameraPhotoUri!!)
                    selectedCameraPhotoUri = null
                    binding.chatMessageInput.setText("")
                } else if (selectedImageUri != null) {
                    uploadImageToFirebase(selectedImageUri!!)
                    selectedImageUri = null
                    binding.chatMessageInput.setText("")
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
        Log.d("sendMessageToFirebase", "sendMessageToFirebase() called with message: $messageText")
        if (otherUserId == null) {
            Log.e("ChatScreenFragment", "otherUserId is null in sendMessageToFirebase!")
            Toast.makeText(requireContext(), "Cannot send message.  Chat partner is unknown.", Toast.LENGTH_SHORT).show()
            return
        }

        val chatRef = FirebaseDatabase.getInstance().getReference("chats")
        val messageId = chatRef.push().key ?: run {
            Log.e("ChatScreenFragment", "Failed to get a new message key from Firebase.")
            Toast.makeText(requireContext(), "Failed to send message.  Try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val message = ChatModel(
            senderId = senderId,
            message = messageText,
            timestamp = System.currentTimeMillis() // Use String for timestamp for consistency
        )

        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d("ChatScreenFragment", "Message sent successfully to Firebase.")
                binding.chatMessageInput.setText("") // Clear the input field on success.
            }
            .addOnFailureListener { error ->
                Log.e("ChatScreenFragment", "Failed to send message to Firebase: ${error.message}", error)
                Toast.makeText(
                    requireContext(),
                    "Failed to send message: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun listenForMessages() {
        if (otherUserId == null) {
            Log.e("ChatScreenFragment", "Cannot listen for messages, otherUserId is null.")
            return
        }
        // TODO: Listen to a specific chat node based on involved user IDs
        // Example: val chatNodeId = getChatNodeId(currentUserId, otherUserId!!)
        val chatRef = FirebaseDatabase.getInstance().getReference("chats") //.child(chatNodeId)
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatModel::class.java)
                    // TODO: Filter messages to ensure they belong to this specific chat
                    // if (message != null && isMessageForThisChat(message, currentUserId, otherUserId!!)) {
                    //     message?.let { chatList.add(it) }
                    // }
                    message?.let { chatList.add(it) } // Current: Adds all messages
                }
                chatList.sortBy { it.timestamp }
                chatMessageAdapter.notifyDataSetChanged()
                if (chatList.isNotEmpty()) {
                    binding.chatScreenRecyclerView.scrollToPosition(chatList.size - 1)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load messages", Toast.LENGTH_SHORT).show()
                Log.e("ChatScreenFragment", "Failed to load messages", error.toException())
            }
        })
    }

    override fun onAlbumClicked() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> checkAndRequestPermission(Manifest.permission.READ_MEDIA_IMAGES)
            else -> checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun checkAndRequestPermission(permission: String) {
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> openGallery()
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        try {
            if (imageUri == Uri.EMPTY) { Toast.makeText(requireContext(), "Invalid image selected", Toast.LENGTH_SHORT).show(); return }
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            if (inputStream == null) { Toast.makeText(requireContext(), "Cannot access image", Toast.LENGTH_SHORT).show(); return }
            inputStream.close()

            val compressedImageUri = compressImage(imageUri) ?: run { Toast.makeText(requireContext(), "Failed to compress", Toast.LENGTH_SHORT).show(); return }

            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}")
            _binding?.progressBar?.visibility = View.VISIBLE
            val mimeType = requireContext().contentResolver.getType(compressedImageUri) ?: "image/jpeg"
            val extension = mimeType.substringAfterLast('/') ?: "jpg"
            val finalImageRef = imageRef.child("image.$extension")
            val metadata = com.google.firebase.storage.StorageMetadata.Builder().setContentType(mimeType).build()

            finalImageRef.putFile(compressedImageUri, metadata)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    _binding?.progressBar?.progress = progress
                }
                .addOnSuccessListener {
                    finalImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        if (otherUserId == null) { Log.e("ChatScreenFragment", "Cannot send image message, otherUserId is null."); _binding?.progressBar?.visibility = View.GONE; return@addOnSuccessListener }
                        val chatRef = FirebaseDatabase.getInstance().getReference("chats") // TODO: Use specific node
                        val messageId = chatRef.push().key ?: return@addOnSuccessListener
                        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                        val message = ChatModel(
                            senderId = senderId, message = "📷 Image", imageUrl = downloadUrl.toString(), timestamp = System.currentTimeMillis()
                            // TODO: Add avatar URLs?
                        )
                        chatRef.child(messageId).setValue(message)
                            .addOnSuccessListener { Toast.makeText(requireContext(), "Image sent", Toast.LENGTH_SHORT).show(); Log.d("ChatScreenFragment", "Image message sent") }
                            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to send image message: ${e.message}", Toast.LENGTH_SHORT).show(); Log.e("ChatScreenFragment", "Failed to send image message", e) }
                        _binding?.progressBar?.visibility = View.GONE
                    }.addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to get image URL: ${e.message}", Toast.LENGTH_SHORT).show(); _binding?.progressBar?.visibility = View.GONE; Log.e("ChatScreenFragment", "Failed to get download URL", e) }
                }
                .addOnFailureListener { e ->
                    val errorMessage = "Failed to upload: ${e.message}" // Simplified error
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    _binding?.progressBar?.visibility = View.GONE
                    Log.e("ChatScreenFragment", "Failed to upload image", e)
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
            _binding?.progressBar?.visibility = View.GONE
            Log.e("ChatScreenFragment", "Exception during image upload", e)
        }
    }

    private fun compressImage(imageUri: Uri): Uri? {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) return null

            val maxDimension = 1024
            val width = bitmap.width; val height = bitmap.height
            var newWidth = width; var newHeight = height
            if (width > height && width > maxDimension) { newWidth = maxDimension; newHeight = (height.toFloat() * maxDimension / width).toInt() }
            else if (height > maxDimension) { newHeight = maxDimension; newWidth = (width.toFloat() * maxDimension / height).toInt() }

            val compressedBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            bitmap.recycle()

            val tempFile = File.createTempFile("compressed_", ".jpg", requireContext().cacheDir)
            val outputStream = java.io.FileOutputStream(tempFile)
            compressedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush(); outputStream.close()
            compressedBitmap.recycle()
            return Uri.fromFile(tempFile)
        } catch (e: Exception) { Log.e("ChatScreenFragment", "Error compressing image", e); return null }
    }

    override fun onCameraClicked() { checkCameraPermission() }

    private fun checkCameraPermission() { checkAndRequestPermission(Manifest.permission.CAMERA) }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (requireActivity().packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
                val photoFile: File? = try { createImageFile() } catch (ex: java.io.IOException) { Log.e("ChatScreenFragment", "Error creating image file", ex); null }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", it)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    cameraLauncher.launch(intent)
                } ?: run { Toast.makeText(requireContext(), "Error preparing camera", Toast.LENGTH_SHORT).show() }
            } else { Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { Toast.makeText(requireContext(), "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show(); Log.e("ChatScreenFragment", "Error opening camera", e) }
    }

    @Throws(java.io.IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.mkdirs() // Ensure directory exists
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply { currentPhotoPath = absolutePath }
    }

    private fun checkAudioPermission(): Boolean { return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED }

    private fun startRecording() {
        try {
            val cacheDir = requireContext().cacheDir; cacheDir.mkdirs()
            audioFile = File(cacheDir, "voice_message_${UUID.randomUUID()}.3gp")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext()) else @Suppress("DEPRECATION") MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); setOutputFile(audioFile!!.absolutePath); prepare(); start()
                Toast.makeText(context, "Recording started...", Toast.LENGTH_SHORT).show(); Log.d("ChatScreenFragment", "Recording started to ${audioFile!!.absolutePath}")
            } ?: run { Log.e("ChatScreenFragment", "MediaRecorder initialization failed."); Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { Log.e("ChatScreenFragment", "Error starting recording", e); Toast.makeText(context, "Error starting recording: ${e.message}", Toast.LENGTH_SHORT).show(); mediaRecorder?.release(); mediaRecorder = null; audioFile?.delete(); audioFile = null }
    }

    private fun stopRecording() {
        try { mediaRecorder?.apply { stop(); release(); Log.d("ChatScreenFragment", "Recording stopped.") } }
        catch (e: RuntimeException) { Log.e("ChatScreenFragment", "Error stopping recording", e); audioFile?.delete() }
        finally { mediaRecorder = null; Toast.makeText(context, if (audioFile?.exists() == true) "Recording stopped" else "Recording failed", Toast.LENGTH_SHORT).show() }
    }

    private fun sendAudioMessage() {
        val fileToSend = audioFile
        if (fileToSend?.exists() == true && fileToSend.length() > 0) {
            val storageRef = FirebaseStorage.getInstance().reference; val audioRef = storageRef.child("chat_audio/${UUID.randomUUID()}.3gp"); val fileUri = Uri.fromFile(fileToSend)
            _binding?.progressBar?.visibility = View.VISIBLE
            audioRef.putFile(fileUri)
                .addOnProgressListener { taskSnapshot -> val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt(); _binding?.progressBar?.progress = progress }
                .addOnSuccessListener {
                    audioRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        if (otherUserId == null) { Log.e("ChatScreenFragment", "Cannot send audio message, otherUserId is null."); _binding?.progressBar?.visibility = View.GONE; return@addOnSuccessListener }
                        val chatRef = FirebaseDatabase.getInstance().getReference("chats") // TODO: Use specific node
                        val messageId = chatRef.push().key ?: return@addOnSuccessListener
                        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                        val message = ChatModel(senderId = senderId, message = "🎤 Voice Message", imageUrl = downloadUrl.toString(), timestamp = System.currentTimeMillis()) // Reusing imageUrl for audio URL? Consider dedicated field.
                        chatRef.child(messageId).setValue(message)
                            .addOnSuccessListener { Toast.makeText(requireContext(), "Voice message sent", Toast.LENGTH_SHORT).show(); Log.d("ChatScreenFragment", "Voice message sent") }
                            .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to send voice message: ${e.message}", Toast.LENGTH_SHORT).show(); Log.e("ChatScreenFragment", "Failed to send voice message", e) }
                        _binding?.progressBar?.visibility = View.GONE; audioFile?.delete(); audioFile = null
                    }.addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to get audio URL: ${e.message}", Toast.LENGTH_SHORT).show(); _binding?.progressBar?.visibility = View.GONE; Log.e("ChatScreenFragment", "Failed to get audio URL", e)}
                }
                .addOnFailureListener { e -> Toast.makeText(requireContext(), "Failed to upload voice message: ${e.message}", Toast.LENGTH_SHORT).show(); _binding?.progressBar?.visibility = View.GONE; Log.e("ChatScreenFragment", "Failed to upload voice message", e) }
        } else { Toast.makeText(context, "No valid recording found", Toast.LENGTH_SHORT).show(); Log.w("ChatScreenFragment", "sendAudioMessage called but audioFile invalid.") }
        audioFile = null // Reset after attempt
    }

    override fun onVideoCallClicked() {
        Toast.makeText(requireContext(), "Video call feature not implemented yet", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release()
        mediaRecorder = null
        audioFile?.delete()
        audioFile = null
        _binding = null
    }
    // --- End Original Methods ---
}