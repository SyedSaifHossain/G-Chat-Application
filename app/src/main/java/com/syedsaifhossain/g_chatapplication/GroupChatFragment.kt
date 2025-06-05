package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.syedsaifhossain.g_chatapplication.adapter.GroupMessageAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentGroupChatBinding
import com.syedsaifhossain.g_chatapplication.models.GroupMessage
import java.io.File

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var chatRef: DatabaseReference
    private lateinit var adapter: GroupMessageAdapter
    private val groupMessages = mutableListOf<GroupMessage>()

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFilePath: String

    // Request permission for voice recording
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startRecording()
            } else {
                Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val fileUri = result.data!!.data
            fileUri?.let {
                uploadFileToFirebase(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("GroupChatFragment", "onCreate 执行了")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.d("GroupChatFragment", "onCreateView 执行了")
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        android.util.Log.d("GroupChatFragment", "onViewCreated 执行了")
        auth = FirebaseAuth.getInstance()
        chatRef = FirebaseDatabase.getInstance().getReference("group_chats")

        // 动态设置群聊标题
        val groupId = arguments?.getString("groupId")
        if (groupId != null) {
            val groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId)
            groupRef.child("name").get().addOnSuccessListener { snapshot ->
                val groupName = snapshot.getValue(String::class.java)
                if (!groupName.isNullOrEmpty()) {
                    binding.groupchatTxt.text = groupName
                }
            }
        }

        adapter = GroupMessageAdapter(groupMessages, auth.uid.orEmpty())
        binding.groupChatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.groupChatRecyclerView.adapter = adapter

        listenForMessages()

        // Send text message
        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // Voice message
        binding.micButton.setOnClickListener {
            if (mediaRecorder == null) {
                checkMicPermissionAndRecord()
            } else {
                stopRecording()
            }
        }

        // File attachment
        binding.attachButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
        }

        // Back button
        binding.groupchatBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        // 设置群聊默认头像并加日志
        android.util.Log.d("GroupChatFragment", "设置群聊头像")
        binding.groupchatAvatar.setImageResource(R.drawable.addcontacticon)
    }

    private fun sendMessage() {
        val text = binding.messageInput.text.toString().trim()
        if (text.isNotEmpty()) {
            val messageId = chatRef.push().key!!
            val message = GroupMessage(
                id = messageId,
                senderId = auth.uid,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            chatRef.child(messageId).setValue(message)
            binding.messageInput.text?.clear()
        }
    }

    private fun listenForMessages() {
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupMessages.clear()
                for (snap in snapshot.children) {
                    val msg = snap.getValue(GroupMessage::class.java)
                    msg?.let { groupMessages.add(it) }
                }
                adapter.notifyDataSetChanged()
                binding.groupChatRecyclerView.scrollToPosition(groupMessages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkMicPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        val audioFile = File(requireContext().cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        audioFilePath = audioFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            prepare()
            start()
        }

        Toast.makeText(context, "Recording started...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()

        sendVoiceMessage(audioFilePath)
    }

    private fun sendVoiceMessage(filePath: String) {
        val file = File(filePath)
        val fileUri = Uri.fromFile(file)
        val storageRef = FirebaseStorage.getInstance().reference
            .child("voiceMessages/${file.name}")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    sendVoiceMessageToDatabase(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendVoiceMessageToDatabase(audioUrl: String) {
        val messageId = chatRef.push().key!!
        val message = GroupMessage(
            id = messageId,
            senderId = auth.uid,
            text = null,
            audioUrl = audioUrl,
            timestamp = System.currentTimeMillis()
        )
        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Toast.makeText(context, "Voice message sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadFileToFirebase(uri: Uri) {
        val fileName = getFileName(uri)
        val storageRef = FirebaseStorage.getInstance().reference
            .child("attachments/$fileName")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    sendAttachmentMessage(downloadUrl.toString(), fileName)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
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

    private fun sendAttachmentMessage(fileUrl: String, fileName: String) {
        val messageId = chatRef.push().key!!
        val message = GroupMessage(
            id = messageId,
            senderId = auth.uid,
            text = null,
            fileUrl = fileUrl,
            fileName = fileName,
            timestamp = System.currentTimeMillis()
        )
        chatRef.child(messageId).setValue(message)
            .addOnSuccessListener {
                Toast.makeText(context, "File sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to send file", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}