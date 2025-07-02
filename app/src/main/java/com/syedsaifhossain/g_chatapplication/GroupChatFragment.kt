package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
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
import com.syedsaifhossain.g_chatapplication.adapter.GroupMessageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentGroupChatBinding
import com.syedsaifhossain.g_chatapplication.models.GroupMessage
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
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
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.util.concurrent.Executors

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "GroupChatFlow"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var chatRef: DatabaseReference
    private lateinit var adapter: GroupMessageAdapter
    private val groupMessages = mutableListOf<GroupMessage>()

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFilePath: String
    private var isRecording = false
    private var recordStartTime = 0L
    private var recordingTimer: Timer? = null
    private var elapsedTime: Long = 0
    private var lastVoiceDuration: Int = 0
    private var shouldSendVoiceMessage = true
    private var initialX = 0f
    private val CANCEL_X_THRESHOLD = 150f

    // 新增：语音录制相关变量
    private var outputFile: String = ""
    private var isRecordingPhase = false
    private var recordSeconds = 0
    private var recordTimer: Timer? = null
    private var previewPlayer: MediaPlayer? = null
    private var isPreviewPlaying = false
    private var previewTimer: Timer? = null
    private var previewSeconds = 0
    private var recordedFilePath: String? = null

    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var selectedCameraPhotoUri: Uri? = null
    private var audioFile: File? = null

    private lateinit var emojiPopup: EmojiPopup
    private var groupId: String? = null
    private var groupName: String? = null
    private var myName: String? = null
    private var myAvatarUrl: String? = null

    // 权限申请 - 将在 onViewCreated 中初始化
    private lateinit var requestPermissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private lateinit var requestGalleryPermissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private lateinit var requestCameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadFileToFirebase(uri)
            }
        }
    }

    // 统一的图库选择器 (参照私聊模块)
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val mimeType = requireContext().contentResolver.getType(uri) ?: ""
                if (mimeType.startsWith("image")) {
                    uploadImageToFirebase(uri)
                } else if (mimeType.startsWith("video")) {
                    uploadVideoToFirebase(uri)
                } else {
                    Toast.makeText(requireContext(), "Unsupported file type", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 相机启动器
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val photoFile = File(path)
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )
                uploadImageToFirebase(photoUri)
            }
        }
    }

    // 视频录制启动器 (新增)
    private val videoRecorderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "CALLBACK: videoRecorderLauncher received result.")
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null && result.data!!.data != null) {
                val videoUri = result.data!!.data!!
                Log.d(TAG, "CALLBACK: videoRecorderLauncher -> URI: $videoUri")
                uploadVideoToFirebase(videoUri)
            } else {
                Log.e(TAG, "CALLBACK: videoRecorderLauncher -> Video URI is null.")
            }
        } else {
            Log.e(TAG, "CALLBACK: videoRecorderLauncher -> Result is NOT OK (Code: ${result.resultCode})")
        }
    }

    private var groupCallListener: ChildEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Started.")

        // 初始化权限请求器
        initializePermissionLaunchers()

        auth = FirebaseAuth.getInstance()
        groupId = arguments?.getString("groupId")
        Log.d(TAG, "onViewCreated: Group ID is $groupId")

        if (groupId == null) {
            Log.e(TAG, "onViewCreated: Group ID is null, finishing.")
            Toast.makeText(requireContext(), "Group ID missing", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        Log.d(TAG, "onViewCreated: Calling setupGroupInfo.")
        setupGroupInfo()
        Log.d(TAG, "onViewCreated: Calling setupEmoji.")
        setupEmoji()
        Log.d(TAG, "onViewCreated: Calling setupMessageInput.")
        setupMessageInput()
        Log.d(TAG, "onViewCreated: Calling setupButtons.")
        setupButtons()
        Log.d(TAG, "onViewCreated: Calling setupRecyclerView.")
        setupRecyclerView()
        Log.d(TAG, "onViewCreated: Calling listenForMessages.")
        listenForMessages()
        Log.d(TAG, "onViewCreated: Calling requestPermissionsIfNeeded.")
        requestPermissionsIfNeeded()
        Log.d(TAG, "onViewCreated: Calling fetchMyUserInfo.")
        fetchMyUserInfo()
        Log.d(TAG, "onViewCreated: Calling setSoftInputMode.")
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        Log.d(TAG, "onViewCreated: Calling listenForGroupCalls.")
        listenForGroupCalls()
        Log.d(TAG, "onViewCreated: Finished.")
    }

    private fun initializePermissionLaunchers() {
        Log.d(TAG, "PERMISSION: Initializing permission launchers")
        
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "PERMISSION: General permissions result: $permissions")
            if (permissions[Manifest.permission.RECORD_AUDIO] != true) {
                Toast.makeText(requireContext(), "Audio permission required", Toast.LENGTH_SHORT).show()
            }
        }

        requestGalleryPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "PERMISSION: Gallery permissions result received")
            Log.d(TAG, "PERMISSION: Permissions map: $permissions")
            
            // 根据Android版本检查不同的权限
            val allGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 需要所有媒体权限
                permissions.all { it.value }
            } else {
                // Android 12 及以下只需要读取权限
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }
            
            Log.d(TAG, "PERMISSION: All required permissions granted: $allGranted")
            
            if (allGranted) {
                Log.d(TAG, "PERMISSION: All required permissions granted, opening gallery")
                openGallery()
            } else {
                Log.e(TAG, "PERMISSION: Some required permissions were denied")
                val deniedPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissions.filter { !it.value }.keys
                } else {
                    permissions.filter { it.key == Manifest.permission.READ_EXTERNAL_STORAGE && !it.value }.keys
                }
                Log.e(TAG, "PERMISSION: Denied required permissions: $deniedPermissions")
                
                // 检查是否应该显示权限说明
                val shouldShowRationale = deniedPermissions.any { permission ->
                    shouldShowRequestPermissionRationale(permission)
                }
                
                if (shouldShowRationale) {
                    Log.d(TAG, "PERMISSION: Should show rationale")
                    showPermissionRationaleDialog()
                } else {
                    Log.d(TAG, "PERMISSION: Should not show rationale, user permanently denied")
                    Toast.makeText(requireContext(), "Storage permission required to access gallery, please enable it in settings", Toast.LENGTH_LONG).show()
                }
            }
        }

        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d(TAG, "PERMISSION: Camera permission result: $isGranted")
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        
        Log.d(TAG, "PERMISSION: Permission launchers initialized successfully")
    }

    private fun fetchMyUserInfo() {
        val myUid = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(myUid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                myName = snapshot.child("name").getValue(String::class.java)
                myAvatarUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    ?: snapshot.child("avatarUrl").getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupChatFragment", "Failed to load my user info", error.toException())
            }
        })
    }

    private fun setupGroupInfo() {
        val groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId!!)
        groupRef.child("name").get().addOnSuccessListener { snapshot ->
            groupName = snapshot.getValue(String::class.java) ?: "Group Chat"
            binding.groupChatToolbarUserName.text = groupName
        }
    }

    private fun setupEmoji() {
        EmojiManager.install(GoogleEmojiProvider())
        emojiPopup = EmojiPopup.Builder.fromRootView(binding.root)
            .setOnEmojiClickListener { _, emoji ->
                val currentText = binding.messageInput.text.toString()
                binding.messageInput.setText(currentText + emoji.unicode)
                emojiPopup.dismiss()
            }
            .build(binding.messageInput)

        binding.imojiBtn.setOnClickListener {
            if (emojiPopup.isShowing) emojiPopup.dismiss()
            else emojiPopup.toggle()
        }
    }

    private fun setupMessageInput() {
        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendTextMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupButtons() {
        // 返回按钮
        binding.groupchatBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        // 发送按钮
        binding.chatSendButton.setOnClickListener {
            sendTextMessage()
        }

        // 语音按钮
        binding.micButton.setOnClickListener {
            if (!isRecordingPhase) {
                startRecordingWithBubble()
            } else {
                stopRecordingWithBubble()
            }
        }

        // 相机按钮
        binding.groupCameraIcon.setOnClickListener {
            showCameraOptions()
        }

        // 添加按钮
        binding.groupChatAddButton.setOnClickListener { view ->
            showAddOptionsMenu(view)
        }

        // 视频通话按钮
        binding.groupVideoIcon.setOnClickListener {
            initiateGroupVideoCall()
        }

        // 语音通话按钮
        binding.groupCallIcon.setOnClickListener {
            initiateGroupVoiceCall()
        }

        // 更多选项按钮
        binding.groupMoreIcon.setOnClickListener {
            showGroupOptionsMenu()
        }
    }

    private fun setupRecyclerView() {
        adapter = GroupMessageAdapter(groupMessages, auth.uid.orEmpty()) { message, view ->
            showMessageOptionsMenu(message, view)
        }
        binding.groupChatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.groupChatRecyclerView.adapter = adapter
    }

    private fun sendTextMessage() {
        val text = binding.messageInput.text.toString().trim()
        if (text.isNotEmpty()) {
            val messageId = chatRef.push().key!!
            val message = GroupMessage(
                id = messageId,
                senderId = auth.uid,
                text = text,
                timestamp = System.currentTimeMillis(),
                type = "text",
                messageId = messageId,
                senderName = myName,
                senderAvatarUrl = myAvatarUrl
            )
            chatRef.child(messageId).setValue(message)
            binding.messageInput.text?.clear()
        }
    }

    private fun sendVoiceMessage(audioUrl: String, duration: Int) {
        val messageId = chatRef.push().key!!
        val message = GroupMessage(
            id = messageId,
            senderId = auth.uid,
            audioUrl = audioUrl,
            timestamp = System.currentTimeMillis(),
            type = "voice",
            duration = duration,
            messageId = messageId,
            senderName = myName,
            senderAvatarUrl = myAvatarUrl
        )
        chatRef.child(messageId).setValue(message)
    }

    private fun listenForMessages() {
        chatRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId!!).child("messages")
        // Log.d(TAG, "RECEIVE: Setting up listener on: ${chatRef.path}")

        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "RECEIVE: onDataChange triggered. Snapshot has ${snapshot.childrenCount} children.")
                if (!snapshot.exists()) return

                groupMessages.clear()
                for (snap in snapshot.children) {
                    Log.d(TAG, "RECEIVE: Raw message snapshot: ${snap.value}")
                    val msg = snap.getValue(GroupMessage::class.java)
                    Log.d(TAG, "RECEIVE: Parsed message object: $msg")
                    if (msg != null) {
                        val messageWithId = msg.copy(messageId = snap.key ?: "")
                        groupMessages.add(messageWithId)
                    }
                }
                Log.d(TAG, "RECEIVE: Finished processing. Final message list size: ${groupMessages.size}")

                adapter.notifyDataSetChanged()
                if (groupMessages.isNotEmpty()) {
                    binding.groupChatRecyclerView.scrollToPosition(groupMessages.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "RECEIVE: Firebase listener cancelled", error.toException())
                Toast.makeText(requireContext(), "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startRecordingWithBubble() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return
        }
        
        Log.d("VoiceDebug", "startRecordingWithBubble called")
        isRecordingPhase = true
        recordSeconds = 0
        val bubble = binding.voicePreviewLayout.root
        bubble.visibility = View.VISIBLE
        binding.groupchatBottomLayout.visibility = View.GONE
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
                    tvDuration.text = String.format("%d:%02d", recordSeconds / 60, recordSeconds % 60)
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
                                    tvDuration.text = String.format("%d:%02d", recordSeconds / 60, recordSeconds % 60)
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
                                    tvDuration.text = String.format("%d:%02d", previewSeconds / 60, previewSeconds % 60)
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
            binding.groupchatBottomLayout.visibility = View.VISIBLE
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
                binding.groupchatBottomLayout.visibility = View.VISIBLE
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
        Toast.makeText(requireContext(), "stopRecordingWithBubble called", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(requireContext(), "btnPlayPause=$btnPlayPause, btnSend=$btnSend, btnDelete=$btnDelete", Toast.LENGTH_LONG).show()
        tvDuration.text = String.format("%d:%02d", recordSeconds / 60, recordSeconds % 60)
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        btnPlayPause.isEnabled = true
        btnSend.isEnabled = true

        // 试听播放/暂停
        var previewPlayer: MediaPlayer? = null
        var isPreviewPlaying = false
        btnPlayPause.setOnClickListener {
            Toast.makeText(requireContext(), "PlayPause Clicked", Toast.LENGTH_SHORT).show()
            Log.d("VoiceDebug", "PlayPause Clicked")
            if (recordedFilePath.isNullOrEmpty() || !File(recordedFilePath!!).exists()) {
                Toast.makeText(requireContext(), "Audio file does not exist, cannot play", Toast.LENGTH_SHORT).show()
                Log.e("VoiceDebug", "录音文件不存在: $recordedFilePath")
                return@setOnClickListener
            }
            if (isPreviewPlaying) {
                previewPlayer?.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                isPreviewPlaying = false
            } else {
                try {
                    if (previewPlayer == null) {
                        previewPlayer = MediaPlayer().apply {
                            setDataSource(recordedFilePath)
                            prepare()
                            setOnCompletionListener {
                                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                                isPreviewPlaying = false
                                Log.d("VoiceDebug", "播放完成")
                                Toast.makeText(requireContext(), "Playback finished", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    previewPlayer?.start()
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    isPreviewPlaying = true
                    Log.d("VoiceDebug", "开始播放")
                    Toast.makeText(requireContext(), "Playback started", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Playback failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("VoiceDebug", "播放失败: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        // 删除按钮
        btnDelete.setOnClickListener {
            Toast.makeText(requireContext(), "Delete Clicked", Toast.LENGTH_SHORT).show()
            Log.d("VoiceDebug", "Delete Clicked")
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            isPreviewPlaying = false
            binding.voicePreviewLayout.root.visibility = View.GONE
            binding.groupchatBottomLayout.visibility = View.VISIBLE
            recordedFilePath?.let {
                val deleted = File(it).delete()
                Toast.makeText(requireContext(), "Recording file deleted: $deleted", Toast.LENGTH_SHORT).show()
                Log.d("VoiceDebug", "录音文件删除: $deleted, 路径: $it")
            }
        }
        // 录音完成时弹Toast
        Log.d("VoiceDebug", "录音完成，文件: $recordedFilePath, 存在: ${File(recordedFilePath ?: "").exists()}")
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

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VoiceDebug", "Failed to start recording: ${e.message}")
            Toast.makeText(requireContext(), "Failed to start recording: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
                    Log.e("VoiceDebug", "Exception during stop/release for short recording: ${e.message}")
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
            Log.d("VoiceDebug", "Recording finished, file path: $outputFile, file size: ${file.length()} bytes")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VoiceDebug", "Failed to stop recording: ${e.message}")
            mediaRecorder = null
            isRecording = false
            recordingTimer?.cancel()
        }
    }

    // 录音语音消息上传并发送
    private fun sendVoiceMessageWithFirebase(filePath: String, duration: Int) {
        val storageRef = FirebaseStorage.getInstance().reference
        val voiceRef = storageRef.child("group_voices/${groupId}/${UUID.randomUUID()}.3gp")
        val fileUri = Uri.fromFile(File(filePath))
        val uploadTask = voiceRef.putFile(fileUri)
        uploadTask.addOnSuccessListener {
            voiceRef.downloadUrl.addOnSuccessListener { uri ->
                sendVoiceMessage(uri.toString(), duration)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Voice upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCameraOptions() {
        val options = arrayOf("Take Photo", "Record Video")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera()
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openVideoRecorder()
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            }
            .show()
    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        val photoURI = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI)
        cameraLauncher.launch(intent)
    }

    private fun openVideoRecorder() {
        Log.d(TAG, "ACTION: openVideoRecorder called.")
        val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
        videoRecorderLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun openGallery() {
        Log.d(TAG, "GALLERY: Attempting to open gallery")
        
        // 检查权限
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "GALLERY: Android 13+ - Images permission: $hasImages, Videos permission: $hasVideos")
            hasImages && hasVideos
        } else {
            val hasStorage = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "GALLERY: Android 12- - Storage permission: $hasStorage")
            hasStorage
        }
        
        Log.d(TAG, "GALLERY: Has permission: $hasPermission")
        
        if (hasPermission) {
            Log.d(TAG, "GALLERY: Opening gallery with intent")
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            galleryLauncher.launch(Intent.createChooser(intent, "Select Image or Video"))
        } else {
            Log.d(TAG, "GALLERY: No permission, requesting permissions")
            checkAndRequestGalleryPermission()
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("group_images/${groupId}/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    sendImageMessage(downloadUrl.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadVideoToFirebase(videoUri: Uri) {
        Log.d(TAG, "UPLOAD: Starting upload for URI: $videoUri")
        val mimeType = requireContext().contentResolver.getType(videoUri)
        Log.d(TAG, "UPLOAD: MIME type for URI is: $mimeType")

        val storageRef = FirebaseStorage.getInstance().reference
        val videoRef = storageRef.child("group_videos/${groupId}/${System.currentTimeMillis()}.mp4")

        videoRef.putFile(videoUri)
            .addOnSuccessListener {
                videoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d(TAG, "UPLOAD: Success. URL: $downloadUrl")
                    sendVideoMessage(downloadUrl.toString())
                }.addOnFailureListener { e ->
                    Log.e(TAG, "UPLOAD: Failed to get download URL.", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "UPLOAD: Upload failed.", e)
                Toast.makeText(requireContext(), "Video upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadFileToFirebase(fileUri: Uri) {
        val fileName = getFileName(fileUri)
        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("group_files/${groupId}/${System.currentTimeMillis()}_$fileName")

        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    sendFileMessage(downloadUrl.toString(), fileName)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "File upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendImageMessage(imageUrl: String) {
        val messageId = chatRef.push().key!!
        val message = GroupMessage(
            id = messageId,
            senderId = auth.uid,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            type = "image",
            messageId = messageId,
            senderName = myName,
            senderAvatarUrl = myAvatarUrl
        )
        chatRef.child(messageId).setValue(message)
    }

    private fun sendVideoMessage(videoUrl: String) {
        Log.d(TAG, "SEND: Creating message object with URL: $videoUrl")
        val messageId = chatRef.push().key!!
        val message = GroupMessage(
            id = messageId,
            senderId = auth.uid,
            videoUrl = videoUrl,
            timestamp = System.currentTimeMillis(),
            type = "video",
            messageId = messageId,
            senderName = myName,
            senderAvatarUrl = myAvatarUrl
        )
        Log.d(TAG, "SEND: Message object to be sent: $message")
        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d(TAG, "SEND: Message sent to Firebase successfully.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "SEND: Failed to send message.", e)
            }
    }

    private fun sendFileMessage(fileUrl: String, fileName: String) {
        val messageId = chatRef.push().key!!
        val message = GroupMessage(
            id = messageId,
            senderId = auth.uid,
            fileUrl = fileUrl,
            fileName = fileName,
            timestamp = System.currentTimeMillis(),
            type = "file",
            messageId = messageId,
            senderName = myName,
            senderAvatarUrl = myAvatarUrl
        )
        chatRef.child(messageId).setValue(message)
    }

    private fun showAddOptionsMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.NO_GRAVITY, 0, R.style.PopupMenuStyle)
        popupMenu.inflate(R.menu.add_options_menu)

        // 强制显示图标
        try {
            val fields = popupMenu.javaClass.declaredFields
            for (field in fields) {
                if (field.name == "mPopup") {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popupMenu)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.galleryId -> {
                    Log.d(TAG, "MENU: Gallery option clicked")
                    Log.d(TAG, "MENU: Current permissions status:")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val hasImages = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                        val hasVideos = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                        Log.d(TAG, "MENU: Android 13+ - READ_MEDIA_IMAGES: $hasImages, READ_MEDIA_VIDEO: $hasVideos")
                    } else {
                        val hasStorage = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        Log.d(TAG, "MENU: Android 12- - READ_EXTERNAL_STORAGE: $hasStorage")
                    }
                    
                    Log.d(TAG, "MENU: Calling checkAndRequestGalleryPermission")
                    checkAndRequestGalleryPermission()
                    true
                }
                R.id.documentId -> {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "*/*"
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
                    true
                }
                R.id.contactId -> {
                    Toast.makeText(requireContext(), "Contact feature under development", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showMessageOptionsMenu(message: GroupMessage, view: View) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.NO_GRAVITY, 0, R.style.PopupMenuStyle)
        popupMenu.inflate(R.menu.message_options_menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.editMessage -> {
                    showEditMessageDialog(message)
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

    private fun showEditMessageDialog(message: GroupMessage) {
        if (message.senderId != auth.uid) {
            Toast.makeText(requireContext(), "You can only edit your own messages", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = android.widget.EditText(requireContext())
        editText.setText(message.text)
        editText.setSelection(editText.text.length)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    editMessage(message, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editMessage(message: GroupMessage, newText: String) {
        val updates = mapOf(
            "text" to newText,
            "isEdited" to true,
            "editTimestamp" to System.currentTimeMillis()
        )
        chatRef.child(message.messageId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Message edited", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Edit failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteMessage(message: GroupMessage) {
        if (message.senderId != auth.uid) {
            Toast.makeText(requireContext(), "You can only delete your own messages", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                chatRef.child(message.messageId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Message deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyMessageToClipboard(message: GroupMessage) {
        val clipboard = requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Message", message.text ?: "")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showGroupOptionsMenu() {
        val options = arrayOf("Group Info", "Group Members", "Leave Group")
        AlertDialog.Builder(requireContext())
            .setTitle("Group Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showGroupInfo()
                    1 -> showGroupMembers()
                    2 -> leaveGroup()
                }
            }
            .show()
    }

    private fun showGroupInfo() {
        Toast.makeText(requireContext(), "Group Info feature under development", Toast.LENGTH_SHORT).show()
    }

    private fun showGroupMembers() {
        Toast.makeText(requireContext(), "Group Members feature under development", Toast.LENGTH_SHORT).show()
    }

    private fun leaveGroup() {
        AlertDialog.Builder(requireContext())
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Leave") { _, _ ->
                val groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId!!)
                groupRef.child("members").child(auth.uid!!).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Left group", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Leave failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initiateGroupVideoCall() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val groupId = this.groupId ?: return
        val callId = FirebaseDatabase.getInstance().getReference("group_calls").child(groupId).push().key ?: return
        val callRequest = mapOf(
            "initiator" to currentUserId,
            "groupId" to groupId,
            "callType" to "video",
            "status" to "pending",
            "timestamp" to System.currentTimeMillis(),
            "members" to mapOf(currentUserId to "invited")
        )
        FirebaseDatabase.getInstance().getReference("group_calls").child(groupId).child(callId).setValue(callRequest)
        // 自己直接进入通话界面
        val bundle = Bundle().apply { putString("callId", callId); putString("groupId", groupId) }
        findNavController().navigate(R.id.action_groupChatFragment_to_videoCallFragment, bundle)
    }

    private fun initiateGroupVoiceCall() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val groupId = this.groupId ?: return
        val callId = FirebaseDatabase.getInstance().getReference("group_calls").child(groupId).push().key ?: return
        val callRequest = mapOf(
            "initiator" to currentUserId,
            "groupId" to groupId,
            "callType" to "voice",
            "status" to "pending",
            "timestamp" to System.currentTimeMillis(),
            "members" to mapOf(currentUserId to "invited")
        )
        FirebaseDatabase.getInstance().getReference("group_calls").child(groupId).child(callId).setValue(callRequest)
        // 自己直接进入通话界面
        val bundle = Bundle().apply { putString("callId", callId); putString("groupId", groupId) }
        findNavController().navigate(R.id.action_groupChatFragment_to_voiceCallFragment, bundle)
    }

    private fun checkAndRequestGalleryPermission() {
        Log.d(TAG, "PERMISSION: checkAndRequestGalleryPermission called")
        
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用新的媒体权限
            Log.d(TAG, "PERMISSION: Requesting Android 13+ media permissions")
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            // Android 12 及以下只需要读取权限
            Log.d(TAG, "PERMISSION: Requesting Android 12- read storage permission")
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        Log.d(TAG, "PERMISSION: Requesting permissions: ${permissions.joinToString()}")
        Log.d(TAG, "PERMISSION: About to launch permission request")
        
        try {
            requestGalleryPermissionsLauncher.launch(permissions)
            Log.d(TAG, "PERMISSION: Permission request launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "PERMISSION: Failed to launch permission request", e)
            Toast.makeText(requireContext(), "Permission request failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // 根据Android版本请求不同的存储权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用新的媒体权限
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            // Android 12 及以下只需要读取权限
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "file_${System.currentTimeMillis()}"
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("The app needs access to your gallery and video library to send images and videos. Please allow the permissions in the next dialog box.")
            .setPositiveButton("OK") { _, _ ->
                Log.d(TAG, "PERMISSION: User confirmed rationale, requesting permissions again")
                checkAndRequestGalleryPermission()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d(TAG, "PERMISSION: User cancelled rationale dialog")
                Toast.makeText(requireContext(), "Permission denied, cannot access gallery", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun listenForGroupCalls() {
        val groupId = this.groupId ?: return
        val groupCallsRef = FirebaseDatabase.getInstance().getReference("group_calls").child(groupId)
        groupCallListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val callType = snapshot.child("callType").getValue(String::class.java) ?: return
                val status = snapshot.child("status").getValue(String::class.java) ?: return
                val callId = snapshot.key ?: return
                val members = snapshot.child("members").children.map { it.key }
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                if (status == "pending" && members.contains(currentUserId)) {
                    // 弹窗提示加入群聊通话
                    showGroupCallDialog(callId, callType)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        groupCallsRef.addChildEventListener(groupCallListener!!)
    }

    private fun showGroupCallDialog(callId: String, callType: String) {
        if (!isAdded || context == null) return
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (callType == "video") "Group Video Call Invitation" else "Group Voice Call Invitation")
        builder.setMessage("New group call invitation, do you want to join?")
        builder.setPositiveButton("Join") { _, _ ->
            // 标记自己为已加入
            val groupId = this.groupId ?: return@setPositiveButton
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
            val memberRef = FirebaseDatabase.getInstance().getReference("group_calls").child(groupId).child(callId).child("members").child(currentUserId)
            memberRef.setValue("joined")
            // 跳转到通话界面
            val bundle = Bundle().apply { putString("callId", callId); putString("groupId", groupId) }
            if (callType == "video") {
                findNavController().navigate(R.id.action_groupChatFragment_to_videoCallFragment, bundle)
            } else {
                findNavController().navigate(R.id.action_groupChatFragment_to_voiceCallFragment, bundle)
            }
        }
        builder.setNegativeButton("Decline") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(false)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release()
        mediaRecorder = null
        recordTimer?.cancel()
        previewTimer?.cancel()
        previewPlayer?.release()
        previewPlayer = null
        adapter.onDestroy()
        _binding = null
        groupCallListener?.let {
            val groupId = this.groupId ?: return
            FirebaseDatabase.getInstance().getReference("group_calls").child(groupId).removeEventListener(it)
        }
    }
}