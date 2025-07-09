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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import androidx.navigation.fragment.findNavController
import android.app.AlertDialog
import com.google.firebase.database.DatabaseReference
import androidx.navigation.NavController

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

    private var callId: String? = null
    private var waitingDialog: AlertDialog? = null

    private var isTimerStarted = false

    // Firebase监听器引用
    private var callStatusListener: ValueEventListener? = null
    private var callStatusRef: DatabaseReference? = null
    private var waitingStatusRef: DatabaseReference? = null
    private var waitingStatusListener: ValueEventListener? = null

    // NavController安全引用
    private var safeNavController: NavController? = null
    private var navController: NavController? = null

    // 添加一个标志来跟踪Fragment是否正在销毁
    private var isFragmentDestroying = false

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraDebug", "onUserJoined: $uid, elapsed: $elapsed")
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
                        }
                        setupRemoteVideo(uid)
                    }
                }
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("AgoraDebug", "onUserOffline: $uid, reason: $reason")
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Remote user offline: $uid", Toast.LENGTH_SHORT).show()
                        }
                        _binding?.remoteVideoViewContainer?.removeAllViews()
                    }
                }
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d("AgoraJoin", "onJoinChannelSuccess: channel=$channel, uid=$uid")
            // Reset retry count on successful join
            channelJoinRetryCount = 0
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Log.d("AgoraVideo", "Joined channel successfully: $channel, uid: $uid")
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
                        }
                        startCallTimer()
                        // Setup local video after successful join
                        setupLocalVideo()
                    }
                }
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
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        startCallTimerIfNeeded()
                    }
                }
            }
        }

        override fun onError(err: Int) {
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Log.e("AgoraVideo", "Agora Error: $err")
                        val errorMessage = when(err) {
                            Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                            Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                            Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                            Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                            Constants.ERR_NO_PERMISSION -> "No audio/video recording permission."
                            else -> "Unknown Agora Error: $err"
                        }
                        
                        // Retry channel join for certain errors
                        if (err == Constants.ERR_JOIN_CHANNEL_REJECTED || err == Constants.ERR_INVALID_TOKEN) {
                            if (channelJoinRetryCount < maxChannelJoinRetries - 1) {
                                channelJoinRetryCount++
                                Log.d("VideoCallDebug", "Retrying channel join (${channelJoinRetryCount + 1}/$maxChannelJoinRetries)")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Retrying channel join...", Toast.LENGTH_SHORT).show()
                                }
                                // Retry after a short delay
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isAdded && !isFragmentDestroying) {
                                        fetchTokenAndJoinChannel()
                                    }
                                }, 2000)
                            } else {
                                Log.e("VideoCallDebug", "Max channel join retries reached")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Failed to join channel after $maxChannelJoinRetries attempts", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Agora Error: $errorMessage", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
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
            context?.let { ctx ->
                Toast.makeText(ctx, "Back arrow clicked", Toast.LENGTH_SHORT).show()
            }
            endCall()
        }

        binding.videoCallAddContact.setOnClickListener {
            context?.let { ctx ->
                Toast.makeText(ctx, "Add contact clicked", Toast.LENGTH_SHORT).show()
            }
        }

        updateSpeakerButton()
        updateMicButton()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Reset destruction flag
        isFragmentDestroying = false
        
        // Safely get NavController
        safeNavController = try { 
            if (isAdded && parentFragmentManager.isStateSaved.not()) {
                findNavController() 
            } else {
                null
            }
        } catch (e: Exception) { 
            Log.e("VideoCall", "Failed to get NavController: ${e.message}")
            null 
        }
        
        navController = try {
            if (isAdded && parentFragmentManager.isStateSaved.not()) {
                findNavController()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("VideoCall", "Failed to get NavController: "+e.message)
            null
        }
        
        callId = arguments?.getString("callId")
        Log.d("VideoCallDebug", "onViewCreated: callId=$callId")
        
        // Start initialization process immediately
        if (callId != null) {
            listenCallStatus(callId!!)
            showWaitingIfPending(callId!!)
            
            // Test token server connection first
            testTokenServer()
            
            // Start permission check and initialization immediately
            Log.d("VideoCallDebug", "Starting permission check and initialization process")
            if (!checkPermissions()) {
                Log.d("PermissionDebug", "Permissions not granted, requesting...")
                requestPermissions()
            } else {
                Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
                fetchTokenAndJoinChannel()
            }
        } else {
            Log.e("VideoCallDebug", "callId is null, cannot proceed")
        }
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
        Log.d("VideoCallDebug", "Requesting permissions")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                getRequiredPermissions(),
                PERMISSION_REQ_ID
            )
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d("VideoCallDebug", "Checking permissions...")
        for (permission in getRequiredPermissions()) {
            context?.let { ctx ->
                val status = ContextCompat.checkSelfPermission(ctx, permission)
                Log.d("VideoCallDebug", "Permission $permission status: ${if (status == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
                if (status != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            } ?: return false
        }
        Log.d("VideoCallDebug", "All permissions granted")
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("VideoCallDebug", "Permission request result: requestCode=$requestCode")
        if (requestCode == PERMISSION_REQ_ID) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            for (i in permissions.indices) {
                Log.d("VideoCallDebug", "Permission: ${permissions[i]}, Granted: ${grantResults[i] == PackageManager.PERMISSION_GRANTED}")
            }
            if (allGranted) {
                Log.d("VideoCallDebug", "All permissions granted, starting token fetch and channel initialization")
                fetchTokenAndJoinChannel()
            } else {
                Log.e("VideoCallDebug", "Not all permissions granted, cannot start video call")
                context?.let { ctx ->
                    Toast.makeText(ctx, "Permissions not granted. Cannot start video call.", Toast.LENGTH_LONG).show()
                }
                if (isAdded && activity != null) {
                    try {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } catch (e: Exception) {
                        Log.e("VideoCall", "Failed to go back when permissions denied: ${e.message}")
                    }
                }
            }
        } else {
            Log.d("VideoCallDebug", "Unknown request code: $requestCode")
        }
    }

    // ----------- Token 获取逻辑（Node.js Token Server） -----------
    private var tokenRetryCount = 0
    private val maxTokenRetries = 3
    
    private fun fetchTokenAndJoinChannel() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("VideoCallDebug", "User not authenticated")
            if (isAdded) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
                }
                if (isAdded && activity != null) {
                    try {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } catch (e: Exception) {
                        Log.e("VideoCall", "Failed to go back when user not authenticated: ${e.message}")
                    }
                }
            }
            return
        }

        val agoraUid = Math.abs(currentUser.uid.hashCode())
        // Use a fixed channel name for testing, or use callId if available
        val channelName = if (!callId.isNullOrEmpty()) {
            "channel_$callId"
        } else {
            CHANNEL_NAME
        }
        val url = "https://agora-token-service-oajn.onrender.com/rtc/$channelName/publisher/uid/$agoraUid/"
        
        Log.d("VideoCallDebug", "Starting token request (attempt ${tokenRetryCount + 1}/$maxTokenRetries): channelName=$channelName, uid=$agoraUid, callId=$callId")
        Log.d("VideoCallDebug", "Request URL: $url")

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VideoCallDebug", "Failed to get token (attempt ${tokenRetryCount + 1}): ${e.message}")
                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            if (tokenRetryCount < maxTokenRetries - 1) {
                                tokenRetryCount++
                                Log.d("VideoCallDebug", "Retrying token request (${tokenRetryCount + 1}/$maxTokenRetries)")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Retrying token request...", Toast.LENGTH_SHORT).show()
                                }
                                // Retry after a short delay
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isAdded && !isFragmentDestroying) {
                                        fetchTokenAndJoinChannel()
                                    }
                                }, 2000)
                            } else {
                                Log.e("VideoCallDebug", "Max token retries reached")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Failed to get token after $maxTokenRetries attempts", Toast.LENGTH_LONG).show()
                                }
                                if (isAdded && activity != null) {
                                    try {
                                        activity?.onBackPressedDispatcher?.onBackPressed()
                                    } catch (e2: Exception) {
                                        Log.e("VideoCall", "Failed to go back when token request failed: ${e2.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("VideoCallDebug", "Token response code: ${response.code}")
                Log.d("VideoCallDebug", "Token response body: $responseBody")

                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            when (response.code) {
                                200 -> {
                                    if (!responseBody.isNullOrEmpty()) {
                                        try {
                                            val jsonResponse = JSONObject(responseBody)
                                            val token = jsonResponse.getString("rtcToken")
                                            Log.d("VideoCallDebug", "Token received successfully: $token")
                                            Log.d("VideoCallDebug", "Token length: ${token.length}")
                                            Log.d("VideoCallDebug", "Starting Agora engine initialization")
                                            // Reset retry count on success
                                            tokenRetryCount = 0
                                            initializeAndJoinChannel(token, agoraUid)
                                        } catch (e: Exception) {
                                            Log.e("VideoCallDebug", "Error parsing server response", e)
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "Error parsing server response", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Log.e("VideoCallDebug", "Empty response from server")
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Empty response from server", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                404 -> {
                                    Log.e("VideoCallDebug", "Server temporarily unavailable")
                                    context?.let { ctx ->
                                        Toast.makeText(ctx, "Server temporarily unavailable, please try again later", Toast.LENGTH_LONG).show()
                                    }
                                }
                                else -> {
                                    Log.e("VideoCallDebug", "Server error: ${response.code}")
                                    context?.let { ctx ->
                                        Toast.makeText(ctx, "Server error: ${response.code}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    // Initialize and join channel, pass agoraUid
    private var channelJoinRetryCount = 0
    private val maxChannelJoinRetries = 3
    
    private fun initializeAndJoinChannel(token: String, agoraUid: Int) {
        try {
            Log.d("VideoCallDebug", "Starting Agora engine initialization: uid=$agoraUid (attempt ${channelJoinRetryCount + 1}/$maxChannelJoinRetries)")
            
            val config = RtcEngineConfig()
            config.mContext = context?.applicationContext
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

            agoraEngine = RtcEngine.create(config)
            Log.d("VideoCallDebug", "Agora RtcEngine created successfully for video call")

            // Initialize basic settings immediately on main thread
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        try {
                            Log.d("VideoCallDebug", "Configuring Agora engine for video")
                            agoraEngine?.enableVideo()
                            agoraEngine?.enableAudio()
                            agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
                            
                            // Use the same channel name as in token request
                            val channelName = if (!callId.isNullOrEmpty()) {
                                "channel_$callId"
                            } else {
                                CHANNEL_NAME
                            }
                            
                            Log.d("VideoCallDebug", "Preparing to join video channel: $channelName")
                            agoraEngine?.joinChannel(token, channelName, null, agoraUid)
                            Log.d("VideoCallDebug", "joinChannel called: channel=$channelName, uid=$agoraUid")
                            
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Joining video channel: $channelName", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("VideoCallDebug", "Error initializing Agora engine: ${e.message}")
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("VideoCallDebug", "Failed to create Agora engine: ${e.message}")
            context?.let { ctx ->
                Toast.makeText(ctx, "Error creating Agora engine: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupLocalVideo() {
        _binding?.localVideoViewContainer?.let { container ->
            container.removeAllViews()
            context?.let { ctx ->
                val surfaceView = SurfaceView(ctx)
                surfaceView.setZOrderMediaOverlay(true) // 让本地视频悬浮
                container.addView(surfaceView)
                agoraEngine?.setupLocalVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0))
                Log.d("VideoDebug", "本地视频已添加到local_video_view_container")
            }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        _binding?.remoteVideoViewContainer?.let { container ->
            container.removeAllViews()
            context?.let { ctx ->
                val surfaceView = SurfaceView(ctx)
                container.addView(surfaceView)
                agoraEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid))
                Log.d("VideoDebug", "远程视频已添加到remote_video_view_container, uid=$uid")
            }
        }
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
        updateSpeakerButton()
        context?.let { ctx ->
            Toast.makeText(ctx, "Speaker " + (if (isSpeakerOn) "On" else "Off"), Toast.LENGTH_SHORT).show()
        }
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
        context?.let { ctx ->
            Toast.makeText(ctx, "Mic " + (if (isMicMuted) "Muted" else "Unmuted"), Toast.LENGTH_SHORT).show()
        }
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
        context?.let { ctx ->
            Toast.makeText(ctx, "Switched camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun endCall() {
        try {
            if (callId != null) {
                FirebaseDatabase.getInstance().getReference("calls").child(callId!!).child("status").setValue("ended")
            }
            Log.d("VideoCall", "Starting to end call...")
            
            // 1. Stop timer
            Log.d("VideoCall", "Stopping timer")
            stopCallTimer()
            
            // 2. Dismiss waiting dialog
            Log.d("VideoCall", "Dismissing waiting dialog")
            try {
                waitingDialog?.dismiss()
                waitingDialog = null
            } catch (e: Exception) {
                Log.e("VideoCall", "Error dismissing waiting dialog: ${e.message}")
            }
            
            // 3. Leave channel
            Log.d("VideoCall", "Preparing to leave channel")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VideoCall", "Left channel successfully")
            } catch (e: Exception) {
                Log.e("VideoCall", "Error leaving channel: ${e.message}")
            }
            
            // 4. Clean up video views
            Log.d("VideoCall", "Cleaning up video views")
            try {
                _binding?.localVideoViewContainer?.removeAllViews()
                _binding?.remoteVideoViewContainer?.removeAllViews()
                Log.d("VideoCall", "Video views cleaned up")
            } catch (e: Exception) {
                Log.e("VideoCall", "Error cleaning up video views: ${e.message}")
            }
            
            // 5. Destroy engine
            Log.d("VideoCall", "Preparing to destroy engine")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VideoCall", "Engine destroyed")
            } catch (e: Exception) {
                Log.e("VideoCall", "Error destroying engine: ${e.message}")
            }
            
            // 6. Show toast
            if (isAdded) {
                try {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Video call ended", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("VideoCall", "Error showing toast: ${e.message}")
                }
            }
            
            // 7. Return
            Log.d("VideoCall", "Preparing to return")
            if (isAdded) {
                try {
                    safePopBackStack()
                    Log.d("VideoCall", "Return triggered")
                } catch (e: Exception) {
                    Log.e("VideoCall", "Error returning: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e("VideoCall", "Error ending call: ${e.message}")
            e.printStackTrace()
            if (isAdded) {
                try {
                    safePopBackStack()
                } catch (e2: Exception) {
                    Log.e("VideoCall", "Error handling return on failure: ${e2.message}")
                }
            }
        }
    }

    private fun startCallTimerIfNeeded() {
        if (!isTimerStarted) {
            startCallTimer()
            isTimerStarted = true
        }
    }

    // ----------- Call Timer Logic -----------
    private fun startCallTimer() {
        try {
            stopCallTimer()
            callDuration = 0
            timer = Timer()
            timer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    callDuration++
                    val minutes = callDuration / 60
                    val seconds = callDuration % 60
                    val timeStr = String.format("%d:%02d", minutes, seconds)
                    if (isAdded && context != null) {
                        handler.post {
                            if (isAdded && context != null) {
                                _binding?.callTimerText?.text = timeStr
                            }
                        }
                    }
                }
            }, 1000, 1000)
        } catch (e: Exception) {
            Log.e("VideoCall", "Error starting timer: ${e.message}")
        }
    }

    private fun stopCallTimer() {
        try {
            timer?.cancel()
            timer = null
            callDuration = 0
            isTimerStarted = false
            if (isAdded && context != null) {
                handler.post {
                    if (isAdded && context != null) {
                        _binding?.callTimerText?.text = "0:00"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VideoCall", "Error stopping timer: ${e.message}")
        }
    }
    // ----------------------------------------

    override fun onDestroyView() {
        Log.d("VideoCall", "onDestroyView started")
        
        // Set destruction flag
        isFragmentDestroying = true
        
        try {
            // 1. Stop timer
            Log.d("VideoCall", "Stopping timer")
            stopCallTimer()
            
            // 2. Dismiss waiting dialog
            Log.d("VideoCall", "Dismissing waiting dialog")
            try {
                waitingDialog?.dismiss()
                waitingDialog = null
            } catch (e: Exception) {
                Log.e("VideoCall", "Error dismissing waiting dialog: ${e.message}")
            }
            
            // 3. Leave channel
            Log.d("VideoCall", "Preparing to leave channel")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VideoCall", "Left channel successfully")
            } catch (e: Exception) {
                Log.e("VideoCall", "Error leaving channel: ${e.message}")
            }
            
            // 4. Clean up video views
            Log.d("VideoCall", "Cleaning up video views")
            try {
                _binding?.localVideoViewContainer?.removeAllViews()
                _binding?.remoteVideoViewContainer?.removeAllViews()
                Log.d("VideoCall", "Video views cleaned up")
            } catch (e: Exception) {
                Log.e("VideoCall", "Error cleaning up video views: ${e.message}")
            }
            
            // 5. Destroy engine
            Log.d("VideoCall", "Preparing to destroy engine")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VideoCall", "Engine destroyed")
            } catch (e: Exception) {
                Log.e("VideoCall", "Error destroying engine: ${e.message}")
            }
            
            // 6. Safely remove listeners
            callStatusListener?.let { listener ->
                callStatusRef?.removeEventListener(listener)
            }
            callStatusListener = null
            callStatusRef = null
            
            // 7. Remove waiting status listener
            waitingStatusListener?.let { listener ->
                waitingStatusRef?.removeEventListener(listener)
            }
            waitingStatusListener = null
            waitingStatusRef = null
            
            safeNavController = null
            
        } catch (e: Exception) {
            Log.e("VideoCall", "onDestroyView error: ${e.message}")
            e.printStackTrace()
        }
        Log.d("VideoCall", "onDestroyView ended")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d("VideoCall", "onDestroy called")
        try {
            // Ensure all resources are cleaned up when Fragment is fully destroyed
            stopCallTimer()
            waitingDialog?.dismiss()
            agoraEngine?.leaveChannel()
            RtcEngine.destroy()
            agoraEngine = null
            
            // Remove Firebase listeners
            callStatusListener?.let { listener ->
                callStatusRef?.removeEventListener(listener)
            }
            callStatusListener = null
            callStatusRef = null
            
            // Remove waiting status listener
            waitingStatusListener?.let { listener ->
                waitingStatusRef?.removeEventListener(listener)
            }
            waitingStatusListener = null
            waitingStatusRef = null
        } catch (e: Exception) {
            Log.e("VideoCall", "onDestroy error: ${e.message}")
            e.printStackTrace()
        }
        super.onDestroy()
    }

    // New: Caller waits for the other user to answer
    private fun showWaitingIfPending(callId: String) {
        Log.d("VideoCallDebug", "showWaitingIfPending called for callId=$callId")
        waitingStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        waitingStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if Fragment is still attached and not in destruction process
                if (!isAdded || context == null || isFragmentDestroying) {
                    Log.d("VideoCall", "Fragment not attached or being destroyed, skipping showWaitingIfPending callback")
                    return
                }
                
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("VideoCallDebug", "showWaitingIfPending: status=$status for callId=$callId")
                
                when (status) {
                    "pending" -> {
                        Log.d("VideoCallDebug", "showWaitingIfPending: showing waitingDialog for callId=$callId")
                        context?.let { ctx ->
                            try {
                                waitingDialog = AlertDialog.Builder(ctx)
                                    .setTitle("Waiting for answer...")
                                    .setMessage("The other user is being called. Please wait.")
                                    .setNegativeButton("Cancel") { d, _ ->
                                        Log.d("VideoCallDebug", "showWaitingIfPending: Cancel clicked, ending callId=$callId")
                                        FirebaseDatabase.getInstance().getReference("calls").child(callId).child("status").setValue("ended")
                                        d.dismiss()
                                        if (isAdded && context != null && !isFragmentDestroying) {
                                            safePopBackStack()
                                        }
                                    }
                                    .setCancelable(false)
                                    .create()
                                waitingDialog?.show()
                            } catch (e: Exception) {
                                Log.e("VideoCall", "Error showing waiting dialog: ${e.message}")
                            }
                        }
                    }
                    "accepted" -> {
                        Log.d("VideoCallDebug", "showWaitingIfPending: Call accepted, dismissing dialog and starting call")
                        try { waitingDialog?.dismiss() } catch (_: Exception) {}
                        // Start the actual call process
                        if (!checkPermissions()) {
                            Log.d("PermissionDebug", "Permissions not granted, requesting...")
                            requestPermissions()
                        } else {
                            Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
                            fetchTokenAndJoinChannel()
                        }
                    }
                    "rejected", "ended" -> {
                        Log.d("VideoCallDebug", "showWaitingIfPending: Call ended: status=$status")
                        try { waitingDialog?.dismiss() } catch (_: Exception) {}
                        context?.let { ctx ->
                            val message = if (status == "rejected") "Call rejected" else "Call ended"
                            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                        }
                        if (isAdded && context != null && !isFragmentDestroying) {
                            safePopBackStack()
                        }
                    }
                    else -> {
                        Log.d("VideoCallDebug", "showWaitingIfPending: Unknown status: $status")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("VideoCallDebug", "showWaitingIfPending: onCancelled: ${error.message}")
            }
        }
        waitingStatusRef?.addValueEventListener(waitingStatusListener!!)
    }

    // Safe popBackStack method, completely avoiding findNavController
    private fun safePopBackStack() {
        if (!isAdded || activity == null || isFragmentDestroying) {
            Log.d("VideoCall", "Fragment not attached or being destroyed, skipping popBackStack")
            return
        }
        try {
            if (navController != null && navController!!.currentDestination != null) {
                Log.d("VideoCallDebug", "Using saved NavController to pop back")
                val popped = navController!!.popBackStack()
                if (!popped) {
                    // popBackStack失败，强制跳转到聊天界面
                    Log.d("VideoCallDebug", "popBackStack failed, navigating to chatScreenFragment")
                    navController!!.navigate(R.id.chatScreenFragment)
                }
                return
            }
        } catch (e: Exception) {
            Log.e("VideoCall", "Failed to use saved NavController: ${e.message}")
        }
        try {
            if (isAdded && activity != null) {
                Log.d("VideoCallDebug", "Using Activity's onBackPressed as fallback")
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        } catch (e2: Exception) {
            Log.e("VideoCall", "Fallback return method also failed: ${e2.message}")
        }
    }

    // Modify listenCallStatus, automatically close waiting interface when accepted
    private fun listenCallStatus(callId: String) {
        Log.d("VideoCallDebug", "listenCallStatus called for callId=$callId")
        callStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        callStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || activity == null || isFragmentDestroying) {
                    Log.d("VideoCall", "Fragment not attached or being destroyed, skipping listenCallStatus callback")
                    return
                }
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("VideoCallDebug", "listenCallStatus: status=$status for callId=$callId")
                activity?.runOnUiThread {
                    if (!isAdded || activity == null || isFragmentDestroying) {
                        Log.d("VideoCall", "Fragment not attached or being destroyed, skipping UI operation")
                        return@runOnUiThread
                    }
                    when (status) {
                        "accepted" -> {
                            Log.d("VideoCallDebug", "listenCallStatus: Call accepted, dismissing waiting dialog")
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                            // The actual call process will be handled by showWaitingIfPending
                        }
                        "ended" -> {
                            Log.d("VideoCallDebug", "listenCallStatus: Call ended")
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                            try {
                                Toast.makeText(activity!!.applicationContext, "Call ended", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("VideoCall", "Error showing toast: ${e.message}")
                            }
                            safePopBackStack()
                        }
                        "rejected" -> {
                            Log.d("VideoCallDebug", "listenCallStatus: Call rejected")
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                            try {
                                Toast.makeText(activity!!.applicationContext, "Call rejected", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("VideoCall", "Error showing toast: ${e.message}")
                            }
                            safePopBackStack()
                        }
                        else -> {
                            Log.d("VideoCallDebug", "listenCallStatus: Status: $status")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("VideoCallDebug", "listenCallStatus: onCancelled: ${error.message}")
            }
        }
        callStatusRef?.addValueEventListener(callStatusListener!!)
    }

    // Test method to verify token server connection
    private fun testTokenServer() {
        val testUrl = "https://agora-token-service-oajn.onrender.com/rtc/test_channel/publisher/uid/123/"
        Log.d("VideoCallDebug", "Testing token server connection: $testUrl")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(testUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VideoCallDebug", "Token server test failed: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("VideoCallDebug", "Token server test response: ${response.code} - $responseBody")
            }
        })
    }
}