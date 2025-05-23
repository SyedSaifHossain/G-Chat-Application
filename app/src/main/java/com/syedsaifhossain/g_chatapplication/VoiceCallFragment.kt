package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVoiceCallBinding // Ensure this matches your XML file name
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class VoiceCallFragment : Fragment() {

    // Agora App ID (still needed for client-side SDK initialization)
    private val APP_ID = "01764965ef8f461197b67bb61a51ed30" // Replace with your actual Agora App ID
    private val CHANNEL_NAME = "gchat_voice_call" // Unique channel name for voice calls

    private var agoraEngine: RtcEngine? = null
    private var isSpeakerOn = true // Default to speaker on for voice calls
    private var isMicMuted = false

    // View Binding for the layout
    private var _binding: FragmentVoiceCallBinding? = null
    private val binding get() = _binding!! // Null-safe access

    // Required permissions for voice call
    private val PERMISSION_REQ_ID = 22
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT // For Bluetooth headsets on Android S+
        )
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    // Agora event handler
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        /**
         * Occurs when a remote user joins the channel.
         * Displays a toast and logs the user ID.
         */
        override fun onUserJoined(uid: Int, elapsed: Int) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user joined: $uid", Toast.LENGTH_SHORT).show()
                Log.d("AgoraVoice", "Remote user joined: $uid")
                // For voice calls, no video setup is needed here.
                // You might update UI to show user is connected.
            }
        }

        /**
         * Occurs when a remote user leaves the channel.
         * Displays a toast and logs the user ID.
         */
        override fun onUserOffline(uid: Int, reason: Int) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Remote user offline: $uid", Toast.LENGTH_SHORT).show()
                Log.d("AgoraVoice", "Remote user offline: $uid, reason: $reason")
                // Update UI to reflect user leaving (e.g., change status, remove from participant list)
            }
        }

        /**
         * Occurs when the local user successfully joins a channel.
         * Displays a toast and logs the channel and user ID.
         */
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            requireActivity().runOnUiThread {
                Log.d("AgoraVoice", "Joined channel successfully: $channel, uid: $uid")
                Toast.makeText(requireContext(), "Joined channel: $channel", Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Occurs when a general error happens in the Agora SDK.
         * Displays a detailed toast message for common errors.
         */
        override fun onError(err: Int) {
            requireActivity().runOnUiThread {
                Log.e("AgoraVoice", "Agora Error: $err")
                val errorMessage = when(err) {
                    Constants.ERR_INVALID_APP_ID -> "Invalid App ID. Please check your Agora App ID."
                    Constants.ERR_INVALID_TOKEN -> "Invalid or expired token. Generate a new token if required."
                    Constants.ERR_JOIN_CHANNEL_REJECTED -> "Join channel rejected. Check channel name or user limits."
                    Constants.ERR_DECRYPTION_FAILED -> "Decryption failed (check encryption settings if used)."
                    Constants.ERR_NO_PERMISSION -> "No audio recording permission."
                    // Removed Constants.ERR_ADM_INIT as it's not a direct constant in recent SDK versions
                    else -> "Unknown Agora Error: $err"
                }
                Toast.makeText(requireContext(), "Agora Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Called when the fragment is first created.
     * Initializes Firebase authentication and functions instances.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the layout, sets up click listeners, and initiates permission checks.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("VoiceCallFragment", "onCreateView called.")
        // Inflate the layout using View Binding
        _binding = FragmentVoiceCallBinding.inflate(inflater, container, false)
        val view = binding.root

        // Set up click listeners for UI elements
        binding.speakerOn.setOnClickListener { toggleSpeaker() }
        binding.mute.setOnClickListener { toggleMic() }
        binding.endCallButton.setOnClickListener { endCall() }

        binding.voiceCallBackArrow.setOnClickListener {
            Toast.makeText(requireContext(), "Back arrow clicked", Toast.LENGTH_SHORT).show()
            endCall() // End call when back arrow is clicked
        }

        binding.videoCallAddContact.setOnClickListener {
            Toast.makeText(requireContext(), "Add contact clicked", Toast.LENGTH_SHORT).show()
        }

        // Handle the 'videoCall' button - for voice call fragment, it's just a toast
        binding.videoCall.setOnClickListener {
            Toast.makeText(requireContext(), "This is a voice call. Video feature not active here.", Toast.LENGTH_SHORT).show()
        }

        // Update initial UI states of speaker and mic buttons
        updateSpeakerButton()
        updateMicButton()

        // Check and request permissions, then fetch token and join channel
        if (!checkPermissions()) {
            Log.d("PermissionDebug", "Permissions not granted, requesting...")
            requestPermissions()
        } else {
            Log.d("PermissionDebug", "Permissions already granted, fetching token and initializing channel.")
            fetchTokenAndJoinChannel()
        }

        return view
    }

    /**
     * Checks if all required permissions are granted.
     * @return true if all permissions are granted, false otherwise.
     */
    private fun checkPermissions(): Boolean {
        Log.d("PermissionDebug", "Checking permissions...")
        for (permission in REQUIRED_PERMISSIONS) {
            val status = ContextCompat.checkSelfPermission(requireContext(), permission)
            Log.d("PermissionDebug", "Permission $permission status: ${if (status == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
            if (status != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        Log.d("PermissionDebug", "All permissions checked and granted.")
        return true
    }

    /**
     * Requests missing permissions from the user.
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, PERMISSION_REQ_ID)
    }

    /**
     * Callback for the result of requesting permissions.
     * If all permissions are granted, fetches the token and joins the channel.
     * Otherwise, displays a toast and navigates back.
     */
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
                Log.e("PermissionDebug", "Not all permissions granted. Cannot start voice call.")
                Toast.makeText(requireContext(), "Permissions not granted. Cannot start voice call.", Toast.LENGTH_LONG).show()
                requireActivity().onBackPressedDispatcher.onBackPressed() // Go back if permissions are denied
            }
        } else {
            Log.d("PermissionDebug", "Unknown request code: $requestCode")
        }
    }

    /**
     * Fetches the Agora RTC token from Firebase Cloud Functions and then joins the channel.
     * Ensures the user is authenticated before attempting to fetch a token.
     */
    private fun fetchTokenAndJoinChannel() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated. Please log in.", Toast.LENGTH_LONG).show()
            Log.e("AgoraToken", "User not authenticated for token fetch. Aborting call setup.")
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        // Convert Firebase UID to an Agora UID (Int).
        // Using hashCode() might lead to collisions; for production, consider a more robust mapping.
        val agoraUid = currentUser.uid.hashCode() and 0xFFFFFFFF.toInt() // Ensure positive integer

        val data = hashMapOf(
            "channelName" to CHANNEL_NAME,
            "agoraUid" to agoraUid
        )

        functions
            .getHttpsCallable("generateAgoraToken") // Your Cloud Function name
            .call(data)
            .addOnSuccessListener { result ->
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

    /**
     * Initializes the Agora RtcEngine and joins the specified channel using the provided token.
     * Sets up the engine for audio-only communication.
     */
    private fun initializeAndJoinChannel(token: String) {
        try {
            val config = RtcEngineConfig()
            config.mContext = requireContext().applicationContext // Use application context
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            // Set channel profile to COMMUNICATION for one-to-one or group voice calls
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

            agoraEngine = RtcEngine.create(config)
            Log.d("AgoraInit", "Agora RtcEngine created successfully for voice call.")

            // Enable audio only, explicitly disable video
            agoraEngine?.enableAudio()
            agoraEngine?.disableVideo() // Ensure video is disabled for voice calls

            // Set initial speakerphone state
            agoraEngine?.setEnableSpeakerphone(isSpeakerOn)

            // Join the channel with the fetched token
            agoraEngine?.joinChannel(token, CHANNEL_NAME, null, 0)
            Toast.makeText(requireContext(), "Joining voice channel: $CHANNEL_NAME", Toast.LENGTH_SHORT).show()
            Log.d("AgoraInit", "Join voice channel initiated for: $CHANNEL_NAME with token.")

        } catch (e: Exception) {
            Log.e("AgoraInit", "Error initializing Agora for voice call: ${e.message}", e)
            Toast.makeText(requireContext(), "Error initializing Agora: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Toggles the speakerphone on or off.
     */
    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        agoraEngine?.setEnableSpeakerphone(isSpeakerOn)
        updateSpeakerButton()
        Toast.makeText(requireContext(), "Speaker " + (if (isSpeakerOn) "On" else "Off"), Toast.LENGTH_SHORT).show()
    }

    /**
     * Updates the speaker button icon based on the current speakerphone state.
     */
    private fun updateSpeakerButton() {
        _binding?.speakerOn?.let {
            if (isSpeakerOn) {
                it.setImageResource(R.drawable.speakeron) // Assuming 'speakeron' is the icon for speaker enabled
            } else {
                it.setImageResource(R.drawable.speakeron) // Assuming 'speakeroff' is the icon for speaker disabled
                // Make sure you have a 'speakeroff' drawable if needed
            }
        }
    }

    /**
     * Toggles the microphone mute state.
     */
    private fun toggleMic() {
        isMicMuted = !isMicMuted
        agoraEngine?.muteLocalAudioStream(isMicMuted)
        updateMicButton()
        Toast.makeText(requireContext(), "Mic " + (if (isMicMuted) "Muted" else "Unmuted"), Toast.LENGTH_SHORT).show()
    }

    /**
     * Updates the microphone button icon based on the current mute state.
     */
    private fun updateMicButton() {
        _binding?.mute?.let {
            if (isMicMuted) {
                it.setImageResource(R.drawable.micoff) // Assuming 'micoff' is the icon for mic muted
            } else {
                it.setImageResource(R.drawable.micoff) // Assuming 'micon' is the icon for mic unmuted
                // Make sure you have a 'micon' drawable if needed
            }
        }
    }

    /**
     * Ends the current voice call by leaving the Agora channel.
     * Navigates back to the previous screen.
     */
    private fun endCall() {
        agoraEngine?.leaveChannel()
        Toast.makeText(requireContext(), "Voice call ended", Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressedDispatcher.onBackPressed() // Go back to the previous fragment/activity
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * Cleans up Agora resources to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        agoraEngine?.leaveChannel() // Ensure leaving channel before destroying engine
        RtcEngine.destroy() // Destroy the RtcEngine instance
        agoraEngine = null // Nullify the engine reference
        _binding = null // Nullify the binding reference
        Log.d("VoiceCallFragment", "onDestroyView called. Agora engine destroyed.")
    }
}
