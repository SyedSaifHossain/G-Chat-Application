package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.syedsaifhossain.g_chatapplication.databinding.FragmentFullScreenImageBinding

class FullScreenImageFragment : Fragment() {

    private var _binding: FragmentFullScreenImageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val imageUrl = arguments?.getString("image_url")

        Glide.with(requireContext())
            .load(imageUrl)
            .into(binding.fullscreenImageView)

        binding.fullscreenImageView.setOnClickListener {
            parentFragmentManager.popBackStack() // go back on click
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}