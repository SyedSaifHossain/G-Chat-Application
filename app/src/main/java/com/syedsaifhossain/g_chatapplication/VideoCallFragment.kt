package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class VideoCallFragment : Fragment() {

    private val APP_ID = "66157d3652d548ba86cc9c6075a69274"
    private val CHANNEL_NAME = "gchat_video_call"

    private var agoraEngine: RtcEngine? = null
    private var isSpeakerOn = true
    private var isMicMuted = false
    private var isCameraFront = true

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!

    private val PERMISSION_REQ_ID = 22

    private lateinit var auth: FirebaseAuth

    // Timer related variables for call duration
    private var callDuration = 0 // seconds
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraDebug", "onUserJoined: $uid, elapsed: $elapsed")
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("AgoraDebug", "onUserOffline: $uid, reason: $reason")
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user offline: $uid", Toast.LENGTH_SHORT).show()
                _binding?.remoteVideoViewContainer?.removeAllViews()
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d("AgoraDebug", "onJoinChannelSuccess: channel=$channel, uid=$uid, elapsed=$elapsed")
            requireActivity().runOnUiThread {
                Log.d("Agora", "Joined channel successfully: $channel, uid: $uid")
                Toast.makeText(requireContext(), "Joined channel: $channel", Toast.LENGTH_SHORT).show()
                startCallTimer() // Start timer when joined
            }
        }

        override fun onFirstLocalVideoFrame(
            source: Constants.VideoSourceType,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            Log.d("AgoraDebug", "onFirstLocalVideoFrame: ${width}x${height}, elapsed: $elapsed")
        }

        override fun onFirstRemoteVideoDecoded(
            uid: Int,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            Log.d("AgoraDebug", "onFirstRemoteVideoDecoded: uid=$uid, ${width}x${height}, elapsed: $elapsed")
        }

        override fun onError(err: Int) {
            Log.e("AgoraDebug", "onError: $err")
            requireActivity().runOnUiThread {
                Log.e("Agora", "Agora Error: $err")
                val errorMessage = when(err) {
                    Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                    Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                    Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                    Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                    Constants.ERR_NO_PERMISSION -> "No video or audio recording permission."
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
        Log.d("VideoCallFragment", "onCreateView called.")
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.speakerOn.setOnClickListener { toggleSpeaker() }
        binding.switchCamera.setOnClickListener { switchCamera() }
        binding.mute.setOnClickListener { toggleMic() }
        binding.endCallButton.setOnClickListener { endCall() }

        binding.videoCallBackArrow.setOnClickListener {
            Toast.makeText(requireContext(), "Back arrow clicked", Toast.LENGTH_SHORT).show()
            endCall()
        }

        binding.videoCallAddContact.setOnClickListener {
            Toast.makeText(requireContext(), "Add contact clicked", Toast.LENGTH_SHORT).show()
        }

        updateSpeakerButton()
        updateMicButton()
//        requestPermissions()
//        fetchTokenAndJoinChannel()


        if (!checkPermissions()) {
            Log.d("PermissionDebug", "Permissions not granted, requesting...")
            requestPermissions()
        } else {
            Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
            fetchTokenAndJoinChannel()
        }

        return view
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), getRequiredPermissions(), PERMISSION_REQ_ID)
    }

    private fun checkPermissions(): Boolean {
        Log.d("PermissionDebug", "Checking permissions...")
        for (permission in getRequiredPermissions()) {
            val status = ContextCompat.checkSelfPermission(requireContext(), permission)
            Log.d("PermissionDebug", "Permission $permission status: ${if (status == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
            if (status != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        Log.d("PermissionDebug", "All permissions checked and granted.")
        return true
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
                Log.e("PermissionDebug", "Not all permissions granted. Cannot start video call.")
                Toast.makeText(requireContext(), "Permissions not granted. Cannot start video call.", Toast.LENGTH_LONG).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        } else {
            Log.d("PermissionDebug", "Unknown request code: $requestCode")
        }
    }

    // Use OkHttp to get token and join channel
    private fun fetchTokenAndJoinChannel() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("AgoraToken", "No user authenticated. Attempting anonymous sign-in...")
            if (!isAdded) {
                Log.d("VideoCall", "Fragment not attached, ignoring anonymous sign-in")
                return
            }
            auth.signInAnonymously()
                .addOnCompleteListener(requireActivity()) { task ->
                    if (!isAdded) {
                        Log.d("VideoCall", "Fragment not attached, ignoring sign-in result")
                        return@addOnCompleteListener
                    }
                    if (task.isSuccessful) {
                        Log.d("AgoraToken", "Anonymous sign-in successful. Retrying token fetch.")
                        fetchTokenAndJoinChannel()
                    } else {
                        Log.e("AgoraToken", "Anonymous sign-in failed: ${task.exception?.message}", task.exception)
                        Toast.makeText(requireContext(), "Authentication required to start call.", Toast.LENGTH_LONG).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            return
        }

        val agoraUid = Math.abs(currentUser.uid.hashCode())
        val channelName = CHANNEL_NAME
        val url = "https://agora-token-service-oajn.onrender.com/rtc/$channelName/publisher/uid/$agoraUid/"
        
        Log.d("VideoCall", "Requesting token with parameters:")
        Log.d("VideoCall", "URL: $url")
        Log.d("VideoCall", "Channel Name: $channelName")
        Log.d("VideoCall", "User ID: ${currentUser.uid}")
        Log.d("VideoCall", "Agora UID: $agoraUid")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isAdded) {
                    Log.d("VideoCall", "Fragment not attached, ignoring network failure")
                    return
                }
                Log.e("VideoCall", "Network request failed: ${e.message}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to get token: ${e.message}", Toast.LENGTH_LONG).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded) {
                    Log.d("VideoCall", "Fragment not attached, ignoring network response")
                    return
                }

                Log.d("VideoCall", "Server response code: ${response.code}")
                Log.d("VideoCall", "Server response message: ${response.message}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("VideoCall", "Server returned error: ${response.code}")
                    Log.e("VideoCall", "Error response body: $errorBody")
                    requireActivity().runOnUiThread {
                        when (response.code) {
                            404 -> Toast.makeText(requireContext(), "服务器暂时不可用，请稍后再试", Toast.LENGTH_LONG).show()
                            else -> Toast.makeText(requireContext(), "服务器错误: ${response.code}", Toast.LENGTH_LONG).show()
                        }
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    return
                }

                try {
                    val body = response.body?.string()
                    Log.d("VideoCall", "Server response body: $body")
                    
                    if (body.isNullOrEmpty()) {
                        Log.e("VideoCall", "Empty response from server")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Empty response from server", Toast.LENGTH_LONG).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        return
                    }

                    val token = JSONObject(body).optString("rtcToken")
                    if (token.isNotEmpty()) {
                        Log.d("VideoCall", "Successfully obtained token")
                        requireActivity().runOnUiThread {
                            initializeAndJoinChannel(token, agoraUid)
                        }
                    } else {
                        Log.e("VideoCall", "No token in response: $body")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Failed to get call token from server.", Toast.LENGTH_LONG).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VideoCall", "Error parsing response: ${e.message}")
                    Log.e("VideoCall", "Response body that caused error: ${response.body?.string()}")
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Error parsing server response", Toast.LENGTH_LONG).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    // Initialize and join channel, pass agoraUid
    private fun initializeAndJoinChannel(token: String, agoraUid: Int) {
        try {
            val config = RtcEngineConfig()
            config.mContext = requireContext().applicationContext
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

            agoraEngine = RtcEngine.create(config)
            Log.d("AgoraInit", "Agora RtcEngine created successfully.")

            // 在后台线程中初始化视频
            Thread {
                try {
                    agoraEngine?.enableVideo()
                    agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
                    agoraEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

                    requireActivity().runOnUiThread {
                        setupLocalVideo()
                        Log.d("AgoraDebug", "准备 joinChannel: token=$token, channel=$CHANNEL_NAME, uid=$agoraUid")
                        agoraEngine?.joinChannel(token, CHANNEL_NAME, null, agoraUid)
                        Log.d("AgoraDebug", "joinChannel 已调用")
                        Toast.makeText(requireContext(), "Joining channel: $CHANNEL_NAME", Toast.LENGTH_SHORT).show()
                        Log.d("AgoraInit", "Join channel initiated for: $CHANNEL_NAME with token.")
                    }
                } catch (e: Exception) {
                    Log.e("AgoraInit", "Error in video initialization: ", e)
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Error initializing video: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e("AgoraInit", "Error initializing Agora: ", e)
            Toast.makeText(requireContext(), "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupLocalVideo() {
        _binding?.localVideoViewContainer?.let { container ->
            val surfaceView = SurfaceView(requireContext())
            container.addView(surfaceView)
            agoraEngine?.setupLocalVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0))
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        _binding?.remoteVideoViewContainer?.let { container ->
            container.removeAllViews()
            val surfaceView = SurfaceView(requireContext())
            container.addView(surfaceView)
            agoraEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid))
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

    private fun switchCamera() {
        agoraEngine?.switchCamera()
        isCameraFront = !isCameraFront
        Toast.makeText(requireContext(), "Switched camera", Toast.LENGTH_SHORT).show()
    }

    private fun endCall() {
        try {
            Log.d("VideoCall", "开始结束通话...")
            
            // 1. 停止计时器
            Log.d("VideoCall", "停止计时器")
            stopCallTimer()
            
            // 2. 离开频道
            Log.d("VideoCall", "准备离开频道")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VideoCall", "已离开频道")
            } catch (e: Exception) {
                Log.e("VideoCall", "离开频道时出错: ${e.message}")
            }
            
            // 3. 清理视频视图
            Log.d("VideoCall", "清理视频视图")
            try {
                _binding?.localVideoViewContainer?.removeAllViews()
                _binding?.remoteVideoViewContainer?.removeAllViews()
                Log.d("VideoCall", "视频视图已清理")
            } catch (e: Exception) {
                Log.e("VideoCall", "清理视频视图时出错: ${e.message}")
            }
            
            // 4. 销毁引擎
            Log.d("VideoCall", "准备销毁引擎")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VideoCall", "引擎已销毁")
            } catch (e: Exception) {
                Log.e("VideoCall", "销毁引擎时出错: ${e.message}")
            }
            
            // 5. 显示提示
            if (isAdded) {
                try {
                    Toast.makeText(requireContext(), "Call ended", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("VideoCall", "显示Toast时出错: ${e.message}")
                }
            }
            
            // 6. 返回
            Log.d("VideoCall", "准备返回")
            if (isAdded) {
                try {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    Log.d("VideoCall", "已触发返回")
                } catch (e: Exception) {
                    Log.e("VideoCall", "返回时出错: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e("VideoCall", "结束通话时发生错误: ${e.message}")
            e.printStackTrace()
            if (isAdded) {
                try {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                } catch (e2: Exception) {
                    Log.e("VideoCall", "错误处理时返回失败: ${e2.message}")
                }
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
                    _binding?.callTimerText?.text = timeStr
                }
            }
        }, 1000, 1000)
    }

    private fun stopCallTimer() {
        timer?.cancel()
        timer = null
        callDuration = 0
        handler.post {
            _binding?.callTimerText?.text = "0:00"
        }
    }
    // ----------------------------------------

    override fun onDestroyView() {
        Log.d("VideoCall", "onDestroyView 开始")
        try {
            // 1. 停止计时器
            Log.d("VideoCall", "停止计时器")
            stopCallTimer()
            
            // 2. 离开频道
            Log.d("VideoCall", "准备离开频道")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VideoCall", "已离开频道")
            } catch (e: Exception) {
                Log.e("VideoCall", "离开频道时出错: ${e.message}")
            }
            
            // 3. 销毁引擎
            Log.d("VideoCall", "准备销毁引擎")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VideoCall", "引擎已销毁")
            } catch (e: Exception) {
                Log.e("VideoCall", "销毁引擎时出错: ${e.message}")
            }
            
            // 4. 清理绑定
            Log.d("VideoCall", "清理绑定")
            _binding = null
            
        } catch (e: Exception) {
            Log.e("VideoCall", "onDestroyView 发生错误: ${e.message}")
            e.printStackTrace()
        }
        Log.d("VideoCall", "onDestroyView 结束")
        super.onDestroyView()
    }
}