package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.databinding.FragmentChatScreenPageMoreOptionBinding

class ChatScreenPageMoreOptionFragment : Fragment() {
    private var _binding: FragmentChatScreenPageMoreOptionBinding? = null

    private val binding get() = _binding!!

    private var otherUserId: String? = null
    private var otherUserName: String? = null
    private var otherUserAvatarUrl: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {


        _binding = FragmentChatScreenPageMoreOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getString("otherUserId")

        binding.moreOptionBackImg.setOnClickListener {
            findNavController().popBackStack()
        }

        arguments?.let {
            otherUserId = it.getString("otherUserId")
            otherUserName = it.getString("otherUserName")
            otherUserAvatarUrl = it.getString("otherUserAvatarUrl")
        }

        // Display the other user's name and profile image
        otherUserName?.let {
            binding.userNameProfile.text = it
        }

        otherUserAvatarUrl?.let {
            Glide.with(requireContext())
                .load(it)
                .placeholder(R.drawable.default_avatar) // Placeholder if no image URL

                .into(binding.moreOptionProfileImg) // ImageView to display profile picture

        }

        // 音频通话按钮
        binding.audioLayout.setOnClickListener {
            if (otherUserId.isNullOrEmpty()) {
                return@setOnClickListener
            }
            CallManager.initiateVoiceCall(this, otherUserId!!)
        }

        // 视频通话按钮
        binding.videoLayout.setOnClickListener {
            if (otherUserId.isNullOrEmpty()) {
                return@setOnClickListener
            }
            CallManager.initiateVideoCall(this, otherUserId!!)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}