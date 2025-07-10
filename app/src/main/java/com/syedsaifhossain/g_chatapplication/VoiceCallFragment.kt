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

    // 在所有Agora回调中补充详细日志
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraJoin", "onUserJoined: remote uid=$uid, callId=$callId")
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
            Log.d("AgoraJoin", "onUserOffline: remote uid=$uid, reason=$reason, callId=$callId")
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
            Log.d("AgoraJoin", "onJoinChannelSuccess: channel=$channel, uid=$uid, callId=$callId")
            channelJoinRetryCount = 0
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Log.d("AgoraVoice", "Joined channel successfully: $channel, uid: $uid")
                        // 强制初始化音频状态
                        try {
                            agoraEngine?.muteLocalAudioStream(false)
                            Log.d("AgoraVoice", "Called muteLocalAudioStream(false)")
                            agoraEngine?.setEnableSpeakerphone(true)
                            Log.d("AgoraVoice", "Called setEnableSpeakerphone(true)")
                        } catch (e: Exception) {
                            Log.e("AgoraVoice", "Error initializing audio state: ${e.message}")
                        }
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
                        }
                        startCallTimer()
                    }
                }
            }
        }

        override fun onError(err: Int) {
            Log.e("AgoraVoice", "Agora Error: $err, callId=$callId")
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        val errorMessage = when(err) {
                            Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                            Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                            Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                            Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                            Constants.ERR_NO_PERMISSION -> "No audio recording permission."
                            else -> "Unknown Agora Error: $err"
                        }
                        
                        // Retry channel join for certain errors
                        if (err == Constants.ERR_JOIN_CHANNEL_REJECTED || err == Constants.ERR_INVALID_TOKEN) {
                            if (channelJoinRetryCount < maxChannelJoinRetries - 1) {
                                channelJoinRetryCount++
                                Log.d("VoiceCallDebug", "Retrying channel join (${channelJoinRetryCount + 1}/$maxChannelJoinRetries)")
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
                                Log.e("VoiceCallDebug", "Max channel join retries reached")
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
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d("AgoraVoice", "onConnectionStateChanged: state=$state, reason=$reason, callId=$callId")
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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 修复：确保navController正确初始化
        try {
            if (isAdded && !isFragmentDestroying) {
                navController = findNavController()
                Log.d("VoiceCallDebug", "NavController initialized successfully")
            } else {
                Log.d("VoiceCallDebug", "Fragment not ready for NavController initialization")
            }
        } catch (e: Exception) { 
            Log.e("VoiceCall", "Failed to get NavController: ${e.message}")
            navController = null
        }
        
        // 关键：每次都从arguments获取callId
        callId = arguments?.getString("callId")
        
        // 打印所有参数
        Log.d("VoiceCallDebug", "onViewCreated: callId=$callId, arguments=$arguments")
        if (callId.isNullOrEmpty()) {
            Log.e("VoiceCallDebug", "callId is null or empty in onViewCreated, aborting")
            context?.let { ctx ->
                Toast.makeText(ctx, "通话参数异常，请重试", Toast.LENGTH_LONG).show()
            }
            safePopBackStack()
            return
        }
        
        // Start initialization process immediately
        if (callId != null) {
            listenCallStatus(callId!!)
            showWaitingIfPending(callId!!)
            
            // Test token server connection first
            testTokenServer()
            
            // Start permission check and initialization immediately
            Log.d("VoiceCallDebug", "Starting permission check and initialization process")
            if (!checkPermissions()) {
                Log.d("PermissionDebug", "Permissions not granted, requesting...")
                requestPermissions()
            } else {
                Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
                fetchTokenAndJoinChannel()
            }
        } else {
            Log.e("VoiceCallDebug", "callId is null, cannot proceed")
        }

        val groupId = arguments?.getString("groupId")
        val callIdArg = arguments?.getString("callId")
        if (!groupId.isNullOrEmpty() && !callIdArg.isNullOrEmpty()) {
            listenGroupCallMembers(groupId, callIdArg)
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d("VoiceCallDebug", "Checking permissions...")
        for (permission in REQUIRED_PERMISSIONS) {
            context?.let { ctx ->
                val status = ContextCompat.checkSelfPermission(ctx, permission)
                Log.d("VoiceCallDebug", "Permission $permission status: ${if (status == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
                if (status != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            } ?: return false
        }
        Log.d("VoiceCallDebug", "All permissions granted")
        return true
    }

    private fun requestPermissions() {
        Log.d("VoiceCallDebug", "Requesting permissions")
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
        Log.d("VoiceCallDebug", "Permission request result: requestCode=$requestCode")
        if (requestCode == PERMISSION_REQ_ID) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            for (i in permissions.indices) {
                Log.d("VoiceCallDebug", "Permission: ${permissions[i]}, Granted: ${grantResults[i] == PackageManager.PERMISSION_GRANTED}")
            }
            if (allGranted) {
                Log.d("VoiceCallDebug", "All permissions granted, starting token fetch and channel initialization")
                fetchTokenAndJoinChannel()
            } else {
                Log.e("VoiceCallDebug", "Not all permissions granted, cannot start voice call")
                context?.let { ctx ->
                    Toast.makeText(ctx, "Permissions not granted. Cannot start voice call.", Toast.LENGTH_LONG).show()
                }
                if (isAdded && activity != null) {
                    try {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } catch (e: Exception) {
                        Log.e("VoiceCall", "Failed to go back when permissions denied: ${e.message}")
                    }
                }
            }
        } else {
            Log.d("VoiceCallDebug", "Unknown request code: $requestCode")
        }
    }

    // ----------- Token 获取逻辑（Node.js Token Server） -----------
    private var tokenRetryCount = 0
    private val maxTokenRetries = 3
    
    private fun fetchTokenAndJoinChannel() {
        val currentUser = auth.currentUser
        Log.d("VoiceCallDebug", "fetchTokenAndJoinChannel: callId=$callId, currentUser=${currentUser?.uid}")
        if (currentUser == null) {
            Log.e("VoiceCallDebug", "User not authenticated, navigating to login page")
            try {
                navController?.navigate(R.id.loginPage)
            } catch (e: Exception) {
                Log.e("VoiceCallDebug", "Failed to navigate to login page: ${e.message}")
            }
            return
        }
        if (callId.isNullOrEmpty()) {
            Log.e("VoiceCallDebug", "callId is null or empty in fetchTokenAndJoinChannel, aborting")
            context?.let { ctx ->
                Toast.makeText(ctx, "通话参数异常，请重试", Toast.LENGTH_LONG).show()
            }
            safePopBackStack()
            return
        }
        val agoraUid = Math.abs(currentUser.uid.hashCode())
        val channelName = "channel_$callId"
        Log.d("VoiceCallDebug", "fetchTokenAndJoinChannel: channelName=$channelName, agoraUid=$agoraUid")
        val url = "https://agora-token-service-oajn.onrender.com/rtc/$channelName/publisher/uid/$agoraUid/"
        Log.d("VoiceCallDebug", "Request URL: $url")

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VoiceCallDebug", "Failed to get token (attempt ${tokenRetryCount + 1}): ${e.message}")
                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            if (tokenRetryCount < maxTokenRetries - 1) {
                                tokenRetryCount++
                                Log.d("VoiceCallDebug", "Retrying token request (${tokenRetryCount + 1}/$maxTokenRetries)")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Retrying token request...", Toast.LENGTH_SHORT).show()
                                }
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isAdded && !isFragmentDestroying) {
                                        fetchTokenAndJoinChannel()
                                    }
                                }, 2000)
                            } else {
                                Log.e("VoiceCallDebug", "Max token retries reached")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Failed to get token after $maxTokenRetries attempts", Toast.LENGTH_LONG).show()
                                }
                                safePopBackStack()
                            }
                        }
                    }
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("VoiceCallDebug", "Token response code: ${response.code}")
                Log.d("VoiceCallDebug", "Token response body: $responseBody")
                
                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                                try {
                                    val jsonObject = JSONObject(responseBody)
                                    val token = jsonObject.getString("rtcToken")
                                    Log.d("VoiceCallDebug", "Token received successfully, length: ${token.length}")
                                    
                                    // Initialize Agora engine
                                    Log.d("VoiceCallDebug", "Initializing Agora engine...")
                                    val config = RtcEngineConfig()
                                    config.mAppId = APP_ID
                                    config.mContext = context?.applicationContext
                                    config.mEventHandler = mRtcEventHandler
                                    
                                    try {
                                        agoraEngine = RtcEngine.create(config)
                                        Log.d("VoiceCallDebug", "Agora engine created successfully")
                                        
                                        // Enable audio
                                        agoraEngine?.enableAudio()
                                        Log.d("VoiceCallDebug", "Audio enabled")
                                        
                                        // Join channel
                                        Log.d("VoiceCallDebug", "Joining channel: $channelName, uid: $agoraUid, token: ${token.take(20)}...")
                                        val result = agoraEngine?.joinChannel(token, channelName, null, agoraUid)
                                        Log.d("VoiceCallDebug", "joinChannel result: $result")
                                        
                                        if (result == 0) {
                                            Log.d("VoiceCallDebug", "joinChannel called successfully")
                                        } else {
                                            Log.e("VoiceCallDebug", "joinChannel failed with error: $result")
                                        }
                                        
                                    } catch (e: Exception) {
                                        Log.e("VoiceCallDebug", "Error initializing Agora engine: ${e.message}")
                                        e.printStackTrace()
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Error initializing call engine", Toast.LENGTH_LONG).show()
                                        }
                                        safePopBackStack()
                                    }
                                    
                                } catch (e: Exception) {
                                    Log.e("VoiceCallDebug", "Error parsing token response: ${e.message}")
                                    context?.let { ctx ->
                                        Toast.makeText(ctx, "Error parsing server response", Toast.LENGTH_LONG).show()
                                    }
                                    safePopBackStack()
                                }
                            } else {
                                Log.e("VoiceCallDebug", "Token request failed: ${response.code}")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Failed to get token from server", Toast.LENGTH_LONG).show()
                                }
                                safePopBackStack()
                            }
                        }
                    }
                }
            }
        })
    }
    // -------------------------------------------------------------

    private var channelJoinRetryCount = 0
    private val maxChannelJoinRetries = 3

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
        Log.d("VoiceCall", "onDestroyView started")
        
        // Set destruction flag
        isFragmentDestroying = true
        
        try {
            // 1. Stop timer
            Log.d("VoiceCall", "Stopping timer")
            stopCallTimer()
            
            // 2. Dismiss waiting dialog
            Log.d("VoiceCall", "Dismissing waiting dialog")
            try {
                waitingDialog?.dismiss()
                waitingDialog = null
            } catch (e: Exception) {
                Log.e("VoiceCall", "Error dismissing waiting dialog: ${e.message}")
            }
            
            // 3. Leave channel
            Log.d("VoiceCall", "Preparing to leave channel")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VoiceCall", "Left channel successfully")
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
            
            // 5. Safely remove listeners
            callStatusListener?.let { listener ->
                callStatusRef?.removeEventListener(listener)
            }
            callStatusListener = null
            callStatusRef = null
            
            // 6. Remove waiting status listener
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
        Log.d("VoiceCall", "onDestroyView ended")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
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
            Log.e("VoiceCall", "onDestroy error: ${e.message}")
        }
    }

    // 兼容 xml onClick 的 joinChannel 方法
    fun joinChannel(view: View) {
        fetchTokenAndJoinChannel()
    }

    // New: Caller waits for the other user to answer
    private fun showWaitingIfPending(callId: String) {
        Log.d("VoiceCallDebug", "showWaitingIfPending called for callId=$callId")
        waitingStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        waitingStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if Fragment is still attached and not in destruction process
                if (!isAdded || context == null || isFragmentDestroying) {
                    Log.d("VoiceCall", "Fragment not attached or being destroyed, skipping showWaitingIfPending callback")
                    return
                }
                
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("VoiceCallDebug", "showWaitingIfPending: status=$status for callId=$callId")
                
                when (status) {
                    "pending" -> {
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
                                Log.e("VoiceCall", "Error showing waiting dialog: ${e.message}")
                            }
                        }
                    }
                    "accepted" -> {
                        Log.d("VoiceCallDebug", "showWaitingIfPending: Call accepted, dismissing dialog and starting call")
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
                        Log.d("VoiceCallDebug", "showWaitingIfPending: Call ended: status=$status")
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
                        Log.d("VoiceCallDebug", "showWaitingIfPending: Unknown status: $status")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("VoiceCallDebug", "showWaitingIfPending: onCancelled: ${error.message}")
            }
        }
        waitingStatusRef?.addValueEventListener(waitingStatusListener!!)
    }

    // Safe popBackStack method, completely avoiding findNavController
    private fun safePopBackStack() {
        if (!isAdded || activity == null || isFragmentDestroying) {
            Log.d("VoiceCall", "Fragment not attached or being destroyed, skipping popBackStack")
            return
        }
        
        Log.d("VoiceCallDebug", "safePopBackStack called, navController=$navController")
        
        try {
            if (navController != null && navController!!.currentDestination != null) {
                Log.d("VoiceCallDebug", "Using saved NavController to pop back")
                Log.d("VoiceCallDebug", "Current destination: ${navController!!.currentDestination?.label}")
                val popped = navController!!.popBackStack()
                Log.d("VoiceCallDebug", "popBackStack result: $popped")
                if (!popped) {
                    // popBackStack失败，强制跳转到聊天界面
                    Log.d("VoiceCallDebug", "popBackStack failed, navigating to chatScreenFragment")
                    navController!!.navigate(R.id.chatScreenFragment)
                } else {
                    Log.d("VoiceCallDebug", "popBackStack succeeded, should be back to previous screen")
                }
                return
            } else {
                Log.d("VoiceCallDebug", "NavController is null or currentDestination is null")
            }
        } catch (e: Exception) {
            Log.e("VoiceCall", "Failed to use saved NavController: ${e.message}")
        }
        
        // 兜底方案1：尝试重新获取NavController
        try {
            if (isAdded && !isFragmentDestroying) {
                val freshNavController = findNavController()
                Log.d("VoiceCallDebug", "Trying fresh NavController")
                Log.d("VoiceCallDebug", "Fresh NavController current destination: ${freshNavController.currentDestination?.label}")
                val popped = freshNavController.popBackStack()
                Log.d("VoiceCallDebug", "Fresh NavController popBackStack result: $popped")
                if (!popped) {
                    Log.d("VoiceCallDebug", "Fresh NavController popBackStack failed, navigating to chatScreenFragment")
                    freshNavController.navigate(R.id.chatScreenFragment)
                } else {
                    Log.d("VoiceCallDebug", "Fresh NavController popBackStack succeeded")
                }
                return
            }
        } catch (e: Exception) {
            Log.e("VoiceCall", "Failed to use fresh NavController: ${e.message}")
        }
        
        // 兜底方案2：使用Activity的onBackPressed
        try {
            if (isAdded && activity != null) {
                Log.d("VoiceCallDebug", "Using Activity's onBackPressed as fallback")
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        } catch (e2: Exception) {
            Log.e("VoiceCall", "Fallback return method also failed: ${e2.message}")
        }
    }

    // Modify listenCallStatus, automatically close waiting interface when accepted
    private fun listenCallStatus(callId: String) {
        Log.d("VoiceCallDebug", "listenCallStatus called for callId=$callId")
        callStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        callStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || activity == null || isFragmentDestroying) {
                    Log.d("VoiceCall", "Fragment not attached or being destroyed, skipping listenCallStatus callback")
                    return
                }
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("VoiceCallDebug", "listenCallStatus: status=$status for callId=$callId")
                activity?.runOnUiThread {
                    if (!isAdded || activity == null || isFragmentDestroying) {
                        Log.d("VoiceCall", "Fragment not attached or being destroyed, skipping UI operation")
                        return@runOnUiThread
                    }
                    when (status) {
                        "accepted" -> {
                            Log.d("VoiceCallDebug", "listenCallStatus: Call accepted, dismissing waiting dialog")
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                            // The actual call process will be handled by showWaitingIfPending
                        }
                        "ended" -> {
                            Log.d("VoiceCallDebug", "listenCallStatus: Call ended")
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                            try {
                                Toast.makeText(activity!!.applicationContext, "Call ended", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("VoiceCall", "Error showing toast: ${e.message}")
                            }
                            safePopBackStack()
                        }
                        "rejected" -> {
                            Log.d("VoiceCallDebug", "listenCallStatus: Call rejected")
                            try { waitingDialog?.dismiss() } catch (_: Exception) {}
                            try {
                                Toast.makeText(activity!!.applicationContext, "Call rejected", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("VoiceCall", "Error showing toast: ${e.message}")
                            }
                            safePopBackStack()
                        }
                        else -> {
                            Log.d("VoiceCallDebug", "listenCallStatus: Status: $status")
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

    // Test method to verify token server connection
    private fun testTokenServer() {
        val testUrl = "https://agora-token-service-oajn.onrender.com/rtc/test_channel/publisher/uid/123/"
        Log.d("VoiceCallDebug", "Testing token server connection: $testUrl")
        
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(testUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VoiceCallDebug", "Token server test failed: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("VoiceCallDebug", "Token server test response: ${response.code} - $responseBody")
            }
        })
    }
}
