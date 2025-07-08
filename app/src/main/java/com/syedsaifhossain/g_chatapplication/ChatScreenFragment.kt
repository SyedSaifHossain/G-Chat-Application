package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaPlayer
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
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.DataSource
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import com.syedsaifhossain.g_chatapplication.adapter.ChatAdapter
import com.syedsaifhossain.g_chatapplication.models.Chats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.google.firebase.database.FirebaseDatabase
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// Ensure the class declaration includes the interface implementation from your original
class ChatScreenFragment : Fragment() {

    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    // Original Variables
    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private var chatList = mutableListOf<ChatModel>()
    private lateinit var currentUserId: String
    private lateinit var emojiPopup: EmojiPopup

    // Variables from original code for image/camera handling (kept)
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var selectedCameraPhotoUri: Uri? = null

    // Variables from original code for audio (kept)
    private var audioFile: File? = null

    // --- ADDED Member Variables for Arguments (Nullable) ---
    private var otherUserId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatarUrl: String? = null // Still needed for RecyclerView later
    private var myAvatarUrl: String? = null // Still needed for RecyclerView later

    // --- END Member Variables ---
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String = ""
    private var isRecording = false
    private var recordStartTime = 0L
    private var recordingTimer: Timer? = null
    private var elapsedTime: Long = 0
    private var lastVoiceDuration: Int = 0
    private var shouldSendVoiceMessage = true  // 新增：是否应该发送语音消息
    private var initialX = 0f // For WhatsApp style cancel
    private val CANCEL_X_THRESHOLD = 150f // Swipe left 150px to cancel

    private val messages = mutableListOf<Chats>()
    private lateinit var chatAdapter: ChatAdapter

    private var recordTimer: Timer? = null
    private var recordSeconds = 0

    private var previewPlayer: MediaPlayer? = null
    private var isPreviewPlaying = false
    private var previewFilePath: String? = null

    private var voicePlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isRecordingPhase = false
    private var recordedFilePath: String? = null

    // --- 新增：保存消息监听器和引用 ---
    private var chatValueEventListener: ValueEventListener? = null
    private var chatRef: DatabaseReference? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] != true) {
            Toast.makeText(requireContext(), "Audio permission is required", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // 优化后的权限申请Launcher，支持多权限
    private val requestGalleryPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Original ActivityResultLaunchers (kept)
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val mimeType = requireContext().contentResolver.getType(uri) ?: ""
                    if (mimeType.startsWith("image")) {
                        uploadImageToFirebase(uri)
                    } else if (mimeType.startsWith("video")) {
                        uploadVideoToFirebase(uri)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Unsupported file type",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                currentPhotoPath?.let { path ->
                    val photoFile = File(path)
                    val photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        photoFile
                    )
                    selectedCameraPhotoUri = photoUri
                    // 自动上传图片
                    uploadImageToFirebase(selectedCameraPhotoUri!!)
                    selectedCameraPhotoUri = null
                    binding.chatMessageInput.setText("")
                }
            }
        }

    // 1. Video launcher
    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val videoUri = result.data?.data
                if (videoUri != null) {
                    uploadVideoToFirebase(videoUri)
                } else {
                    Toast.makeText(requireContext(), "No video found", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private var isFragmentDestroying = false

    // For incoming calls
    private var incomingCallsRef: com.google.firebase.database.Query? = null
    private var incomingCallListener: com.google.firebase.database.ChildEventListener? = null

    // For outgoing calls (waiting dialog)
    private var waitingCallRef: com.google.firebase.database.DatabaseReference? = null
    private var waitingCallListener: com.google.firebase.database.ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("VoiceDebug", "onViewCreated called")
        Log.d("VoiceDebug", "setOnTouchListener for chatMicButton is set")
        binding.chatMicButton.setOnClickListener {
            if (!isRecordingPhase) {
                startRecordingWithBubble()
            } else {
                stopRecordingWithBubble()
            }
        }

        // --- Receive Arguments (Revised) ---
        arguments?.let { bundle ->
            otherUserId = bundle.getString("otherUserId")
            otherUserName = bundle.getString("otherUserName")
            otherUserAvatarUrl = bundle.getString("otherUserAvatarUrl")
            // 不再直接用bundle里的头像url
        }

        // Check if essential arguments were received
        if (otherUserId == null || otherUserName == null) {
            Log.e(
                "ChatScreenFragment",
                "Essential arguments (otherUserId or otherUserName) are missing!"
            )
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
        }

        // 实时获取双方头像url
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.child(otherUserId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                otherUserAvatarUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: snapshot.child("avatarUrl").getValue(String::class.java)
                            ?: ""
                // 加载到顶部头像前加日志
                Log.d("ChatScreenFragment", "加载到的头像URL: $otherUserAvatarUrl")
                usersRef.child(currentUserId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(mySnap: DataSnapshot) {
                            myAvatarUrl =
                                mySnap.child("profileImageUrl").getValue(String::class.java)
                                    ?: mySnap.child("avatarUrl").getValue(String::class.java)
                                            ?: ""
                            // 用最新头像初始化Adapter
                            chatMessageAdapter = ChatMessageAdapter(
                                chatList,
                                currentUserId,
                                myAvatarUrl,
                                otherUserAvatarUrl
                            ) { message, view ->
                                showMessageOptionsMenu(message, view)
                            }
                            binding.chatScreenRecyclerView.layoutManager =
                                LinearLayoutManager(requireContext())
                            binding.chatScreenRecyclerView.adapter = chatMessageAdapter
                            chatMessageAdapter.notifyDataSetChanged()
                            // 只有adapter初始化后再监听消息，避免未初始化异常
                            listenForMessages()
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }

            override fun onCancelled(error: DatabaseError) {}
        })

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

        // Setup message input listener (Original)
        handleMessageSendOnEnter()

        // Setup back button listener (Original)
        binding.chatMessageBackImg.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.videoIcon.setOnClickListener {
            initiateVideoCall()
        }

        binding.callIcon.setOnClickListener {
            initiateVoiceCall()
        }

        // Update camera button click event
        binding.cameraIcon.setOnClickListener {
            // Show option dialog
            val options = arrayOf("Take Photo", "Record Video")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Choose Action")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // Take photo
                            if (ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                openCamera()
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }

                        1 -> {
                            // Record video
                            if (ContextCompat.checkSelfPermission(
                                    requireContext(),
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                openVideoRecorder()
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                }
                .show()
        }


        binding.chatAddButton.setOnClickListener { view ->

            val popupView = LayoutInflater.from(view.context).inflate(R.layout.layout_custom_popup_bottom, null)

            // 测量 popup 高度
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupHeight = popupView.measuredHeight
            val popupWidth = popupView.measuredWidth

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            popupWindow.isOutsideTouchable = true
            popupWindow.elevation = 10f

            // 获取锚点位置
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val anchorX = location[0]
            val anchorY = location[1]

            // 显示在按钮上方右对齐
            val offsetX = anchorX + view.width - popupWidth
            val offsetY = anchorY - popupHeight

            popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, offsetX, offsetY)

            // 点击事件
            popupView.findViewById<LinearLayout>(R.id.item_gallery).setOnClickListener {
                popupWindow.dismiss()
                checkAndRequestGalleryPermission()
            }
            popupView.findViewById<LinearLayout>(R.id.item_document).setOnClickListener {
                popupWindow.dismiss()
            }
            popupView.findViewById<LinearLayout>(R.id.item_contact).setOnClickListener {
                popupWindow.dismiss()
            }
        }

        requestPermissionsIfNeeded()

        binding.tvToolbarUserName.text = otherUserName


        listenForIncomingCalls()

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding.chatSendButton.setOnClickListener {
            val message = binding.chatMessageInput.text.toString().trim()

            // Send text message
            if (message.isNotEmpty()) {
                sendMessageToFirebase(message)
                binding.chatMessageInput.setText("") // Clear the input field
            }
            // Send voice message
            else if (outputFile.isNotEmpty() && File(outputFile).exists()) {
                sendVoiceMessageWithCoroutine(outputFile) // Send voice message
                outputFile = "" // Clear the voice message file path
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please enter a message, record a voice, or upload a file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        binding.chatMessageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                after: Int,
            ) {
                // If there's text in the EditText, make the buttons invisible
                if (charSequence.isNullOrEmpty()) {
                    // Show the buttons if the input is empty
                    binding.cameraIcon.visibility = View.VISIBLE
                    binding.chatAddButton.visibility = View.VISIBLE
                    binding.chatMicButton.visibility = View.VISIBLE
                } else {
                    // Hide the buttons if there's text
                    binding.cameraIcon.visibility = View.INVISIBLE
                    binding.chatAddButton.visibility = View.INVISIBLE
                    binding.chatMicButton.visibility = View.INVISIBLE
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })


        binding.moreIcon.setOnClickListener {
            val bundle = Bundle().apply {
                putString("otherUserId", otherUserId)
                putString("otherUserName", otherUserName)
                putString("otherUserAvatarUrl", otherUserAvatarUrl)
            }
            findNavController().navigate(R.id.action_chatScreenFragment_to_chatScreenPageMoreOptionFragment, bundle)
        }
    }


    private fun sendTextMessage(messageText: String) {
        // Generate message ID first
        val messageId = FirebaseDatabase.getInstance().getReference("chats").push().key ?: return

        val message = ChatModel(
            senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            message = messageText,
            timestamp = System.currentTimeMillis(),
            messageId = messageId  // Set the message ID
        )

        // Save to Firebase first
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val chatNodeId = getChatNodeId(senderId, otherUserId!!)
        val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)

        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                // Only add to local list after successful save
                chatList.add(message)
                chatMessageAdapter.notifyItemInserted(chatList.size - 1)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to send message: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendVoiceMessage(audioUrl: String, duration: Int) {
        Log.d("VoiceDebug", "sendVoiceMessage called with URL: $audioUrl, duration: $duration")
        val messageId = FirebaseDatabase.getInstance().getReference("chats").push().key ?: return
        Log.d("VoiceDebug", "Generated message ID: $messageId")

        val message = ChatModel(
            senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            message = audioUrl,
            timestamp = System.currentTimeMillis(),
            type = "voice",
            duration = duration,
            messageId = messageId
        )
        Log.d("VoiceDebug", "Created message object: $message")

        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val chatNodeId = getChatNodeId(senderId, otherUserId!!)
        val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)
        Log.d("VoiceDebug", "Sending to chat node: $chatNodeId")

        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d("VoiceDebug", "Voice message saved to database successfully")
                chatList.add(message)
                chatMessageAdapter.notifyItemInserted(chatList.size - 1)
            }
            .addOnFailureListener { e ->
                Log.e("VoiceDebug", "Failed to save voice message: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to send voice message: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // --- NEW METHOD: Create a unique chat node ID ---
    private fun getChatNodeId(userId1: String, userId2: String): String {
        // Sort the user IDs alphabetically to ensure the same chat ID
        // regardless of who started the chat
        val userIds = sortedSetOf(userId1, userId2)
        return userIds.joinToString("_")
    }
    // --- END NEW METHOD ---

    // --- MODIFIED: Handle message sending with integrated audio handling ---
    private fun handleMessageSendOnEnter() {
        binding.chatMessageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                //Voice messagge
                if (outputFile.isNotEmpty() && File(outputFile).exists()) {
                    sendVoiceMessageWithCoroutine(outputFile)
                    // Optional: reset outputFile after sending
                    outputFile = ""
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No voice message recorded",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Handle text message sending
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
    // --- END MODIFICATION ---

    // --- MODIFIED: Send message to specific chat node ---
    private fun sendMessageToFirebase(messageText: String) {
        if (otherUserId == null) {
            Log.e("ChatScreenFragment", "Cannot send message, otherUserId is null.")
            return
        }

        // 获取当前登录用户的 UID
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("ChatScreenFragment", "User not logged in")
            return
        }
        val senderId = currentUser.uid

        // Create a specific chat node ID based on the two users involved
        val chatNodeId = getChatNodeId(senderId, otherUserId!!)
        Log.d("ChatScreenFragment", "Sending message to chat node: $chatNodeId")

        // Reference to the specific chat node
        val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)

        val messageId = chatRef.push().key ?: return
        Log.d("ChatScreenFragment", "Generated message ID: $messageId")

        val message = ChatModel(
            senderId = senderId,  // 使用当前登录用户的 UID
            message = messageText,
            timestamp = System.currentTimeMillis()
        )
        Log.d("ChatScreenFragment", "Created message object: $message")

        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d(
                    "ChatScreenFragment",
                    "Message sent successfully to path: chats/$chatNodeId/$messageId"
                )
                sendPushToUser(otherUserId!!, messageText)
            }
            .addOnFailureListener { e ->
                Log.e(
                    "ChatScreenFragment",
                    "Failed to send message to path: chats/$chatNodeId/$messageId",
                    e
                )
                Toast.makeText(
                    requireContext(),
                    "Failed to send message: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    // --- END MODIFICATION ---

    private fun sendPushToUser(receiverId: String, message: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId)
        usersRef.child("fcmToken").addListenerForSingleValueEvent(object :
            com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val token = snapshot.getValue(String::class.java) ?: return
                sendFcmNotification(token, message)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun sendFcmNotification(token: String, message: String) {
        val json = """
            {
              \"to\": \"$token\",
              \"notification\": {
                \"title\": \"新消息\",
                \"body\": \"$message\"
              }
            }
        """.trimIndent()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .addHeader("Authorization", "key=AAAAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
            .post(body)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }

    // --- MODIFIED: Listen for messages from specific chat node ---
    private fun listenForMessages() {
        if (otherUserId == null) {
            Log.e("ChatScreenFragment", "Cannot listen for messages, otherUserId is null.")
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("ChatScreenFragment", "User not logged in")
            return
        }
        val senderId = currentUser.uid

        val chatNodeId = getChatNodeId(senderId, otherUserId!!)
        Log.d("ChatScreenFragment", "Listening for messages in chat node: $chatNodeId")

        // --- 新增：保存 chatRef ---
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)

        // --- 新增：移除旧监听 ---
        chatValueEventListener?.let { chatRef?.removeEventListener(it) }

        // --- 新增：保存监听器引用 ---
        chatValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // --- 新增：防止 binding 为空崩溃 ---
                if (_binding == null) return
                Log.d("ChatScreenFragment", "Received data snapshot: " + snapshot.exists())
                chatList.clear()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatModel::class.java)
                    Log.d(
                        "ChatDebug",
                        "msgId=${child.key}, deleted=${message?.deleted}, msg=${message?.message}"
                    )
                    val messageWithId = message?.copy(messageId = child.key ?: "")
                    if (messageWithId != null) chatList.add(messageWithId)
                }
                Log.d("ChatScreenFragment", "Total messages in chatList: ${chatList.size}")
                chatList.sortBy { it.timestamp }
                chatMessageAdapter.notifyDataSetChanged()
                if (chatList.isNotEmpty()) {
                    binding.chatScreenRecyclerView.scrollToPosition(chatList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                Toast.makeText(requireContext(), "Failed to load messages", Toast.LENGTH_SHORT)
                    .show()
                Log.e("ChatScreenFragment", "Failed to load messages", error.toException())
            }
        }
        chatRef?.addValueEventListener(chatValueEventListener!!)
    }
    // --- END MODIFICATION ---

    // 替换原有checkAndRequestPermission方法，适配多版本和多权限
    private fun checkAndRequestGalleryPermission() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )

            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            openGallery()
        } else {
            requestGalleryPermissionsLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        galleryLauncher.launch(intent)
    }

    // --- MODIFIED: Upload image to Firebase with specific chat node ---
    private fun uploadImageToFirebase(imageUri: Uri) {
        try {
            if (imageUri == Uri.EMPTY) {
                return
            }
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                return
            }
            inputStream.close()

            val compressedImageUri = compressImage(imageUri) ?: run {
                return
            }

            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}")
            _binding?.progressBar?.visibility = View.VISIBLE
            val mimeType =
                requireContext().contentResolver.getType(compressedImageUri) ?: "image/jpeg"
            val extension = mimeType.substringAfterLast('/') ?: "jpg"
            val finalImageRef = imageRef.child("image.$extension")
            val metadata =
                com.google.firebase.storage.StorageMetadata.Builder().setContentType(mimeType)
                    .build()

            finalImageRef.putFile(compressedImageUri, metadata)
                .addOnProgressListener { taskSnapshot ->
                    val progress =
                        (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    _binding?.progressBar?.progress = progress
                }
                .addOnSuccessListener {
                    finalImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        if (otherUserId == null) {
                            Log.e(
                                "ChatScreenFragment",
                                "Cannot send image message, otherUserId is null."
                            ); _binding?.progressBar?.visibility =
                                View.GONE; return@addOnSuccessListener
                        }

                        // Create a specific chat node ID based on the two users involved
                        val chatNodeId = getChatNodeId(currentUserId, otherUserId!!)

                        val chatRef =
                            FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)
                        val messageId = chatRef.push().key ?: return@addOnSuccessListener
                        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                        val message = ChatModel(
                            senderId = senderId,
                            message = "📷 Image",
                            imageUrl = downloadUrl.toString(),
                            type = "image",
                            timestamp = System.currentTimeMillis()
                        )
                        chatRef.child(messageId).setValue(message)
                            .addOnSuccessListener {
                                Log.d("ChatScreenFragment", "Image message sent")
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "ChatScreenFragment",
                                    "Failed to send image message",
                                    e
                                )
                            }
                        _binding?.progressBar?.visibility = View.GONE
                    }.addOnFailureListener { e ->
                        _binding?.progressBar?.visibility =
                        View.GONE; Log.e("ChatScreenFragment", "Failed to get download URL", e)
                    }
                }
                .addOnFailureListener { e ->
                    val errorMessage = "Failed to upload: ${e.message}" // Simplified error
                    _binding?.progressBar?.visibility = View.GONE
                    Log.e("ChatScreenFragment", "Failed to upload image", e)
                }
        } catch (e: Exception) {
            _binding?.progressBar?.visibility = View.GONE
            Log.e("ChatScreenFragment", "Exception during image upload", e)
        }
    }
    // --- END MODIFICATION ---

    private fun compressImage(imageUri: Uri): Uri? {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val tempFileForExif =
                File.createTempFile("exif_temp_", ".jpg", requireContext().cacheDir)
            inputStream?.use { input ->
                tempFileForExif.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val exif = ExifInterface(tempFileForExif.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            var bitmap = android.graphics.BitmapFactory.decodeFile(tempFileForExif.absolutePath)
            if (bitmap == null) return null

            // 旋转修正
            val matrix = android.graphics.Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            }
            if (!matrix.isIdentity) {
                val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
                bitmap.recycle()
                bitmap = rotatedBitmap
            }

            val maxDimension = 1024
            val width = bitmap.width;
            val height = bitmap.height
            var newWidth = width;
            var newHeight = height
            if (width > height && width > maxDimension) {
                newWidth = maxDimension; newHeight =
                    (height.toFloat() * maxDimension / width).toInt()
            } else if (height > maxDimension) {
                newHeight = maxDimension; newWidth =
                    (width.toFloat() * maxDimension / height).toInt()
            }

            val compressedBitmap =
                android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            if (compressedBitmap != bitmap) bitmap.recycle()

            val tempFile = File.createTempFile("compressed_", ".jpg", requireContext().cacheDir)
            val outputStream = java.io.FileOutputStream(tempFile)
            compressedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush(); outputStream.close()
            compressedBitmap.recycle()
            tempFileForExif.delete()
            return Uri.fromFile(tempFile)
        } catch (e: Exception) {
            Log.e("ChatScreenFragment", "Error compressing image", e); return null
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: java.io.IOException) {
                Log.e("ChatScreenFragment", "Error creating image file", ex); null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(intent)
            } ?: run {
            }
        } catch (e: Exception) {
            Log.e("ChatScreenFragment", "Error opening camera", e)
        }
    }

    @Throws(java.io.IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.mkdirs() // Ensure directory exists
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            .apply { currentPhotoPath = absolutePath }
    }


    private fun requestPermissionsIfNeeded() {
        val neededPermissions = mutableListOf<String>()
        if (!checkAudioPermission()) neededPermissions.add(Manifest.permission.RECORD_AUDIO)
        if (neededPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording() {
        try {
            Log.d("VoiceDebug", "Preparing to start recording...")
            // Prepare recording file
            val dirPath = "${requireContext().externalCacheDir?.absolutePath}/voiceMessages"
            val dir = File(dirPath)
            if (!dir.exists()) dir.mkdirs()

            val fileName = "voice_${System.currentTimeMillis()}.3gp"
            outputFile = "$dirPath/$fileName"
            Log.d("VoiceDebug", "Recording file path: $outputFile")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile)
                prepare()
                start()
            }

            isRecording = true
            recordStartTime = System.currentTimeMillis()
            Log.d("VoiceDebug", "Recording started at timestamp: $recordStartTime")

            // Start timer
            startTimer()
            Log.d("VoiceDebug", "Recording UI and timer started")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VoiceDebug", "Failed to start recording: ${e.message}")
        }
    }

    private fun startTimer() {
        recordingTimer = Timer()
        recordingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                elapsedTime = System.currentTimeMillis() - recordStartTime
                val seconds = (elapsedTime / 1000).toInt()
                // Update the UI with the elapsed time
                activity?.runOnUiThread {
                    //   binding.recordingStatusText.text = "Recording... $seconds sec"
                }
            }
        }, 0, 1000) // Update every second
    }

    private fun stopRecording() {
        Log.d("VoiceDebug", "stopRecording called")
        try {
            val elapsed = System.currentTimeMillis() - recordStartTime
            lastVoiceDuration = (elapsed / 1000).toInt()
            Log.d("VoiceDebug", "Stopping recording, duration: ${elapsed}ms")

            if (elapsed < 1000) { // If recording is too short
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                } catch (e: Exception) {
                    Log.e(
                        "VoiceDebug",
                        "Exception during stop/release for short recording: ${e.message}"
                    )
                }
                mediaRecorder = null
                isRecording = false
                Log.d("VoiceDebug", "Recording cancelled - too short")
                return
            }

            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {
                Log.e("VoiceDebug", "Exception during stop/release: ${e.message}")
            } finally {
                mediaRecorder = null
                isRecording = false
                recordingTimer?.cancel()
            }

            // Log file size
            val file = File(outputFile)
            Log.d(
                "VoiceDebug",
                "Recording finished, file path: $outputFile, file size: ${file.length()} bytes"
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VoiceDebug", "Failed to stop recording: ${e.message}")
            mediaRecorder = null
            isRecording = false
            recordingTimer?.cancel()
        }
    }


    private fun sendVoiceMessageWithCoroutine(filePath: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val audioRef = storageRef.child("voiceMessages/${File(filePath).name}")
        val duration = lastVoiceDuration
        val file = File(filePath)
        Log.d(
            "VoiceDebug",
            "Preparing to upload voice file: $filePath, size: ${file.length()} bytes, duration: ${duration}s"
        )

        // Show upload progress
        _binding?.progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                audioRef.putFile(Uri.fromFile(File(filePath))).await()
                val downloadUrl = audioRef.downloadUrl.await()
                withContext(Dispatchers.Main) {
                    Log.d(
                        "VoiceDebug",
                        "Voice file uploaded successfully, downloadUrl: $downloadUrl"
                    )
                    sendVoiceMessage(downloadUrl.toString(), duration)
                    _binding?.progressBar?.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("VoiceDebug", "Failed to upload voice file: ${e.message}")
                    _binding?.progressBar?.visibility = View.GONE
                }
            }
        }
    }

    // 2. Open system video recorder
    private fun openVideoRecorder() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoLauncher.launch(intent)
    }

    // 3. Upload video to Firebase Storage and send as message
    private fun uploadVideoToFirebase(videoUri: Uri) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val videoRef = storageRef.child("chat_videos/${java.util.UUID.randomUUID()}.mp4")
        _binding?.progressBar?.visibility = View.VISIBLE
        val metadata =
            com.google.firebase.storage.StorageMetadata.Builder().setContentType("video/mp4")
                .build()
        videoRef.putFile(videoUri, metadata)
            .addOnProgressListener { taskSnapshot ->
                val progress =
                    (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                _binding?.progressBar?.progress = progress
            }
            .addOnSuccessListener {
                videoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    if (otherUserId == null) {
                        _binding?.progressBar?.visibility = View.GONE; return@addOnSuccessListener
                    }
                    val chatNodeId = getChatNodeId(currentUserId, otherUserId!!)
                    val chatRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("chats").child(chatNodeId)
                    val messageId = chatRef.push().key ?: return@addOnSuccessListener
                    val senderId =
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            ?: "anonymous"
                    val message = com.syedsaifhossain.g_chatapplication.models.ChatModel(
                        senderId = senderId,
                        message = "🎥 Video",
                        imageUrl = downloadUrl.toString(),
                        type = "video",
                        timestamp = System.currentTimeMillis()
                    )
                    chatRef.child(messageId).setValue(message)
                        .addOnSuccessListener {
                        }
                    _binding?.progressBar?.visibility = View.GONE
                }.addOnFailureListener { e ->
                    _binding?.progressBar?.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                _binding?.progressBar?.visibility = View.GONE
            }
    }

    private fun showMessageOptionsMenu(message: ChatModel, view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.message_options_menu, popupMenu.menu)

        // Set menu items visibility based on message status
        popupMenu.menu.findItem(R.id.menu_recall)?.isVisible = !message.deleted
        popupMenu.menu.findItem(R.id.editMessage)?.isVisible =
            !message.deleted && message.type == "text"

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_recall -> {
                    recallMessage(message)
                    true
                }

                R.id.editMessage -> {
                    editMessage(message)
                    true
                }

                R.id.deleteMessage -> {
                    deleteMessage(message)
                    true
                }

                R.id.copyMessage -> {
                    copyMessageToClipboard(message)
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun recallMessage(message: ChatModel) {
        if (otherUserId == null) return

        val chatNodeId = getChatNodeId(currentUserId, otherUserId!!)
        val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)

        // Update message status to deleted, and remove isDeleted field if exists
        val updates = mapOf(
            "deleted" to true,
            "isDeleted" to null, // remove old field if exists
            "message" to "This message was recalled"
        )

        chatRef.child(message.messageId).updateChildren(updates)
            .addOnSuccessListener {
                listenForMessages()
            }
    }

    private fun editMessage(message: ChatModel) {
        // Show edit dialog
        val editText = android.widget.EditText(requireContext()).apply {
            setText(message.message)
            filters = arrayOf(android.text.InputFilter.LengthFilter(1000))
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val newMessage = editText.text.toString().trim()
                if (newMessage.isNotEmpty() && newMessage != message.message) {
                    updateMessage(message, newMessage)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMessage(message: ChatModel, newContent: String) {
        if (otherUserId == null) return

        val chatNodeId = getChatNodeId(currentUserId, otherUserId!!)
        val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)

        // Update message content and edit status
        val updates = mapOf(
            "message" to newContent,
            "isEdited" to true,
            "editTimestamp" to System.currentTimeMillis()
        )

        chatRef.child(message.messageId).updateChildren(updates)
    }

    private fun deleteMessage(message: ChatModel) {
        if (otherUserId == null) return

        val chatNodeId = getChatNodeId(currentUserId, otherUserId!!)
        val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)

        chatRef.child(message.messageId).removeValue()
    }

    private fun copyMessageToClipboard(message: ChatModel) {
        val clipboard =
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("message", message.message)
        clipboard.setPrimaryClip(clip)
    }

    // 录音时和录音后都显示底部气泡
    private fun startRecordingWithBubble() {
        Log.d("VoiceDebug", "startRecordingWithBubble called")
        isRecordingPhase = true
        recordSeconds = 0
        val bubble = binding.voicePreviewLayout.root
        bubble.visibility = View.VISIBLE
        binding.chatScreenBottomLayout.visibility = View.GONE
        val tvDuration = bubble.findViewById<TextView>(R.id.tvDuration)
        val btnPlayPause = bubble.findViewById<ImageView>(R.id.btnPlayPause)
        val btnDelete = bubble.findViewById<ImageView>(R.id.btnDelete)
        val btnSend = bubble.findViewById<ImageView>(R.id.btnSend)

        // 确保按钮可点击
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        btnPlayPause.isEnabled = true
        btnSend.isEnabled = true
        btnSend.isClickable = true
        btnSend.isFocusable = true

        // 开始录音
        startRecording()
        // 录音时长计时
        recordTimer = Timer()
        recordTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                recordSeconds++
                activity?.runOnUiThread {
                    tvDuration.text =
                        String.format("%d:%02d", recordSeconds / 60, recordSeconds % 60)
                }
            }
        }, 1000, 1000)

        // 播放/暂停按钮：控制录音结束和回放
        var isRecording = true
        var previewPlayer: MediaPlayer? = null
        var isPreviewPlaying = false
        var previewTimer: Timer? = null
        var previewSeconds = 0

        btnPlayPause.setOnClickListener {
            if (isRecording) {
                // 结束录音
                stopRecording()
                isRecording = false
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                recordTimer?.cancel()
                recordTimer = null
                Log.d("VoiceDebug", "录音结束，文件路径: $recordedFilePath")
            } else {
                // 播放/暂停预览
                val audioFile = File(outputFile)
                if (!audioFile.exists()) {
                    Log.e("VoiceDebug", "录音文件不存在: $outputFile")
                    return@setOnClickListener
                }

                if (isPreviewPlaying) {
                    // 暂停预览
                    previewPlayer?.pause()
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    isPreviewPlaying = false
                    previewTimer?.cancel()
                    previewTimer = null
                } else {
                    // 开始预览
                    try {
                        if (previewPlayer == null) {
                            previewPlayer = MediaPlayer().apply {
                                setDataSource(outputFile)
                                prepare()
                                setOnCompletionListener {
                                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                                    isPreviewPlaying = false
                                    previewTimer?.cancel()
                                    previewTimer = null
                                    previewSeconds = 0
                                    tvDuration.text = String.format(
                                        "%d:%02d",
                                        recordSeconds / 60,
                                        recordSeconds % 60
                                    )
                                }
                            }
                        }
                        previewPlayer?.start()
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        isPreviewPlaying = true

                        // 开始预览计时器
                        previewSeconds = 0
                        previewTimer = Timer()
                        previewTimer?.scheduleAtFixedRate(object : TimerTask() {
                            override fun run() {
                                previewSeconds++
                                activity?.runOnUiThread {
                                    tvDuration.text = String.format(
                                        "%d:%02d",
                                        previewSeconds / 60,
                                        previewSeconds % 60
                                    )
                                }
                            }
                        }, 1000, 1000)

                        Log.d("VoiceDebug", "开始播放录音: $outputFile")
                    } catch (e: Exception) {
                        Log.e("VoiceDebug", "播放预览失败", e)
                    }
                }
            }
        }

        // 删除按钮：取消录音
        btnDelete.setOnClickListener {
            stopRecordingWithBubble()
            recordTimer?.cancel()
            previewTimer?.cancel()
            previewPlayer?.release()
            previewPlayer = null
            isRecordingPhase = false
            binding.voicePreviewLayout.root.visibility = View.GONE
            binding.chatScreenBottomLayout.visibility = View.VISIBLE
            File(outputFile).delete()
        }

        // 发送按钮：发送录音
        btnSend.setOnClickListener {
            Log.d("VoiceDebug", "Send Clicked")
            val audioFile = File(outputFile)
            if (audioFile.exists()) {
                stopRecordingWithBubble()
                recordTimer?.cancel()
                previewTimer?.cancel()
                previewPlayer?.release()
                previewPlayer = null
                isRecordingPhase = false
                sendVoiceMessageWithFirebase(outputFile, recordSeconds)
                binding.voicePreviewLayout.root.visibility = View.GONE
                binding.chatScreenBottomLayout.visibility = View.VISIBLE
            } else {
                Log.e("VoiceDebug", "录音文件不存在: $outputFile")
            }
        }

        // 添加日志检查按钮状态
        Log.d("VoiceDebug", "btnSend enabled: ${btnSend.isEnabled}")
        Log.d("VoiceDebug", "btnSend clickable: ${btnSend.isClickable}")
        Log.d("VoiceDebug", "btnSend focusable: ${btnSend.isFocusable}")
    }

    private fun stopRecordingWithBubble() {
        Log.d("VoiceDebug", "stopRecordingWithBubble called")
        stopRecording()
        recordTimer?.cancel()
        isRecordingPhase = false
        recordedFilePath = outputFile
        val bubble = binding.voicePreviewLayout.root
        val tvDuration = bubble.findViewById<TextView>(R.id.tvDuration)
        val btnPlayPause = bubble.findViewById<ImageView>(R.id.btnPlayPause)
        val btnDelete = bubble.findViewById<ImageView>(R.id.btnDelete)
        val btnSend = bubble.findViewById<ImageView>(R.id.btnSend)
        Log.d("VoiceDebug", "btnPlayPause=$btnPlayPause, btnSend=$btnSend, btnDelete=$btnDelete")
        tvDuration.text = String.format("%d:%02d", recordSeconds / 60, recordSeconds % 60)
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        btnPlayPause.isEnabled = true
        btnSend.isEnabled = true
    }

    // 录音语音消息上传并发送
    private fun sendVoiceMessageWithFirebase(filePath: String, duration: Int) {
        val storageRef = FirebaseStorage.getInstance().reference
        val voiceRef = storageRef.child("chat_voices/${UUID.randomUUID()}.3gp")
        val fileUri = Uri.fromFile(File(filePath))
        val uploadTask = voiceRef.putFile(fileUri)
        uploadTask.addOnSuccessListener {
            voiceRef.downloadUrl.addOnSuccessListener { uri ->
                val messageId = FirebaseDatabase.getInstance().getReference("chats").push().key
                    ?: return@addOnSuccessListener
                val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val chatNodeId = getChatNodeId(senderId, otherUserId!!)
                val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatNodeId)
                val message = ChatModel(
                    senderId = senderId,
                    message = uri.toString(),
                    timestamp = System.currentTimeMillis(),
                    type = "voice",
                    duration = duration
                )
                chatRef.child(messageId).setValue(message)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "语音上传失败: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // 新增：发起语音通话请求（微信式）
    private fun initiateVoiceCall() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val otherId = otherUserId ?: return
        val callId = FirebaseDatabase.getInstance().getReference("calls").push().key ?: return
        val callRequest = mapOf(
            "from" to currentUserId,
            "to" to otherId,
            "callType" to "voice",
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )
        FirebaseDatabase.getInstance().getReference("calls").child(callId).setValue(callRequest)
        showWaitingDialog(callId)
    }

    // 新增：发起视频通话请求（微信式）
    private fun initiateVideoCall() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val otherId = otherUserId ?: return
        val callId = FirebaseDatabase.getInstance().getReference("calls").push().key ?: return
        val callRequest = mapOf(
            "from" to currentUserId,
            "to" to otherId,
            "callType" to "video",
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )
        FirebaseDatabase.getInstance().getReference("calls").child(callId).setValue(callRequest)
        showWaitingDialog(callId)
    }

    // 新增：等待对方响应的弹窗和监听
    private fun showWaitingDialog(callId: String) {
        if (!isAdded || context == null) {
            Log.w("CallDebug", "showWaitingDialog: Fragment not attached")
            return
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Calling...")
            .setMessage("Waiting for the other user to accept")
            .setNegativeButton("Cancel") { d, _ ->
                FirebaseDatabase.getInstance().getReference("calls").child(callId).child("status")
                    .setValue("ended")
                d.dismiss()
            }
            .setCancelable(false)
            .create()
        dialog.show()

        // --- 正确管理监听器 ---
        waitingCallListener?.let { waitingCallRef?.removeEventListener(it) } // 移除旧的监听器

        waitingCallRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        waitingCallListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (isFragmentDestroying || !isAdded || context == null) {
                    Log.w("CallDebug", "showWaitingDialog onDataChange: Fragment not attached")
                    return
                }

                val status = snapshot.child("status").getValue(String::class.java)
                val callType = snapshot.child("callType").getValue(String::class.java)
                when (status) {
                    "accepted" -> {
                        dialog.dismiss()
                        val bundle = Bundle().apply { putString("callId", callId) }
                        try {
                            when (callType) {
                                "voice" -> findNavController().navigate(
                                    R.id.action_chatScreenFragment_to_voiceCallFragment,
                                    bundle
                                )

                                "video" -> findNavController().navigate(
                                    R.id.action_chatScreenFragment_to_videoCallFragment,
                                    bundle
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("CallDebug", "Navigation failed: ${e.message}")
                        }
                    }

                    "rejected", "ended" -> {
                        dialog.dismiss()
                        val message = if (status == "rejected") "Call rejected" else "Call ended"
                        context?.let { ctx ->
                        }
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("CallDebug", "showWaitingDialog onCancelled: ${error.message}")
            }
        }
        waitingCallRef?.addValueEventListener(waitingCallListener!!)
    }

    // 新增：监听calls节点，弹窗接受/拒绝
    private fun listenForIncomingCalls() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("CallDebug", "listenForIncomingCalls: currentUserId=$currentUserId")

        // --- 正确管理监听器 ---
        incomingCallListener?.let { incomingCallsRef?.removeEventListener(it) } // 移除旧的

        incomingCallsRef = FirebaseDatabase.getInstance().getReference("calls").orderByChild("to")
            .equalTo(currentUserId)
        incomingCallListener = object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?,
            ) {
                if (isFragmentDestroying || !isAdded || context == null) {
                    Log.w("CallDebug", "listenForIncomingCalls onChildAdded: Fragment not attached")
                    return
                }

                val status = snapshot.child("status").getValue(String::class.java)
                val callId = snapshot.key ?: return
                val fromUser = snapshot.child("from").getValue(String::class.java) ?: "Unknown"
                Log.d(
                    "CallDebug",
                    "onChildAdded: status=$status, callId=$callId, fromUser=$fromUser"
                )
                if (status == "pending") {
                    showIncomingCallDialog(callId, fromUser)
                }
            }

            override fun onChildChanged(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?,
            ) {
            }

            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(
                snapshot: com.google.firebase.database.DataSnapshot,
                previousChildName: String?,
            ) {
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("CallDebug", "listenForIncomingCalls: onCancelled: ${error.message}")
            }
        }
        incomingCallsRef?.addChildEventListener(incomingCallListener!!)
    }

    // 新增：弹窗显示接受/拒绝
    private fun showIncomingCallDialog(callId: String, fromUser: String) {
        if (!isAdded || context == null) {
            Log.w("CallDebug", "showIncomingCallDialog: Fragment not attached")
            return
        }

        Log.d("CallDebug", "showIncomingCallDialog: callId=$callId, fromUser=$fromUser")

        // 获取通话类型
        val callRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        callRef.addListenerForSingleValueEvent(object :
            com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (isFragmentDestroying || !isAdded || context == null) {
                    Log.w("CallDebug", "showIncomingCallDialog onDataChange: Fragment not attached")
                    return
                }

                val callType = snapshot.child("callType").getValue(String::class.java) ?: "voice"
                val callTypeText = if (callType == "video") "Video Call" else "Voice Call"

                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("Incoming $callTypeText")
                    .setMessage("User $fromUser is calling you.")
                    .setPositiveButton("Accept") { d, _ ->
                        Log.d(
                            "CallDebug",
                            "showIncomingCallDialog: Accept clicked for callId=$callId"
                        )
                        FirebaseDatabase.getInstance().getReference("calls").child(callId)
                            .child("status").setValue("accepted")
                        d.dismiss()
                        val bundle = Bundle().apply { putString("callId", callId) }
                        try {
                            when (callType) {
                                "voice" -> findNavController().navigate(
                                    R.id.action_chatScreenFragment_to_voiceCallFragment,
                                    bundle
                                )

                                "video" -> findNavController().navigate(
                                    R.id.action_chatScreenFragment_to_videoCallFragment,
                                    bundle
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("CallDebug", "Navigation failed: ${e.message}")
                        }
                    }
                    .setNegativeButton("Reject") { d, _ ->
                        Log.d(
                            "CallDebug",
                            "showIncomingCallDialog: Reject clicked for callId=$callId"
                        )
                        FirebaseDatabase.getInstance().getReference("calls").child(callId)
                            .child("status").setValue("rejected")
                        d.dismiss()
                    }
                    .setCancelable(false)
                    .create()
                dialog.show()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("CallDebug", "showIncomingCallDialog: onCancelled: ${error.message}")
            }
        })
    }

    override fun onDestroyView() {
        Log.d("CallDebug", "onDestroyView called")
        isFragmentDestroying = true

        // --- 正确移除监听器 ---
        incomingCallListener?.let { listener ->
            incomingCallsRef?.removeEventListener(listener)
            incomingCallListener = null
            incomingCallsRef = null
        }

        waitingCallListener?.let { listener ->
            waitingCallRef?.removeEventListener(listener)
            waitingCallListener = null
            waitingCallRef = null
        }

        super.onDestroyView()
        chatValueEventListener?.let { chatRef?.removeEventListener(it) }
        chatValueEventListener = null
        chatRef = null
        mediaRecorder?.release()
        mediaRecorder = null
        audioFile?.delete()
        audioFile = null
        _binding = null
    }

    override fun onDestroy() {
        Log.d("CallDebug", "onDestroy called")
        super.onDestroy()
    }
}