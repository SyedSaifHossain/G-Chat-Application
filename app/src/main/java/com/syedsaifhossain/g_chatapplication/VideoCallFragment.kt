package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import android.opengl.GLSurfaceView

class VideoCallFragment : Fragment() {

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!

    private var rtcEngine: RtcEngine? = null
    private var isMuted = false
    private var isVideoEnabled = true
    private var isSpeakerOn = true
    private val agoraAppId = "8cf64d493e8b460f91b10bf531f6d678"
    private var channelName: String? = null // Managed by Firebase
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private lateinit var callRef: DatabaseReference
    private var callListener: ValueEventListener? = null
    private var callInitiated = false
    private var remoteUserId: String? = null
    private var currentUserUid: String = "user_${System.currentTimeMillis()}" // Replace with actual user ID

    companion object {
        private const val TAG = "VideoCallFragment"
        private const val PERMISSION_REQUEST_CODE = 123
        private const val CALLS_NODE = "calls"
        private const val CALL_STATUS_RINGING = "ringing"
        private const val CALL_STATUS_ACCEPTED = "accepted"
        private const val CALL_STATUS_REJECTED = "rejected"
        private const val CALL_STATUS_ENDED = "ended"
        private const val KEY_CALLER_ID = "callerId"
        private const val KEY_CALLEE_ID = "calleeId"
        private const val KEY_CHANNEL_NAME = "channelName"
        private const val KEY_STATUS = "status"

        fun newInstance(remoteUserId: String): VideoCallFragment {
            val fragment = VideoCallFragment()
            val args = Bundle()
            args.putString("remoteUserId", remoteUserId)
            fragment.arguments = args
            return fragment
        }

        fun newInstanceForIncomingCall(callId: String, callerId: String): VideoCallFragment {
            val fragment = VideoCallFragment()
            val args = Bundle()
            args.putString("callId", callId)
            args.putString("callerId", callerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

        remoteUserId = arguments?.getString("remoteUserId")
        val existingCallId = arguments?.getString("callId")
        val callerId = arguments?.getString("callerId")

        if (hasPermissions()) {
            if (existingCallId != null) {
                channelName = existingCallId
                remoteUserId = callerId // Set remoteUserId for UI
                joinCall(existingCallId)
                setupUIForExistingCall()
            } else if (remoteUserId != null) {
                initiateCall(remoteUserId!!)
                setupUIForNewCall()
            } else {
                // Handle no call to join or initiate
            }
            setupControlButtons()
        } else {
            requestPermissions()
        }
    }

    private fun setupUIForNewCall() {
        binding.userName.text = remoteUserId ?: "Calling..."
        binding.userStatus.text = "Ringing..."
        // Potentially disable some buttons
    }

    private fun setupUIForExistingCall() {
        binding.userName.text = remoteUserId ?: "In Call"
        binding.userStatus.text = "Connected"
    }

    private fun setupControlButtons() {
        binding.micButton.setOnClickListener { toggleMute() }
        binding.cameraButton.setOnClickListener { toggleVideo() }
        binding.speakerButton.setOnClickListener { toggleSpeaker() }
        binding.endCallButton.setOnClickListener { endCall() }
        binding.switchCameraButton.setOnClickListener { rtcEngine?.switchCamera() }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        rtcEngine?.muteLocalAudioStream(isMuted)
        binding.micButton.setImageResource(if (isMuted) R.drawable.micoff else R.drawable.micon)
        binding.micButton.alpha = if (isMuted) 0.5f else 1.0f
    }

    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        rtcEngine?.muteLocalVideoStream(!isVideoEnabled)
        binding.localVideoView.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
        // Update speaker button UI
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun initializeAgoraEngine() {
        if (rtcEngine == null && channelName != null) {
            try {
                val mRtcEventHandler = object : IRtcEngineEventHandler() {
                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        activity?.runOnUiThread { setupRemoteVideo(uid) }
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        activity?.runOnUiThread { binding.remoteVideoView.removeAllViews() }
                    }

                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        Log.i(TAG, "onJoinChannelSuccess: $channel, uid: $uid")
                    }

                    override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                        Log.i(TAG, "onRemoteVideoStateChanged: uid=$uid, state=$state, reason=$reason")
                    }

                    override fun onError(err: Int) {
                        Log.e(TAG, "Agora SDK Error: $err, ${RtcEngine.getErrorDescription(err)}")
                    }
                }
                rtcEngine = RtcEngine.create(requireContext().applicationContext, agoraAppId, mRtcEventHandler)
                rtcEngine?.enableVideo()
                rtcEngine?.setVideoEncoderConfiguration(
                    VideoEncoderConfiguration(
                        VideoEncoderConfiguration.VideoDimensions(640, 480),
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                        VideoEncoderConfiguration.STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                    )
                )
                rtcEngine?.joinChannel(null, channelName, null, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Agora engine: ${e.message}", e)
                // Handle initialization error
            }
        }
    }

    private fun setupLocalVideo() {
        activity?.runOnUiThread {
            if (rtcEngine == null) return@runOnUiThread
            val surfaceView = RtcEngine.CreateRendererView(requireContext()) as GLSurfaceView
            surfaceView.setEGLContextClientVersion(2)
            surfaceView.setZOrderMediaOverlay(true)
            binding.localVideoView.removeAllViews()
            binding.localVideoView.addView(surfaceView)
            rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        activity?.runOnUiThread {
            if (rtcEngine == null) return@runOnUiThread
            val surfaceView = RtcEngine.CreateRendererView(requireContext()) as GLSurfaceView
            surfaceView.setEGLContextClientVersion(2)
            surfaceView.setZOrderMediaOverlay(true)
            binding.remoteVideoView.removeAllViews()
            binding.remoteVideoView.addView(surfaceView)
            rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
        }
    }

    private fun hasPermissions(): Boolean {
        val context = requireContext()
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            if (arguments?.getString("callId") != null) {
                joinCall(arguments?.getString("callId")!!)
                setupUIForExistingCall()
            } else if (remoteUserId != null) {
                initiateCall(remoteUserId!!)
                setupUIForNewCall()
            }
        } else {
            // Handle permission denial
        }
    }

    private fun initiateCall(calleeId: String) {
        val callId = "${currentUserUid}_${calleeId}_${System.currentTimeMillis()}"
        channelName = callId
        callInitiated = true
        callRef = firebaseDatabase.getReference(CALLS_NODE).child(callId)
        val callInfo = mapOf(
            KEY_CALLER_ID to currentUserUid,
            KEY_CALLEE_ID to calleeId,
            KEY_CHANNEL_NAME to callId,
            KEY_STATUS to CALL_STATUS_RINGING
        )
        callRef.setValue(callInfo)
            .addOnSuccessListener {
                initializeAgoraEngine()
                setupLocalVideo()
                listenForCallStatus(callId)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to initiate call on Firebase", it)
                // Handle failure
            }
    }

    private fun joinCall(callId: String) {
        channelName = callId
        initializeAgoraEngine()
        setupLocalVideo()
        listenForCallStatus(callId)
    }

    private fun listenForCallStatus(callId: String) {
        callRef = firebaseDatabase.getReference(CALLS_NODE).child(callId)
        callListener = callRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val callData = snapshot.value as? Map<String, Any>
                when (callData?.get(KEY_STATUS)) {
                    CALL_STATUS_ACCEPTED -> {
                        // Call accepted, Agora engine should already be initialized and joined
                        binding.userStatus.text = "Connected"
                    }
                    CALL_STATUS_REJECTED -> {
                        // Handle call rejection
                        endCallInternal()
                        // Navigate back or show a message
                    }
                    CALL_STATUS_ENDED -> {
                        // Handle call ended
                        endCallInternal()
                        // Navigate back or show a message
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase call status listener cancelled", error.toException())
            }
        })
    }

    private fun endCall() {
        callRef?.child(KEY_STATUS)?.setValue(CALL_STATUS_ENDED)
        endCallInternal()
    }

    private fun endCallInternal() {
        rtcEngine?.leaveChannel()
        rtcEngine?.disableVideo()
        RtcEngine.destroy() // Correct way to destroy the engine
        rtcEngine = null
        callListener?.let { callRef?.removeEventListener(it) }
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        endCallInternal()
    }
}