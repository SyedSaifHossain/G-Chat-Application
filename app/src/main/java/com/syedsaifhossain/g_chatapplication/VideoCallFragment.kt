package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class VideoCallFragment : Fragment() {

    // Agora App ID (still needed for client-side SDK initialization)
    private val APP_ID = "01764965ef8f461197b67bb61a51ed30"
    private val CHANNEL_NAME = "gchat_video_call"

    private var agoraEngine: RtcEngine? = null
    private var isSpeakerOn = true
    private var isMicMuted = false
    private var isCameraFront = true

    // View Binding এর জন্য
    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!! // Null-safe অ্যাক্সেসের জন্য

    // প্রয়োজনীয় পারমিশনগুলো
    private val PERMISSION_REQ_ID = 22

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    // Agora ইভেন্ট হ্যান্ডলার
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user offline: $uid", Toast.LENGTH_SHORT).show()
                _binding?.remoteVideoViewContainer?.removeAllViews()
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            requireActivity().runOnUiThread {
                Log.d("Agora", "Joined channel successfully: $channel, uid: $uid")
                Toast.makeText(requireContext(), "Joined channel: $channel", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFirstLocalVideoFrame(
            source: Constants.VideoSourceType,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            Log.d("Agora", "First local video frame rendered: ${width}x${height}")
        }

        override fun onFirstRemoteVideoDecoded(
            uid: Int,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            Log.d("Agora", "First remote video decoded: $uid, ${width}x${height}")
        }

        override fun onError(err: Int) {
            requireActivity().runOnUiThread {
                Log.e("Agora", "Agora Error: $err")
                val errorMessage = when(err) {
                    Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                    Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                    Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                    Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                    else -> "Unknown Agora Error: $err"
                }
                Toast.makeText(requireContext(), "Agora Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions
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

        // পারমিশন রিকোয়েস্ট করুন এবং Agora ইনিশিয়ালাইজ করুন
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
                Manifest.permission.READ_PHONE_STATE,
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

    // টোকেন এনে চ্যানেলে জয়েন করার ফাংশন (এখন Firebase Cloud Functions ব্যবহার করে)
    private fun fetchTokenAndJoinChannel() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
            Log.e("AgoraToken", "User not authenticated for token fetch. Aborting call setup.")
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        // Firebase UID কে Agora UID তে রূপান্তর (Agora UID একটি Int হয়)
        // hashCode() ব্যবহার করে একটি Int পাওয়া যায়, তবে এটি সবসময় অনন্য নাও হতে পারে।
        // যদি আপনার অ্যাপে সুনির্দিষ্ট ইউজার আইডি প্রয়োজন হয়, তাহলে আপনার ব্যাকএন্ডে একটি ম্যাপিং সিস্টেম ব্যবহার করতে পারেন।
        val agoraUid = currentUser.uid.hashCode() and 0xFFFFFFFF.toInt() // Ensure positive integer

        val data = hashMapOf(
            "channelName" to CHANNEL_NAME,
            "agoraUid" to agoraUid
        )

        functions
            .getHttpsCallable("generateAgoraToken") // আপনার Cloud Function এর নাম
            .call(data)
            .addOnSuccessListener { result ->
                // Explicitly cast result.data to Map<String, Any?> for better type inference
                val responseData = result.data as? Map<String, Any?>
                val token = responseData?.get("token") as? String
                if (token != null) {
                    Log.d("AgoraToken", "Fetched RTC token from Firebase Function: $token")
                    initializeAndJoinChannel(token)
                } else {
                    Log.e("AgoraToken", "Failed to get token from Firebase Function result. Result: ${result.data}")
                    Toast.makeText(requireContext(), "Failed to get call token from server.", Toast.LENGTH_LONG).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            .addOnFailureListener { e ->
                Log.e("AgoraToken", "Error calling Firebase Function: ${e.message}", e)
                Toast.makeText(requireContext(), "Error fetching token: ${e.message}", Toast.LENGTH_LONG).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
    }

    // Agora ইঞ্জিন ইনিশিয়ালাইজ করা এবং চ্যানেলে জয়েন করা (এখন টোকেন গ্রহণ করে)
    private fun initializeAndJoinChannel(token: String) {
        try {
            val config = RtcEngineConfig()
            config.mContext = requireContext()
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

            agoraEngine = RtcEngine.create(config)
            Log.d("AgoraInit", "Agora RtcEngine created successfully.")

            agoraEngine?.enableVideo()
            agoraEngine?.setEnableSpeakerphone(isSpeakerOn)

            setupLocalVideo()
            // এখন প্রাপ্ত টোকেন দিয়ে চ্যানেলে জয়েন করুন
            agoraEngine?.joinChannel(token, CHANNEL_NAME, null, 0)
            Toast.makeText(requireContext(), "Joining channel: $CHANNEL_NAME", Toast.LENGTH_SHORT).show()
            Log.d("AgoraInit", "Join channel initiated for: $CHANNEL_NAME with token.")

        } catch (e: Exception) {
            Log.e("AgoraInit", "Error initializing Agora: ${e.message}", e)
            Toast.makeText(requireContext(), "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // লোকাল ভিডিও সেটআপ করা
    private fun setupLocalVideo() {
        _binding?.localVideoViewContainer?.let { container ->
            val surfaceView = SurfaceView(requireContext())
            container.addView(surfaceView)
            agoraEngine?.setupLocalVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0))
        }
    }

    // রিমোট ভিডিও সেটআপ করা
    private fun setupRemoteVideo(uid: Int) {
        _binding?.remoteVideoViewContainer?.let { container ->
            container.removeAllViews()
            val surfaceView = SurfaceView(requireContext())
            container.addView(surfaceView)
            agoraEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid))
        }
    }

    // স্পিকার টগল করার ফাংশন
    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
        updateSpeakerButton()
        Toast.makeText(requireContext(), "Speaker " + (if (isSpeakerOn) "On" else "Off"), Toast.LENGTH_SHORT).show()
    }

    // স্পিকার বাটনের UI আপডেট করা
    private fun updateSpeakerButton() {
        _binding?.speakerOn?.let {
            if (isSpeakerOn) {
                it.setImageResource(R.drawable.speakeron)
            } else {
                it.setImageResource(R.drawable.speakeron)
            }
        }
    }

    // মাইক্রোফোন টগল করার ফাংশন
    private fun toggleMic() {
        isMicMuted = !isMicMuted
        agoraEngine?.muteLocalAudioStream(isMicMuted)
        updateMicButton()
        Toast.makeText(requireContext(), "Mic " + (if (isMicMuted) "Muted" else "Unmuted"), Toast.LENGTH_SHORT).show()
    }

    // মাইক্রোফোন বাটনের UI আপডেট করা
    private fun updateMicButton() {
        _binding?.mute?.let {
            if (isMicMuted) {
                it.setImageResource(R.drawable.micoff)
            } else {
                it.setImageResource(R.drawable.micon)
            }
        }
    }

    // ক্যামেরা সুইচ করার ফাংশন
    private fun switchCamera() {
        agoraEngine?.switchCamera()
        isCameraFront = !isCameraFront
        Toast.makeText(requireContext(), "Switched camera", Toast.LENGTH_SHORT).show()
    }

    // কল শেষ করার ফাংশন
    private fun endCall() {
        agoraEngine?.leaveChannel()
        _binding?.localVideoViewContainer?.removeAllViews()
        _binding?.remoteVideoViewContainer?.removeAllViews()
        Toast.makeText(requireContext(), "Call ended", Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        agoraEngine?.leaveChannel()
        RtcEngine.destroy()
        agoraEngine = null
        _binding = null
    }
}