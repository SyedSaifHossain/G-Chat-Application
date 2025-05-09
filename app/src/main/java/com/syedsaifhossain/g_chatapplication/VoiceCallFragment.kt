package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVoiceCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine

class VoiceCallFragment : Fragment() {


    private var _binding: FragmentVoiceCallBinding? = null
    private val binding get() = _binding!!

    private lateinit var rtcEngine: RtcEngine
    private val appId = "8cf64d493e8b460f91b10bf531f6d678" // Replace with your actual App ID
    private val token: String? = null       // Use null if not using token
    private val channelName = "testChannel" // Unique per conversation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAgoraEngine()
        joinChannel()

        binding.endCallButton.setOnClickListener {
            rtcEngine.leaveChannel()
            parentFragmentManager.popBackStack()
        }
    }

    private fun initAgoraEngine() {
        rtcEngine = RtcEngine.create(requireContext(), appId, object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Log.d("Agora", "Joined channel: $channel")
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Log.d("Agora", "User joined: $uid")
                activity?.runOnUiThread {
                    binding.callStatus.text = "Connected"
                }
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Log.d("Agora", "User offline: $uid")
                activity?.runOnUiThread {
                    binding.callStatus.text = "User left"
                }
            }
        })
    }

    private fun joinChannel() {
        rtcEngine.joinChannel(token, channelName, "", 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rtcEngine.leaveChannel()
        RtcEngine.destroy()
        _binding = null
    }
}