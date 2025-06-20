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
            Log.d("AgoraDebug", "onJoinChannelSuccess: channel=$channel, uid=$uid, elapsed=$elapsed")
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Log.d("Agora", "Joined channel successfully: $channel, uid: $uid")
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
                        }
                        startCallTimerIfNeeded()
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
            Log.e("AgoraDebug", "onError: $err")
            if (isAdded && activity != null) {
                activity?.runOnUiThread {
                    if (isAdded && context != null) {
                        Log.e("Agora", "Agora Error: $err")
                        val errorMessage = when(err) {
                            Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                            Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                            Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                            Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                            Constants.ERR_NO_PERMISSION -> "No video or audio recording permission."
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
        
        // 重置销毁标志
        isFragmentDestroying = false
        
        // 安全获取NavController
        safeNavController = try { 
            if (isAdded && parentFragmentManager.isStateSaved.not()) {
                findNavController() 
            } else {
                null
            }
        } catch (e: Exception) { 
            Log.e("VideoCall", "获取NavController失败: ${e.message}")
            null 
        }
        
        callId = arguments?.getString("callId")
        Log.d("VideoCallDebug", "onViewCreated: callId=$callId")
        if (callId != null) {
            listenCallStatus(callId!!)
            showWaitingIfPending(callId!!)
        }
        
        if (!checkPermissions()) {
            Log.d("PermissionDebug", "Permissions not granted, requesting...")
            requestPermissions()
        } else {
            Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
            fetchTokenAndJoinChannel()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                getRequiredPermissions(),
                PERMISSION_REQ_ID
            )
        }
    }

    private fun checkPermissions(): Boolean {
        Log.d("PermissionDebug", "Checking permissions...")
        for (permission in getRequiredPermissions()) {
            context?.let { ctx ->
                val status = ContextCompat.checkSelfPermission(ctx, permission)
                Log.d("PermissionDebug", "Permission $permission status: ${if (status == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
                if (status != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            } ?: return false
        }
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
                context?.let { ctx ->
                    Toast.makeText(ctx, "Permissions not granted. Cannot start video call.", Toast.LENGTH_LONG).show()
                }
                if (isAdded && activity != null) {
                    try {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } catch (e: Exception) {
                        Log.e("VideoCall", "权限被拒绝时返回失败: ${e.message}")
                    }
                }
            }
        } else {
            Log.d("PermissionDebug", "Unknown request code: $requestCode")
        }
    }

    // Use OkHttp to get token and join channel
    private fun fetchTokenAndJoinChannel() {
        if (auth.currentUser == null) {
            context?.let { ctx ->
                Toast.makeText(ctx, "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val agoraUid = Math.abs(auth.currentUser!!.uid.hashCode())
        val channelName = callId ?: CHANNEL_NAME
        val url = "https://agora-token-service-oajn.onrender.com/rtc/$channelName/publisher/uid/$agoraUid/"

        Log.d("TokenDebug", "请求参数: channelName=$channelName, uid=$agoraUid")
        Log.d("TokenDebug", "请求URL: $url")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AgoraDebug", "Failed to get token: ${e.message}")
                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            context?.let { ctx ->
                                Toast.makeText(ctx, "Failed to get token: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("AgoraDebug", "Token response: $responseBody")

                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (isAdded && context != null) {
                            when (response.code) {
                                200 -> {
                                    if (!responseBody.isNullOrEmpty()) {
                                        try {
                                            val jsonResponse = JSONObject(responseBody)
                                            val token = jsonResponse.getString("rtcToken")
                                            Log.d("AgoraDebug", "Token received: $token")
                                            initializeAgoraEngine(token, agoraUid, channelName)
                                        } catch (e: Exception) {
                                            Log.e("AgoraDebug", "Error parsing server response", e)
                                            context?.let { ctx ->
                                                Toast.makeText(ctx, "Error parsing server response", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Log.e("AgoraDebug", "Empty response from server")
                                        context?.let { ctx ->
                                            Toast.makeText(ctx, "Empty response from server", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                404 -> context?.let { ctx ->
                                    Toast.makeText(ctx, "Server temporarily unavailable, please try again later", Toast.LENGTH_LONG).show()
                                }
                                else -> context?.let { ctx ->
                                    Toast.makeText(ctx, "Server error: ${response.code}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    // Initialize and join channel, pass agoraUid
    private fun initializeAgoraEngine(token: String, agoraUid: Int, channelName: String) {
        try {
            val config = RtcEngineConfig()
            config.mContext = context?.applicationContext
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

                    if (isAdded && activity != null) {
                        activity?.runOnUiThread {
                            if (isAdded && context != null) {
                                setupLocalVideo()
                                agoraEngine?.joinChannel(token, channelName, null, agoraUid)
                                Log.d("AgoraDebug", "joinChannel 已调用, channelName=$channelName, uid=$agoraUid")
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Joining channel: $channelName", Toast.LENGTH_SHORT).show()
                                }
                                Log.d("AgoraInit", "Join channel initiated for: $channelName with token.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AgoraInit", "Error in video initialization: ", e)
                    if (isAdded && activity != null) {
                        activity?.runOnUiThread {
                            if (isAdded && context != null) {
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "Error initializing video: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e("AgoraInit", "Error initializing Agora: ", e)
            context?.let { ctx ->
                Toast.makeText(ctx, "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
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
            Log.d("VideoCall", "开始结束通话...")
            
            // 1. 停止计时器
            Log.d("VideoCall", "停止计时器")
            stopCallTimer()
            
            // 2. 关闭等待对话框
            Log.d("VideoCall", "关闭等待对话框")
            try {
                waitingDialog?.dismiss()
                waitingDialog = null
            } catch (e: Exception) {
                Log.e("VideoCall", "关闭等待对话框时出错: ${e.message}")
            }
            
            // 3. 离开频道
            Log.d("VideoCall", "准备离开频道")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VideoCall", "已离开频道")
            } catch (e: Exception) {
                Log.e("VideoCall", "离开频道时出错: ${e.message}")
            }
            
            // 4. 清理视频视图
            Log.d("VideoCall", "清理视频视图")
            try {
                _binding?.localVideoViewContainer?.removeAllViews()
                _binding?.remoteVideoViewContainer?.removeAllViews()
                Log.d("VideoCall", "视频视图已清理")
            } catch (e: Exception) {
                Log.e("VideoCall", "清理视频视图时出错: ${e.message}")
            }
            
            // 5. 销毁引擎
            Log.d("VideoCall", "准备销毁引擎")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VideoCall", "引擎已销毁")
            } catch (e: Exception) {
                Log.e("VideoCall", "销毁引擎时出错: ${e.message}")
            }
            
            // 6. 显示提示
            if (isAdded) {
                try {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Video call ended", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("VideoCall", "显示Toast时出错: ${e.message}")
                }
            }
            
            // 7. 返回
            Log.d("VideoCall", "准备返回")
            if (isAdded) {
                try {
                    safePopBackStack()
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
                    safePopBackStack()
                } catch (e2: Exception) {
                    Log.e("VideoCall", "错误处理时返回失败: ${e2.message}")
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
        Log.d("VideoCall", "onDestroyView 开始")
        
        // 设置销毁标志
        isFragmentDestroying = true
        
        try {
            // 1. 停止计时器
            Log.d("VideoCall", "停止计时器")
            stopCallTimer()
            
            // 2. 关闭等待对话框
            Log.d("VideoCall", "关闭等待对话框")
            try {
                waitingDialog?.dismiss()
                waitingDialog = null
            } catch (e: Exception) {
                Log.e("VideoCall", "关闭等待对话框时出错: ${e.message}")
            }
            
            // 3. 离开频道
            Log.d("VideoCall", "准备离开频道")
            try {
                agoraEngine?.leaveChannel()
                Log.d("VideoCall", "已离开频道")
            } catch (e: Exception) {
                Log.e("VideoCall", "离开频道时出错: ${e.message}")
            }
            
            // 4. 清理视频视图
            Log.d("VideoCall", "清理视频视图")
            try {
                _binding?.localVideoViewContainer?.removeAllViews()
                _binding?.remoteVideoViewContainer?.removeAllViews()
                Log.d("VideoCall", "视频视图已清理")
            } catch (e: Exception) {
                Log.e("VideoCall", "清理视频视图时出错: ${e.message}")
            }
            
            // 5. 销毁引擎
            Log.d("VideoCall", "准备销毁引擎")
            try {
                RtcEngine.destroy()
                agoraEngine = null
                Log.d("VideoCall", "引擎已销毁")
            } catch (e: Exception) {
                Log.e("VideoCall", "销毁引擎时出错: ${e.message}")
            }
            
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
            
            safeNavController = null
            
        } catch (e: Exception) {
            Log.e("VideoCall", "onDestroyView 发生错误: ${e.message}")
            e.printStackTrace()
        }
        Log.d("VideoCall", "onDestroyView 结束")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d("VideoCall", "onDestroy called")
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
            Log.e("VideoCall", "onDestroy 发生错误: ${e.message}")
            e.printStackTrace()
        }
        super.onDestroy()
    }

    // 新增：主叫方等待对方接听的界面
    private fun showWaitingIfPending(callId: String) {
        Log.d("VideoCallDebug", "showWaitingIfPending called for callId=$callId")
        waitingStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        waitingStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 检查Fragment是否还附加且未在销毁过程中
                if (!isAdded || context == null || isFragmentDestroying) {
                    Log.d("VideoCall", "Fragment未附加或正在销毁，跳过showWaitingIfPending回调")
                    return
                }
                
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("VideoCallDebug", "showWaitingIfPending: status=$status for callId=$callId")
                if (status == "pending") {
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
                            Log.e("VideoCall", "显示等待对话框时出错: ${e.message}")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("VideoCallDebug", "showWaitingIfPending: onCancelled: ${error.message}")
            }
        }
        waitingStatusRef?.addValueEventListener(waitingStatusListener!!)
    }

    // 终极安全popBackStack方法，完全避免使用findNavController
    private fun safePopBackStack() {
        // 首先检查Fragment是否还附加到Activity
        if (!isAdded || context == null) {
            Log.d("VideoCall", "Fragment未附加，跳过popBackStack")
            return
        }
        
        try {
            // 优先使用已保存的safeNavController
            safeNavController?.let { nav ->
                try {
                    nav.popBackStack()
                    return
                } catch (e: Exception) {
                    Log.e("VideoCall", "SafeNavController popBackStack error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("VideoCall", "SafeNavController access error: ${e.message}")
        }
        
        // 备用方案：使用Activity的onBackPressed
        try {
            if (isAdded && activity != null) {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        } catch (e: Exception) {
            Log.e("VideoCall", "Activity onBackPressed fallback error: ${e.message}")
        }
    }

    // 修改listenCallStatus，彻底防护
    private fun listenCallStatus(callId: String) {
        Log.d("VideoCallFragment", "listenCallStatus started")
        callStatusRef = FirebaseDatabase.getInstance().getReference("calls").child(callId)
        callStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 检查Fragment是否还附加且未在销毁过程中
                if (!isAdded || context == null || isFragmentDestroying) {
                    Log.d("VideoCall", "Fragment未附加或正在销毁，跳过listenCallStatus回调")
                    return
                }
                
                val status = snapshot.child("status").getValue(String::class.java)
                
                // 确保在主线程中执行UI操作
                if (isAdded && activity != null) {
                    activity?.runOnUiThread {
                        if (!isAdded || context == null || isFragmentDestroying) {
                            Log.d("VideoCall", "Fragment未附加或正在销毁，跳过UI操作")
                            return@runOnUiThread
                        }
                        
                        when (status) {
                            "ended" -> {
                                try {
                                    waitingDialog?.dismiss()
                                } catch (e: Exception) {
                                    Log.e("VideoCall", "关闭等待对话框时出错: ${e.message}")
                                }
                                
                                context?.let { ctx ->
                                    try {
                                        Toast.makeText(ctx, "Call ended", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("VideoCall", "显示Toast时出错: ${e.message}")
                                    }
                                }
                                
                                safePopBackStack()
                            }
                            "accepted" -> {
                                try {
                                    waitingDialog?.dismiss()
                                } catch (e: Exception) {
                                    Log.e("VideoCall", "关闭等待对话框时出错: ${e.message}")
                                }
                            }
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
}