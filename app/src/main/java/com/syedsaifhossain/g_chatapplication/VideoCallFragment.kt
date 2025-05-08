package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas

class VideoCallFragment : Fragment() {

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!
    private lateinit var rtcEngine: RtcEngine
    private val appId = "8cf64d493e8b460f91b10bf531f6d678"
    private val channelName = "testChannel"
    private val token: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAgoraEngine()
        setupVideo()
        joinChannel()

        binding.endCallButton.setOnClickListener {
            rtcEngine.leaveChannel()
            parentFragmentManager.popBackStack()
        }
    }

    private fun initAgoraEngine() {
        rtcEngine = RtcEngine.create(requireContext(), appId, object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Log.d("Agora", "Joined video channel: $channel")
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                activity?.runOnUiThread { setupRemoteVideo(uid) }
            }
        })
    }

    private fun setupVideo() {
        rtcEngine.enableVideo()

        val localSurface = RtcEngine.CreateRendererView(requireContext())
        localSurface.setZOrderMediaOverlay(true)
        binding.localVideoViewContainer.addView(localSurface)

        rtcEngine.setupLocalVideo(VideoCanvas(localSurface, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        val remoteSurface = RtcEngine.CreateRendererView(requireContext())
        binding.remoteVideoViewContainer.addView(remoteSurface)

        rtcEngine.setupRemoteVideo(VideoCanvas(remoteSurface, VideoCanvas.RENDER_MODE_HIDDEN, uid))
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