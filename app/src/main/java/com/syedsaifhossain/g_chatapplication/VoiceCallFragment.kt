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
import java.util.concurrent.TimeUnit
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import androidx.navigation.fragment.findNavController
import android.app.AlertDialog
import androidx.navigation.NavController
import com.google.firebase.database.DatabaseReference
import android.widget.LinearLayout
import android.widget.ImageView
import com.bumptech.glide.Glide

class VoiceCallFragment : Fragment() {

    private val APP_ID = "66157d3652d548ba86cc9c6075a69274"
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

    private var callId: String? = null

    // 添加一个标志来跟踪Fragment是否正在销毁
    private var isFragmentDestroying = false

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraJoin", "onUserJoined: remote uid=$uid")
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Remote user offline: $uid", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d("AgoraJoin", "onJoinChannelSuccess: channel=$channel, uid=$uid")
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Log.d("AgoraVoice", "Joined channel successfully: $channel, uid: $uid")
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
                        }
                        startCallTimer()
                    }
                }
            }
        }

        override fun onError(err: Int) {
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Log.e("AgoraVoice", "Agora Error: $err")
                        val errorMessage = when(err) {
                            Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                            Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                            Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                            Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                            Constants.ERR_NO_PERMISSION -> "No audio recording permission."
                            else -> "Unknown Agora Error: $err"
                        }
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Agora Error: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private var waitingDialog: AlertDialog? = null
    private var navController: NavController? = null
    private var callStatusRef: DatabaseReference? = null
    private var callStatusListener: ValueEventListener? = null
    private var waitingStatusRef: DatabaseReference? = null
    private var waitingStatusListener: ValueEventListener? = null

    companion object {
        private const val TAG = "VoiceCallFragment"
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
            context?.let { ctx ->
                Toast.makeText(ctx, "Add contact clicked", Toast.LENGTH_SHORT).show()
            }
        }
        binding.videoCall.setOnClickListener {
            context?.let { ctx ->
                Toast.makeText(ctx, "This is a voice call. Video feature not active here.", Toast.LENGTH_SHORT).show()
            }
        }

        updateSpeakerButton()
        updateMicButton()

        if (!checkPermissions()) {
            Log.d("PermissionDebug", "Permissions not granted, requesting...")
            requestPermissions()
        } else {
            Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
            fetchTokenAndJoinChannel()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 重置销毁标志
        isFragmentDestroying = false
        
        // 安全获取NavController
        navController = try { 
            if (isAdded && parentFragmentManager.isStateSaved.not()) {
                findNavController() 
            } else {
                null
            }
        } catch (e: Exception) { 
            Log.e("VoiceCall", "获取NavController失败: ${e.message}")
            null 
        }
        
        callId = arguments?.getString("callId")
        Log.d("VoiceCallDebug", "onViewCreated: callId=$callId")
        if (callId != null) {
            listenCallStatus(callId!!)
            showWaitingIfPending(callId!!)
        }

        val groupId = arguments?.getString("groupId")
        val callIdArg = arguments?.getString("callId")
        if (!groupId.isNullOrEmpty() && !callIdArg.isNullOrEmpty()) {
            listenGroupCallMembers(groupId, callIdArg)
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d("PermissionDebug", "Checking permissions...")
        for (permission in REQUIRED_PERMISSIONS) {
            context?.let { ctx ->
                val status = ContextCompat.checkSelfPermission(ctx, permission)
                Log.d("PermissionDebug", "Permission $permission status: ${if (status == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
                if (status != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            } ?: return false
        }
        Log.d("PermissionDebug", "All permissions checked and granted.")
        return true
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                PERMISSION_REQ_ID
            )
        }
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
                context?.let { ctx ->
                    Toast.makeText(ctx, "Permissions not granted. Cannot start voice call.", Toast.LENGTH_LONG).show()
                }
                if (isAdded && activity != null) {
                    try {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } catch (e: Exception) {
                        Log.e("VoiceCall", "权限被拒绝时返回失败: ${e.message}")
                    }
                }
            }
        } else {
            Log.d("PermissionDebug", "Unknown request code: $requestCode")
        }
    }

    // ----------- Token 获取逻辑（Node.js Token Server） -----------
    private fun fetchTokenAndJoinChannel() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            if (isAdded) {
                context?.let { ctx ->
                    Toast.makeText(ctx, "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
                }
                if (isAdded && activity != null) {
                    try {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } catch (e: Exception) {
                        Log.e("VoiceCall", "用户未认证时返回失败: ${e.message}")
                    }
                }
            }
            return
        }

        val agoraUid = Math.abs(currentUser.uid.hashCode())
        val channelName = CHANNEL_NAME
        val url = "https://agora-token-service-oajn.onrender.com/rtc/$channelName/publisher/uid/$agoraUid/"
        
        Log.d("VoiceCall", "Requesting token with parameters:")
        Log.d("VoiceCall", "URL: $url")
        Log.d("VoiceCall", "Channel Name: $channelName")
        Log.d("VoiceCall", "User ID: ${currentUser.uid}")
        Log.d("VoiceCall", "Agora UID: $agoraUid")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isAdded) {
                    Log.d("VoiceCall", "Fragment not attached, ignoring network failure")
                    return
                }
                Log.e("VoiceCall", "Network request failed: ${e.message}")
                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Failed to get token: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            if (isAdded && activity != null) {
                                try {
                                    activity?.onBackPressedDispatcher?.onBackPressed()
                                } catch (e2: Exception) {
                                    Log.e("VoiceCall", "获取token失败时返回失败: ${e2.message}")
                                }
                            }
                        }
                    }
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (!isAdded) {
                    Log.d("VoiceCall", "Fragment not attached, ignoring network response")
                    return
                }

                Log.d("VoiceCall", "Server response code: ${response.code}")
                Log.d("VoiceCall", "Server response message: ${response.message}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("VoiceCall", "Server returned error: ${response.code}")
                    Log.e("VoiceCall", "Error response body: $errorBody")
                    if (isAdded && activity != null) {
                        activity?.runOnUiThread {
                            if (isAdded && context != null) {
                                when (response.code) {
                                    404 -> context?.let { ctx ->
                                        Toast.makeText(ctx, "Server temporarily unavailable, please try again later", Toast.LENGTH_LONG).show()
                                    }
                                    else -> context?.let { ctx ->
                                        Toast.makeText(ctx, "Server error: ${response.code}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                if (isAdded && activity != null) {
                                    try {
                                        activity?.onBackPressedDispatcher?.onBackPressed()
                                    } catch (e: Exception) {
                                        Log.e("VoiceCall", "服务器错误时返回失败: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                    return
                }

                try {
                    val body = response.body?.string()
                    Log.d("VoiceCall", "Server response body: $body")
                    
                    if (body.isNullOrEmpty()) {
                        Log.e("VoiceCall", "Empty response from server")
                        if (isAdded && activity != null) {
                            activity?.runOnUiThread {
                                if (isAdded && context != null) {
                                    context?.let { ctx ->
                                        Toast.makeText(ctx, "Empty response from server", Toast.LENGTH_LONG).show()
                                    }
                                    if (isAdded && activity != null) {
                                        try {
                                            activity?.onBackPressedDispatcher?.onBackPressed()
                                        } catch (e: Exception) {
                                            Log.e("VoiceCall", "空响应时返回失败: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                        return
                    }

                    val token = JSONObject(body).optString("rtcToken")
                    if (token.isNotEmpty()) {
                        Log.d("VoiceCall", "Successfully obtained token")
                        if (isAdded && activity != null) {
                            activity?.runOnUiThread {
                                if (isAdded && context != null) {
                                    initializeAndJoinChannel(token, agoraUid)
                                }
                            }
                        }
                    } else {
                        Log.e("VoiceCall", "No token in response: $body")
                        if (isAdded && activity != null) {
                            activity?.runOnUiThread {
                                if (isAdded && context != null) {
                                    context?.let { ctx ->
                                        Toast.makeText(ctx, "Failed to get call token from server.", Toast.LENGTH_LONG).show()
                                    }
                                    if (isAdded && activity != null) {
                                        try {
                                            activity?.onBackPressedDispatcher?.onBackPressed()
                                        } catch (e: Exception) {
                                            Log.e("VoiceCall", "无token时返回失败: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VoiceCall", "Error parsing response: ${e.message}")
                    Log.e("VoiceCall", "Response body that caused error: ${response.body?.string()}")
                    if (isAdded && activity != null) {
                        activity?.runOnUiThread {
                            if (isAdded && context != null) {
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Error parsing server response", Toast.LENGTH_LONG).show()
                                }
                                if (isAdded && activity != null) {
                                    try {
                                        activity?.onBackPressedDispatcher?.onBackPressed()
                                    } catch (e2: Exception) {
                                        Log.e("VoiceCall", "解析响应时返回失败: ${e2.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }
    // -------------------------------------------------------------

    private fun initializeAndJoinChannel(token: String, agoraUid: Int) {
        try {
            val config = RtcEngineConfig()
            config.mContext = context?.applicationContext
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

            agoraEngine = RtcEngine.create(config)
            Log.d("AgoraInit", "Agora RtcEngine created successfully for voice call.")

            try {
                agoraEngine?.enableAudio()
                agoraEngine?.disableVideo()
                agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error configuring audio: ${e.message}")
            }

            try {
                agoraEngine?.joinChannel(token, CHANNEL_NAME, null,agoraUid)
                Log.d("AgoraJoin", "joinChannel: token=$token, channel=$CHANNEL_NAME, uid=$agoraUid")
                context?.let { ctx ->
                    Toast.makeText(ctx, "Joining voice channel: $CHANNEL_NAME", Toast.LENGTH_SHORT).show()
                }
                Log.d("AgoraInit", "Join voice channel initiated for: $CHANNEL_NAME with token.")
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error joining channel: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e("AgoraInit", "Error initializing Agora for voice call: ${e.message}", e)
            context?.let { ctx ->
                Toast.makeText(ctx, "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
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
                                _binding?.videoCallTime?.text = timeStr
                            }
                        }
                    }
                }
            }, 1000, 1000)
        } catch (e: Exception) {
            Log.e("VoiceCall", "Error starting timer: ${e.message}")
        }
    }

    private fun stopCallTimer() {
        try {
            timer?.cancel()
            timer = null
            callDuration = 0
            if (isAdded && context != null) {
                handler.post {
                    if (isAdded && context != null) {
                        _binding?.videoCallTime?.text = "0:00"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceCall", "Error stopping timer: ${e.message}")
        }
    }
    // ----------------------------------------

    private fun endCall() {
        try {
            if (callId != null) {
                FirebaseDatabase.getInstance().getReference("calls").child(callId!!).child("status").setValue("ended")
            }
            Log.d("VoiceCall", "Starting to end call...")
            
            // 1. Stop timer
            Log.d("VoiceCall", "Stopping timer")
            stopCallTimer()
            
            // 2. Close waiting dialog
            Log.d("VoiceCall", "Closing waiting dialog")
            try {
                waitingDialog?.dismiss()
                waitingDialog = null
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error closing waiting dialog: ${e.message}")
            }
            
            // 3. Leave channel
            Log.d("VoiceCall", "Preparing to leave channel")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VoiceCall", "Left channel")
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error leaving channel: ${e.message}")
            }
            
            // 4. Destroy engine
            Log.d("VoiceCall", "Preparing to destroy engine")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VoiceCall", "Engine destroyed")
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error destroying engine: ${e.message}")
            }
            
            // 5. Show toast
            if (isAdded) {
                try {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Voice call ended", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("VoiceCall", "Error showing toast: ${e.message}")
                }
            }
            
            // 6. Return
            Log.d("VoiceCall", "Preparing to return")
            if (isAdded) {
                try {
                    safePopBackStack()
                    Log.d("VoiceCall", "Back pressed triggered")
                } catch (e: Exception) {
                    Log.e("VoiceCall", "Error returning: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e("VoiceCall", "Error ending call: ${e.message}")
            e.printStackTrace()
            if (isAdded) {
                try {
                    safePopBackStack()
                } catch (e2: Exception) {
                    Log.e("VoiceCall", "Error handling return: ${e2.message}")
                }
            }
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView called")
        super.onDestroyView()
        
        // 设置销毁标志
        isFragmentDestroying = true
        
        try {
            // 1. Stop timer
            Log.d("VoiceCall", "Stopping timer")
            stopCallTimer()
            
            // 2. Close waiting dialog
            Log.d("VoiceCall", "Closing waiting dialog")
            try {
                waitingDialog?.dismiss()
                waitingDialog = null
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error closing waiting dialog: ${e.message}")
            }
            
            // 3. Leave channel
            Log.d("VoiceCall", "Preparing to leave channel")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VoiceCall", "Left channel")
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error leaving channel: ${e.message}")
            }
            
            // 4. Destroy engine
            Log.d("VoiceCall", "Preparing to destroy engine")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VoiceCall", "Engine destroyed")
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error destroying engine: ${e.message}")
            }
            
            // 5. Clean up binding
            Log.d("VoiceCall", "Cleaning up binding")
            _binding = null
            
            // 6. 安全移除监听器
            callStatusListener?.let { listener ->
                callStatusRef?.removeEventListener(listener)
            }
            callStatusListener = null
            callStatusRef = null
            
            // 7. 移除等待状态监听器
            waitingStatusListener?.let { listener ->
                waitingStatusRef?.removeEventListener(listener)
            }
            waitingStatusListener = null
            waitingStatusRef = null
            
            navController = null
            
        } catch (e: Exception) {
            Log.e("VoiceCall", "onDestroyView error: ${e.message}")
            e.printStackTrace()
        }
        Log.d("VoiceCall", "onDestroyView ending")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
        try {
            // 确保在Fragment完全销毁时清理所有资源
            stopCallTimer()
            waitingDialog?.dismiss()
            agoraEngine?.leaveChannel()
            RtcEngine.destroy()
            agoraEngine = null
            
            // 移除Firebase监听器
            callStatusListener?.let { listener ->
                callStatusRef?.removeEventListener(listener)
            }
            callStatusListener = null
            callStatusRef = null
            
            // 移除等待状态监听器
            waitingStatusListener?.let { listener ->
                waitingStatusRef?.removeEventListener(listener)
            }
            waitingStatusListener = null
            waitingStatusRef = null
        } catch (e: Exception) {
            Log.e("VoiceCall", "onDestroy error: ${e.message}")
        }
    }

    // 兼容 xml onClick 的 joinChannel 方法
    fun joinChannel(view: View) {
        fetchTokenAndJoinChannel()
    }

    // 新增：主叫方等待对方接听的界面
    private fun showWaitingIfPending(callId: String) {
        Log.d("VoiceCallDebug", "showWaitingIfPending called for callId=$callId")
        waitingStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        waitingStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 检查Fragment是否还附加且未在销毁过程中
                if (!isAdded || context == null || isFragmentDestroying) {
                    Log.d("VoiceCall", "Fragment未附加或正在销毁，跳过showWaitingIfPending回调")
                    return
                }
                
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("VoiceCallDebug", "showWaitingIfPending: status=$status for callId=$callId")
                if (status == "pending") {
                    Log.d("VoiceCallDebug", "showWaitingIfPending: showing waitingDialog for callId=$callId")
                    context?.let { ctx ->
                        try {
                            waitingDialog = AlertDialog.Builder(ctx)
                                .setTitle("Waiting for answer...")
                                .setMessage("The other user is being called. Please wait.")
                                .setNegativeButton("Cancel") { d, _ ->
                                    Log.d("VoiceCallDebug", "showWaitingIfPending: Cancel clicked, ending callId=$callId")
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
                            Log.e("VoiceCall", "显示等待对话框时出错: ${e.message}")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("VoiceCallDebug", "showWaitingIfPending: onCancelled: ${error.message}")
            }
        }
        waitingStatusRef?.addValueEventListener(waitingStatusListener!!)
    }

    // 安全的popBackStack方法，完全避免使用findNavController
    private fun safePopBackStack() {
        if (!isAdded || activity == null || isFragmentDestroying) {
            Log.d("VoiceCall", "Fragment未附加或正在销毁，跳过popBackStack")
            return
        }
        
        try {
            // 优先使用已保存的NavController
            if (navController != null && navController!!.currentDestination != null) {
                navController!!.popBackStack()
                return
            }
        } catch (e: Exception) {
            Log.e("VoiceCall", "使用已保存的NavController失败: ${e.message}")
        }
        
        // 备用方案：使用Activity的onBackPressed
        try {
            if (isAdded && activity != null) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        } catch (e2: Exception) {
            Log.e("VoiceCall", "备用返回方案也失败: ${e2.message}")
        }
    }

    // 修改listenCallStatus，接听后自动关闭等待界面
    private fun listenCallStatus(callId: String) {
        Log.d("VoiceCallDebug", "listenCallStatus called for callId=$callId")
        callStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        callStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || activity == null || isFragmentDestroying) {
                    Log.d("VoiceCall", "Fragment未附加或正在销毁，跳过listenCallStatus回调")
                    return
                }
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("VoiceCallDebug", "listenCallStatus: status=$status for callId=$callId")
                activity?.runOnUiThread {
                    if (!isAdded || activity == null || isFragmentDestroying) {
                        Log.d("VoiceCall", "Fragment未附加或正在销毁，跳过UI操作")
                        return@runOnUiThread
                    }
                    when (status) {
                        "ended" -> {
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                            try {
                                Toast.makeText(activity!!.applicationContext, "Call ended", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("VoiceCall", "显示Toast时出错: ${e.message}")
                            }
                            safePopBackStack()
                        }
                        "accepted" -> {
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("VoiceCallDebug", "listenCallStatus: onCancelled: ${error.message}")
            }
        }
        callStatusRef?.addValueEventListener(callStatusListener!!)
    }

    private fun listenGroupCallMembers(groupId: String, callId: String) {
        val membersRef = FirebaseDatabase.getInstance()
            .getReference("group_calls")
            .child(groupId)
            .child(callId)
            .child("members")
        val avatarLayout = view?.findViewById<LinearLayout>(R.id.groupCallAvatarsLayout)
        membersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                avatarLayout?.removeAllViews()
                for (memberSnap in snapshot.children) {
                    val userId = memberSnap.key ?: continue
                    val status = memberSnap.getValue(String::class.java)
                    if (status == "joined") {
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    val avatarUrl = userSnap.child("profileImageUrl").getValue(String::class.java)
                                        ?: userSnap.child("avatarUrl").getValue(String::class.java)
                                        ?: ""
                                    val imageView = ImageView(requireContext())
                                    val size = resources.getDimensionPixelSize(R.dimen.avatar_size)
                                    val params = LinearLayout.LayoutParams(size, size)
                                    params.setMargins(8, 0, 8, 0)
                                    imageView.layoutParams = params
                                    imageView.setImageResource(R.drawable.default_avatar)
                                    Glide.with(this@VoiceCallFragment)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .circleCrop()
                                        .into(imageView)
                                    avatarLayout?.addView(imageView)
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
