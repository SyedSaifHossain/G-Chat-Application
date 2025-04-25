package com.syedsaifhossain.g_chatapplication

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import android.Manifest

class VideoCallFragment : Fragment() {

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!

    private lateinit var rtcEngine: RtcEngine
    private var isMuted = false
    private var isVideoEnabled = true
    private var isSpeakerOn = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasPermissions()) {
            setupUI()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),

                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                1
            )
        }
    }

    private fun setupUI() {
        initializeAgoraEngine()

        binding.userName.text = "User Name"
        binding.userStatus.text = "View Her Recent Activity"

        binding.micButton.setOnClickListener {
            isMuted = !isMuted
            rtcEngine.muteLocalAudioStream(isMuted)
            // Update UI icon
        }

        binding.cameraButton.setOnClickListener {
            isVideoEnabled = !isVideoEnabled
            rtcEngine.muteLocalVideoStream(!isVideoEnabled)
            binding.localVideoView.visibility = if (isVideoEnabled) View.VISIBLE else View.GONE
        }

        binding.speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            rtcEngine.setEnableSpeakerphone(isSpeakerOn)
        }

        binding.endCallButton.setOnClickListener {
            rtcEngine.leaveChannel()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.switchCameraButton.setOnClickListener {
            rtcEngine.switchCamera()
        }
    }

    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(requireContext(), "8cf64d493e8b460f91b10bf531f6d678", object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    requireActivity().runOnUiThread {
                        setupRemoteVideo(uid)
                    }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    requireActivity().runOnUiThread {
                        // Handle remote user leaving
                        binding.remoteVideoView.removeAllViews()
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.localVideoView.post {
            setupLocalVideo()
        }

        rtcEngine.joinChannel(null, "channelName", "", 0)
    }



    private fun setupLocalVideo() {
        requireActivity().runOnUiThread {
            val surfaceView = RtcEngine.CreateRendererView(requireContext())
            surfaceView.setZOrderMediaOverlay(true) // optional
            binding.localVideoView.addView(surfaceView)
            rtcEngine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        }
    }


    private fun setupRemoteVideo(uid: Int) {
        requireActivity().runOnUiThread {
            val surfaceView = RtcEngine.CreateRendererView(requireContext())
            binding.remoteVideoView.removeAllViews()
            binding.remoteVideoView.addView(surfaceView)
            rtcEngine.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        }
    }


    private fun hasPermissions(): Boolean {
        val context = requireContext()
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setupUI()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rtcEngine.leaveChannel()
        RtcEngine.destroy()
        _binding = null
    }
}