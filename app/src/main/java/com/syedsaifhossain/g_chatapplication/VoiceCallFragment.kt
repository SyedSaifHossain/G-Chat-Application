package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVoiceCallBinding
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class VoiceCallFragment : Fragment() {

    private val APP_ID = "01764965ef8f461197b67bb61a51ed30"
    private val CHANNEL_NAME = "gchat_voice_call"

    private var agoraEngine: RtcEngine? = null
    private var isSpeakerOn = true
    private var isMicMuted = false

    private var _binding: FragmentVoiceCallBinding? = null
    private val binding get() = _binding!!

    private val PERMISSION_REQ_ID = 22
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    private lateinit var auth: FirebaseAuth

    // Timer for call duration
    private var callDuration = 0 // seconds
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraJoin", "onUserJoined: remote uid=$uid")
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user offline: $uid", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d("AgoraJoin", "onJoinChannelSuccess: channel=$channel, uid=$uid")
            requireActivity().runOnUiThread {
                Log.d("AgoraVoice", "Joined channel successfully: $channel, uid: $uid")
                Toast.makeText(requireContext(), "Joined channel: $channel", Toast.LENGTH_SHORT).show()
                startCallTimer()
            }
        }


        override fun onError(err: Int) {
            requireActivity().runOnUiThread {
                Log.e("AgoraVoice", "Agora Error: $err")
                val errorMessage = when(err) {
                    Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                    Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                    Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                    Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                    Constants.ERR_NO_PERMISSION -> "No audio recording permission."
                    else -> "Unknown Agora Error: $err"
                }
                Toast.makeText(requireContext(), "Agora Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("VoiceCallFragment", "onCreateView called.")
        _binding = FragmentVoiceCallBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.speakerOn.setOnClickListener { toggleSpeaker() }
        binding.mute.setOnClickListener { toggleMic() }
        binding.endCallButton.setOnClickListener { endCall() }
        binding.voiceCallBackArrow.setOnClickListener { endCall() }
        binding.videoCallAddContact.setOnClickListener {
            Toast.makeText(requireContext(), "Add contact clicked", Toast.LENGTH_SHORT).show()
        }
        binding.videoCall.setOnClickListener {
            Toast.makeText(requireContext(), "This is a voice call. Video feature not active here.", Toast.LENGTH_SHORT).show()
        }

        updateSpeakerButton()
        updateMicButton()

//        if (!checkPermissions()) {
//            Log.d("PermissionDebug", "Permissions not granted, requesting...")
//            requestPermissions()
//        } else {
//            Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
//            fetchTokenAndJoinChannel()
//        }
        requestPermissions()
        fetchTokenAndJoinChannel()

        return view
    }

    private fun checkPermissions(): Boolean {
        Log.d("PermissionDebug", "Checking permissions...")
        for (permission in REQUIRED_PERMISSIONS) {
            val status = ContextCompat.checkSelfPermission(requireContext(), permission)
            Log.d("PermissionDebug", "Permission $permission status: ${if (status == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
            if (status != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        Log.d("PermissionDebug", "All permissions checked and granted.")
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, PERMISSION_REQ_ID)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("PermissionDebug", "onRequestPermissionsResult called. RequestCode: $requestCode")
        if (requestCode == PERMISSION_REQ_ID) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            for (i in permissions.indices) {
                Log.d("PermissionDebug", "Permission: ${permissions[i]}, Granted: ${grantResults[i] == PackageManager.PERMISSION_GRANTED}")
            }
            if (allGranted) {
                Log.d("PermissionDebug", "All requested permissions granted. Fetching token and initializing channel.")
                fetchTokenAndJoinChannel()
            } else {
                Log.e("PermissionDebug", "Not all permissions granted. Cannot start voice call.")
                Toast.makeText(requireContext(), "Permissions not granted. Cannot start voice call.", Toast.LENGTH_LONG).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        } else {
            Log.d("PermissionDebug", "Unknown request code: $requestCode")
        }
    }

    // ----------- Token 获取逻辑（Node.js Token Server） -----------
    private fun fetchTokenAndJoinChannel() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        val agoraUid = Math.abs(currentUser.uid.hashCode())
        val channelName = CHANNEL_NAME
        // 替换为你的 Node.js Token Server 实际 IP
        val url = "http://192.168.68.64:3000/rtcToken?channelName=$channelName&uid=$agoraUid"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to get token: ${e.message}", Toast.LENGTH_LONG).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val token = JSONObject(body ?: "").optString("token")
                if (token.isNotEmpty()) {
                    requireActivity().runOnUiThread {
                        initializeAndJoinChannel(token, agoraUid)
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to get call token from server.", Toast.LENGTH_LONG).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }
    // -------------------------------------------------------------

    private fun initializeAndJoinChannel(token: String, agoraUid: Int) {
        try {
            val config = RtcEngineConfig()
            config.mContext = requireContext().applicationContext
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

            agoraEngine = RtcEngine.create(config)
            Log.d("AgoraInit", "Agora RtcEngine created successfully for voice call.")

            agoraEngine?.enableAudio()
            agoraEngine?.disableVideo()
            agoraEngine?.setEnableSpeakerphone(isSpeakerOn)

            agoraEngine?.joinChannel(token, CHANNEL_NAME, null,agoraUid)
            Log.d("AgoraJoin", "joinChannel: token=$token, channel=$CHANNEL_NAME, uid=$agoraUid")
            Toast.makeText(requireContext(), "Joining voice channel: $CHANNEL_NAME", Toast.LENGTH_SHORT).show()
            Log.d("AgoraInit", "Join voice channel initiated for: $CHANNEL_NAME with token.")

        } catch (e: Exception) {
            Log.e("AgoraInit", "Error initializing Agora for voice call: ${e.message}", e)
            Toast.makeText(requireContext(), "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
        updateSpeakerButton()
        Toast.makeText(requireContext(), "Speaker " + (if (isSpeakerOn) "On" else "Off"), Toast.LENGTH_SHORT).show()
    }

    private fun updateSpeakerButton() {
        _binding?.speakerOn?.let {
            if (isSpeakerOn) {
                it.setImageResource(R.drawable.speakeron)
            } else {
                it.setImageResource(R.drawable.speakeron)
            }
        }
    }

    private fun toggleMic() {
        isMicMuted = !isMicMuted
        agoraEngine?.muteLocalAudioStream(isMicMuted)
        updateMicButton()
        Toast.makeText(requireContext(), "Mic " + (if (isMicMuted) "Muted" else "Unmuted"), Toast.LENGTH_SHORT).show()
    }

    private fun updateMicButton() {
        _binding?.mute?.let {
            if (isMicMuted) {
                it.setImageResource(R.drawable.micoff)
            } else {
                it.setImageResource(R.drawable.micon)
            }
        }
    }

    // ----------- Call Timer Logic -----------
    private fun startCallTimer() {
        stopCallTimer()
        callDuration = 0
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                callDuration++
                val minutes = callDuration / 60
                val seconds = callDuration % 60
                val timeStr = String.format("%d:%02d", minutes, seconds)
                handler.post {
                    _binding?.videoCallTime?.text = timeStr
                }
            }
        }, 1000, 1000)
    }

    private fun stopCallTimer() {
        timer?.cancel()
        timer = null
        callDuration = 0
        handler.post {
            _binding?.videoCallTime?.text = "0:00"
        }
    }
    // ----------------------------------------

    private fun endCall() {
        stopCallTimer()
        agoraEngine?.leaveChannel()
        Toast.makeText(requireContext(), "Voice call ended", Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCallTimer()
        agoraEngine?.leaveChannel()
        RtcEngine.destroy()
        agoraEngine = null
        _binding = null
        Log.d("VoiceCallFragment", "onDestroyView called. Agora engine destroyed.")
    }
}
