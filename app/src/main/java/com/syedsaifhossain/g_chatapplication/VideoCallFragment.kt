package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView // <--- Ensure this is imported for android.view.SurfaceView
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
import io.agora.rtc2.RtcEngineConfig // <--- Potentially useful for specific configurations later
import io.agora.rtc2.video.VideoCanvas

class VideoCallFragment : Fragment() {

    // Agora App ID এবং Token
    // !!! গুরুত্বপূর্ণ: TOKEN যদি null থাকে, তাহলে আপনার Agora প্রোজেক্টের App Certificate সেটিংস চেক করুন।
    //                 সাধারণত, সুরক্ষিত কলের জন্য একটি ভ্যালিড টোকেন প্রয়োজন।
    private val APP_ID = "01764965ef8f461197b67bb61a51ed30"
    private val TOKEN = "007eJxTYJjqey/W9NLXZ06l3VdijY/7FAuVft7L6Dq17tE1Nc83yYIKDAaG5mYmlmamqWkWaSZmhoaW5klm5klJZoaJpoapKcYGbQ16GQ2BjAyaRndYGRkgEMQXYEhPzkgsiS/LTEnNj09OzMlhYAAA+fUjww==" // যদি App Certificate চালু থাকে তবে এখানে আপনার Agora টোকেন বসাতে হবে
    private val CHANNEL_NAME = "gchat_video_call"

    private var agoraEngine: RtcEngine? = null
    private var isSpeakerOn = true
    private var isMicMuted = false
    private var isCameraFront = true

    // View Binding এর জন্য
    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!! // Null-safe অ্যাক্সেসের জন্য

    // প্রয়োজনীয় পারমিশনগুলো
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

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

        // --- সংশোধিত: onFirstLocalVideoFrame এর সিগনেচার ---
        override fun onFirstLocalVideoFrame(
            source: Constants.VideoSourceType, // Added 'source' parameter
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            Log.d("Agora", "First local video frame rendered: ${width}x${height}")
        }

        // --- সংশোধিত: onFirstRemoteVideoDecoded এর সিগনেচার (যদি ব্যবহৃত হয়, নিশ্চিত করুন) ---
        // আপনার কোডে এটি ছিল, তাই এর সিগনেচারও Agora v4 অনুযায়ী ঠিক করা উচিত
        override fun onFirstRemoteVideoDecoded(
            uid: Int,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            Log.d("Agora", "First remote video decoded: $uid, ${width}x${height}")
            // রিমোট ভিডিও সেটআপ করার জন্য এটিও একটি ভালো জায়গা
            // setupRemoteVideo(uid) // এটি onUserJoined থেকে কল করাই সাধারণত ভালো
        }

        override fun onError(err: Int) {
            requireActivity().runOnUiThread {
                Log.e("Agora", "Agora Error: $err")
                Toast.makeText(requireContext(), "Agora Error: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // View Binding ইনিশিয়ালাইজ করুন
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        val view = binding.root

        // কন্ট্রোল বাটনগুলোর জন্য ক্লিক লিসেনার সেট করুন
        binding.speakerOn.setOnClickListener { toggleSpeaker() }
        binding.switchCamera.setOnClickListener { switchCamera() }
        binding.mute.setOnClickListener { toggleMic() }
        binding.endCallButton.setOnClickListener { endCall() }

        // ব্যাক অ্যারো লিসেনার
        binding.videoCallBackArrow.setOnClickListener {
            Toast.makeText(requireContext(), "Back arrow clicked", Toast.LENGTH_SHORT).show()
            endCall()
        }

        // কন্টাক্ট যোগ করার লিসেনার
        binding.videoCallAddContact.setOnClickListener {
            Toast.makeText(requireContext(), "Add contact clicked", Toast.LENGTH_SHORT).show()
        }

        // বাটনগুলির প্রাথমিক UI অবস্থা সেট করুন
        updateSpeakerButton()
        updateMicButton()

        // পারমিশন রিকোয়েস্ট করুন এবং Agora ইনিশিয়ালাইজ করুন
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(requireActivity(), REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        } else {
            initializeAndJoinChannel()
        }

        return view
    }

    // পারমিশন চেক করার ফাংশন
    private fun checkPermissions(): Boolean {
        for (permission in REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            // For Android 12 (API 31) and above, if you target 31+, you might need BLUETOOTH_CONNECT for some features
            // This is just a note, not an error fix for your current problem
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && permission == Manifest.permission.BLUETOOTH_CONNECT) {
            //     if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            //         return false
            //     }
            // }
        }
        return true
    }

    // পারমিশন রিকোয়েস্টের ফলাফলের হ্যান্ডলার
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                initializeAndJoinChannel()
            } else {
                Toast.makeText(requireContext(), "Permissions not granted. Cannot start video call.", Toast.LENGTH_LONG).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    // Agora ইঞ্জিন ইনিশিয়ালাইজ করা এবং চ্যানেলে জয়েন করা
    private fun initializeAndJoinChannel() {
        try {
            // RtcEngine.create() ব্যবহার করার আগে RtcEngineConfig ব্যবহার করা ভালো
            val config = RtcEngineConfig()
            config.mContext = requireContext()
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING // বা Constants.CHANNEL_PROFILE_COMMUNICATION

            agoraEngine = RtcEngine.create(config)

            agoraEngine?.enableVideo()
            agoraEngine?.setEnableSpeakerphone(isSpeakerOn)

            setupLocalVideo()
            joinChannel()

        } catch (e: Exception) {
            Log.e("Agora", "Error initializing Agora: ${e.message}")
            Toast.makeText(requireContext(), "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // লোকাল ভিডিও সেটআপ করা
    private fun setupLocalVideo() {
        _binding?.localVideoViewContainer?.let { container ->
            // --- সংশোধিত: Standard SurfaceView তৈরি করুন, RtcEngine.createRendererView() নয় ---
            val surfaceView = SurfaceView(requireContext())
            // --- সংশোধিত: setZOrderMediaOverlay() লাইনটি সরানো হয়েছে ---
            container.addView(surfaceView)
            agoraEngine?.setupLocalVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0))
        }
    }

    // রিমোট ভিডিও সেটআপ করা
    private fun setupRemoteVideo(uid: Int) {
        _binding?.remoteVideoViewContainer?.let { container ->
            container.removeAllViews()

            // --- সংশোধিত: Standard SurfaceView তৈরি করুন, RtcEngine.createRendererView() নয় ---
            val surfaceView = SurfaceView(requireContext())
            container.addView(surfaceView)
            agoraEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid))
        }
    }

    // চ্যানেলে জয়েন করার ফাংশন
    private fun joinChannel() {
        agoraEngine?.joinChannel(TOKEN, CHANNEL_NAME, null, 0)
        Toast.makeText(requireContext(), "Joining channel: $CHANNEL_NAME", Toast.LENGTH_SHORT).show()
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
                it.setImageResource(R.drawable.speakeron) // আপনার প্রজেক্টে এই ড্রয়েবলটি থাকতে হবে
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
                it.setImageResource(R.drawable.micoff) // আপনার প্রজেক্টে এই ড্রয়েবলটি থাকতে হবে
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
        RtcEngine.destroy() // এটি RTC ইঞ্জিনকে সম্পূর্ণরূপে রিলিজ করে
        agoraEngine = null
        _binding = null // মেমরি লিক এড়ানোর জন্য গুরুত্বপূর্ণ
    }
}