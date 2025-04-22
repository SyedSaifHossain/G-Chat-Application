package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.syedsaifhossain.g_chatapplication.databinding.BottomSheetAddBinding

class AddOptionsBottomSheet(private val listener: ChatScreenFragment) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddBinding? = null
    private val binding get() = _binding!!

    interface AddOptionClickListener {
        fun onAlbumClicked()
        fun onCameraClicked()
        fun onVideoCallClicked()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardAlbum.setOnClickListener {
            listener.onAlbumClicked()
            dismiss()
        }

        binding.cardCamera.setOnClickListener {
            listener.onCameraClicked()
            dismiss()
        }

        binding.cardVideoCall.setOnClickListener {
            listener.onVideoCallClicked()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}