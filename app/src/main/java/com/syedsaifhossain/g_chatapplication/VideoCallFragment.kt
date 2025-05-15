//package com.syedsaifhossain.g_chatapplication
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.SurfaceView
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding
//import io.agora.rtc2.*
//import io.agora.rtc2.video.VideoCanvas
//import io.agora.rtc2.ChannelMediaOptions
//
//class VideoCallFragment : Fragment() {
//
//    private var _binding: FragmentVideoCallBinding? = null
//    private val binding get() = _binding!!
//
//    private val appId = "01764965ef8f461197b67bb61a51ed30"
//    private val appCertificate = "d9a756176e49445a81c1af8f6051ebd1"
//    private val expirationTimeInSeconds = 3600
//    private val channelName = "gchatvideocall"
//    private var token = "007eJxTYFjC9dP0y6qanq1pnbdZV9y/faK7zXrrtI9vPaYYhoRsqY1XYDAwNDczsTQzTU2zSDMxMzS0NE8yM09KMjNMNDVMTTE28GRXy2gIZGQ4s6aehZEBAkF8Pob05IzEkrLMlNT85MScHAYGAEJ4JKw="
//    private val uid = 0
//    private var isJoined = false
//
//    private var agoraEngine: RtcEngine? = null
//    private var localSurfaceView: SurfaceView? = null
//    private var remoteSurfaceView: SurfaceView? = null
//
//    private val PERMISSION_REQ_ID = 22
//    private val REQUESTED_PERMISSIONS = arrayOf(
//        Manifest.permission.RECORD_AUDIO,
//        Manifest.permission.CAMERA
//    )
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        if (!checkSelfPermission()) {
//            requestPermissions(REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
//        } else {
//            initializeEngineAndJoin()
//        }
//
//        binding.joinCallButton.setOnClickListener { joinChannel() }
//        binding.endCallButton.setOnClickListener { leaveChannel() }
//    }
//
//    private fun checkSelfPermission(): Boolean {
//        return REQUESTED_PERMISSIONS.all {
//            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//
//    private fun initializeEngineAndJoin() {
//        generateToken()
//        setupVideoSDKEngine()
//    }
//
//    private fun generateToken() {
//        val tokenBuilder = RtcTokenBuilder2()
//        val timestamp = (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
//        token = tokenBuilder.buildTokenWithUid(
//            appId, appCertificate, channelName,
//            uid, RtcTokenBuilder2.Role.ROLE_PUBLISHER,
//            timestamp, timestamp
//        )
//    }
//
//    private fun setupVideoSDKEngine() {
//        try {
//            val config = RtcEngineConfig().apply {
//                mContext = requireContext().applicationContext
//                mAppId = appId
//                mEventHandler = mRtcEventHandler
//            }
//            agoraEngine = RtcEngine.create(config)
//            agoraEngine?.enableVideo()
//        } catch (e: Exception) {
//            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun setupLocalVideo() {
//        localSurfaceView = SurfaceView(requireContext())
//        binding.localVideoViewContainer.addView(localSurfaceView)
//        agoraEngine?.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
//    }
//
//    private fun setupRemoteVideo(uid: Int) {
//        remoteSurfaceView = SurfaceView(requireContext()).apply {
//            setZOrderMediaOverlay(true)
//        }
//        binding.remoteVideoViewContainer.addView(remoteSurfaceView)
//        agoraEngine?.setupRemoteVideo(VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
//    }
//
//    private fun joinChannel() {
//        if (!checkSelfPermission()) {
//            Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        setupLocalVideo()
//        localSurfaceView?.visibility = View.VISIBLE
//
//        val options = ChannelMediaOptions().apply {
//            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
//            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
//            publishMicrophoneTrack = true
//            publishCameraTrack = true
//        }
//
//        agoraEngine?.startPreview()
//        agoraEngine?.joinChannel(token, channelName, uid, options)
//    }
//
//    private fun leaveChannel() {
//        if (!isJoined) {
//            Toast.makeText(context, "Join a channel first", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        agoraEngine?.leaveChannel()
//        Toast.makeText(context, "You left the channel", Toast.LENGTH_SHORT).show()
//        localSurfaceView?.visibility = View.GONE
//        remoteSurfaceView?.visibility = View.GONE
//        isJoined = false
//    }
//
//    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
//        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
//            isJoined = true
//            Toast.makeText(context, "Joined channel: $channel", Toast.LENGTH_SHORT).show()
//        }
//
//        override fun onUserJoined(uid: Int, elapsed: Int) {
//            requireActivity().runOnUiThread { setupRemoteVideo(uid) }
//        }
//
//        override fun onUserOffline(uid: Int, reason: Int) {
//            requireActivity().runOnUiThread {
//                Toast.makeText(context, "User $uid offline", Toast.LENGTH_SHORT).show()
//                remoteSurfaceView?.visibility = View.GONE
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        agoraEngine?.stopPreview()
//        agoraEngine?.leaveChannel()
//        Thread {
//            RtcEngine.destroy()
//            agoraEngine = null
//        }.start()
//        _binding = null
//    }
//
//}



package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class VideoCallFragment : Fragment() {

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!

    private val appId = "01764965ef8f461197b67bb61a51ed30"
    private val token = "007eJxTYFjC9dP0y6qanq1pnbdZV9y/faK7zXrrtI9vPaYYhoRsqY1XYDAwNDczsTQzTU2zSDMxMzS0NE8yM09KMjNMNDVMTTE28GRXy2gIZGQ4s6aehZEBAkF8Pob05IzEkrLMlNT85MScHAYGAEJ4JKw="
    private val channelName = "gchat"

    private val PERMISSION_REQ_ID = 22
    private var mRtcEngine: RtcEngine? = null

    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.joinCallButton.setOnClickListener { joinChannel() }
        binding.endCallButton.setOnClickListener { leaveChannel() }

        if (checkPermissions()) {
            startVideoCalling()
        } else {
            requestPermissions(getRequiredPermissions(), PERMISSION_REQ_ID)
        }
    }

    private fun startVideoCalling() {
        initializeRtcEngine()
        enableVideo()
        setupLocalVideo()
        joinChannel()
    }

    private fun initializeRtcEngine() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = requireContext().applicationContext
                mAppId = appId
                mEventHandler = mRtcEventHandler
            }
            mRtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Error initializing RTC engine: ${e.message}")
        }
    }

    private fun enableVideo() {
        mRtcEngine?.apply {
            enableVideo()
            startPreview()
        }
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(requireContext())
        binding.localVideoViewContainer.addView(localSurfaceView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(requireContext()).apply {
            setZOrderMediaOverlay(true)
        }
        binding.remoteVideoViewContainer.addView(remoteSurfaceView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = true
            publishCameraTrack = true
        }
        mRtcEngine?.joinChannel(token, channelName, 0, options)
    }

    private fun leaveChannel() {
        mRtcEngine?.leaveChannel()
        showToast("You left the channel")
        localSurfaceView?.visibility = View.GONE
        remoteSurfaceView?.visibility = View.GONE
    }

    private fun checkPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID && checkPermissions()) {
            startVideoCalling()
        } else {
            showToast("Permissions not granted.")
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            requireActivity().runOnUiThread {
                showToast("Joined channel: $channel")
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            requireActivity().runOnUiThread {
                showToast("User joined: $uid")
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            requireActivity().runOnUiThread {
                showToast("User offline: $uid")
                remoteSurfaceView?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRtcEngine?.apply {
            stopPreview()
            leaveChannel()
        }
        RtcEngine.destroy()
        mRtcEngine = null
        _binding = null
    }
}