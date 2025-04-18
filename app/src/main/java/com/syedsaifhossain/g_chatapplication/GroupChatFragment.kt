package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
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
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts

class GroupChatFragment : Fragment() {

    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var chatRef: DatabaseReference
    private lateinit var adapter: GroupMessageAdapter
    private val groupMessages = mutableListOf<GroupMessage>()

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFilePath: String

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startRecording()
            } else {
                Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()
        chatRef = FirebaseDatabase.getInstance().getReference("group_chats")

        adapter = GroupMessageAdapter(groupMessages, auth.uid.orEmpty())
        binding.groupChatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.groupChatRecyclerView.adapter = adapter

        listenForMessages()

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

        // Mic button for voice message
        binding.micButton.setOnClickListener {
            if (mediaRecorder == null) {
                checkMicPermissionAndRecord()
            } else {
                stopRecording()
            }
        }

        binding.groupchatBackImg.setOnClickListener {
            findNavController().popBackStack()
        }
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
            text = null, // text is null for voice messages
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}