package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.syedsaifhossain.g_chatapplication.databinding.FragmentVideoCallBinding

class VideoCallFragment : Fragment() {

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.userName.text = "User Name"
        binding.userStatus.text = "View Her Recent Activity"

        binding.micButton.setOnClickListener {
            // Toggle mic
        }

        binding.speakerButton.setOnClickListener {
            // Toggle speaker
        }

        binding.cameraButton.setOnClickListener {
            // Toggle camera
        }

        binding.endCallButton.setOnClickListener {

        }

        binding.switchCameraButton.setOnClickListener {
            // Switch camera
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}