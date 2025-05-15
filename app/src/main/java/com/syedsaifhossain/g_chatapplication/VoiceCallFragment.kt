package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVoiceCallBinding
import io.agora.rtc2.*
import io.agora.rtc2.RtcEngineConfig

class VoiceCallFragment : Fragment() {

    private var _binding: FragmentVoiceCallBinding? = null
    private val binding get() = _binding!!

    private var rtcEngine: RtcEngine? = null

    private val appId = "8cf64d493e8b460f91b10bf531f6d678" // Replace with your Agora App ID
    private val token: String? = null // Set this if you are using token authentication
    private val channelName = "testChannel"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeAgoraEngine()
        joinChannel()

        binding.endCallButton.setOnClickListener {
            leaveChannel()
            parentFragmentManager.popBackStack()
        }
    }

    private fun initializeAgoraEngine() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = requireContext().applicationContext
                mAppId = appId
                mEventHandler = rtcEventHandler
            }
            rtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            Log.e("VoiceCallFragment", "RtcEngine init error: ${e.message}")
        }
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = true
            publishCameraTrack = false // Voice-only call
        }

        rtcEngine?.joinChannel(token, channelName, 0, options)
    }

    private fun leaveChannel() {
        rtcEngine?.leaveChannel()
        binding.callStatus.text = "Disconnected"
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d("Agora", "Joined channel: $channel")
            requireActivity().runOnUiThread {
                binding.callStatus.text = "Connected to $channel"
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("Agora", "User joined: $uid")
            requireActivity().runOnUiThread {
                binding.callStatus.text = "User $uid joined"
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("Agora", "User offline: $uid")
            requireActivity().runOnUiThread {
                binding.callStatus.text = "User $uid left"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        _binding = null
    }
}