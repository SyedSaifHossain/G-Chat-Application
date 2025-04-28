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
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import android.opengl.GLSurfaceView

class VideoCallFragment : Fragment() {

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!

    private var rtcEngine: RtcEngine? = null // Make RtcEngine nullable and handle lifecycle
    private var isMuted = false
    private var isVideoEnabled = true
    private var isSpeakerOn = true
    private val agoraAppId = "8cf64d493e8b460f91b10bf531f6d678"
    private val channelName = "your_channel_name"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeAgoraEngine()
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

        if (hasPermissions()) {
            setupUI()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }


    private fun setupUI() {
        binding.userName.text = "User Name"
        binding.userStatus.text = "View Her Recent Activity"

        binding.micButton.setOnClickListener {
            isMuted = !isMuted
            rtcEngine?.muteLocalAudioStream(isMuted)
            binding.micButton.setImageResource(if (isMuted) R.drawable.micoff else R.drawable.micon)
            binding.micButton.alpha = if (isMuted) 0.5f else 1.0f
        }

        binding.cameraButton.setOnClickListener {
            isVideoEnabled = !isVideoEnabled
            rtcEngine?.muteLocalVideoStream(!isVideoEnabled)

            binding.localVideoView.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
        }

        binding.speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
            // You might want to change the speaker button's icon based on the state
        }

        binding.endCallButton.setOnClickListener {
            leaveChannelAndFinish()
        }

        binding.switchCameraButton.setOnClickListener {
            rtcEngine?.switchCamera()
        }

        // Ensure local video is set up after the view is ready
        binding.localVideoView.post {
            setupLocalVideo()
        }
    }


    private fun initializeAgoraEngine() {
        if (rtcEngine == null) {
            try {
                val mRtcEventHandler = object : IRtcEngineEventHandler() {
                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        activity?.runOnUiThread {
                            setupRemoteVideo(uid)
                        }
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        activity?.runOnUiThread {
                            binding.remoteVideoView.removeAllViews()
                        }
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
                        VideoEncoderConfiguration.VideoDimensions(640, 480), // Using the constructor
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                        VideoEncoderConfiguration.STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                    )
                )
                rtcEngine?.joinChannel(null, channelName, null, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Agora engine: ${e.message}", e)
                // Handle initialization error (e.g., show a message to the user)
            }
        }
    }

    private fun setupLocalVideo() {


        val surfaceView = RtcEngine.CreateRendererView(requireContext()) as GLSurfaceView // Cast to GLSurfaceView
        surfaceView.setEGLContextClientVersion(2) // Or try 3 if your device supports it
        surfaceView.setZOrderMediaOverlay(true)

        activity?.runOnUiThread {
            if (rtcEngine == null) return@runOnUiThread
            val surfaceView = RtcEngine.CreateRendererView(requireContext())
            surfaceView.setZOrderMediaOverlay(true)
            binding.localVideoView.removeAllViews() // Ensure only one local view
            binding.localVideoView.addView(surfaceView)
            rtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(requireContext()) as GLSurfaceView // Cast to GLSurfaceView
        surfaceView.setEGLContextClientVersion(2) // Or try 3 if your device supports it
        surfaceView.setZOrderMediaOverlay(true)

        activity?.runOnUiThread {
            if (rtcEngine == null) return@runOnUiThread
            val surfaceView = RtcEngine.CreateRendererView(requireContext())
            binding.remoteVideoView.removeAllViews() // Ensure only one remote view
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
            initializeAgoraEngine()
            setupUI()
        } else {
            // Handle permission denial (e.g., show a message)
        }
    }

    private fun leaveChannelAndFinish() {
        rtcEngine?.leaveChannel()
        // Consider destroying the engine only when the fragment is destroyed or the app exits
        // rtcEngine?.destroy()
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Do NOT destroy the RtcEngine here if you might reuse it in other parts of the app.
        // If this is the only video call screen, you can destroy it here.
        // if (rtcEngine != null) {
        //     RtcEngine.destroy()
        //     rtcEngine = null
        // }
    }

    companion object {
        private const val TAG = "VideoCallFragment"
        private const val PERMISSION_REQUEST_CODE = 123
    }

}