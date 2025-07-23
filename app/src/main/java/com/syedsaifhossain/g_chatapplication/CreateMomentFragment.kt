package com.syedsaifhossain.g_chatapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.databinding.FragmentCreateMomentBinding
import java.text.SimpleDateFormat
import java.util.*

class CreateMomentFragment : Fragment() {

    private var _binding: FragmentCreateMomentBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private var selectedImageUri: Uri? = null
    private val currentDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(requireContext())
                .load(it)
                .into(binding.momentImagePreview)
            binding.imagePreviewContainer.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateMomentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        loadUserData()
    }

    private fun setupClickListeners() {
        binding.apply {
            // 返回按钮
            backButton.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            
            // 发布按钮
            publishButton.setOnClickListener {
                publishMoment()
            }
            
            // 添加图片按钮
            addImageButton.setOnClickListener {
                selectImage()
            }
            
            // 删除图片按钮
            removeImageButton.setOnClickListener {
                selectedImageUri = null
                imagePreviewContainer.visibility = View.GONE
            }
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "Unknown"
                val imageUrl = document.getString("profileImageUrl") 
                    ?: document.getString("avatarUrl")
                
                binding.userName.text = name
                
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .override(40, 40)
                    .into(binding.userAvatar)
            }
    }

    private fun selectImage() {
        getContent.launch("image/*")
    }

    private fun publishMoment() {
        val momentText = binding.momentTextInput.text.toString().trim()
        
        if (momentText.isEmpty() && selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please enter text or select an image", Toast.LENGTH_SHORT).show()
            return
        }

        binding.publishButton.isEnabled = false
        binding.publishButton.text = "Publishing..."

        val userId = auth.currentUser?.uid ?: return
        val momentId = firestore.collection("moments").document().id

        // 如果有图片，先上传图片
        if (selectedImageUri != null) {
            uploadImageAndPublishMoment(momentId, momentText, userId)
        } else {
            publishMomentToFirestore(momentId, momentText, null, userId)
        }
    }

    private fun uploadImageAndPublishMoment(momentId: String, momentText: String, userId: String) {
        val imageRef = storage.reference.child("moments/$momentId.jpg")
        
        selectedImageUri?.let { uri ->
            // 添加元数据
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
                
            imageRef.putFile(uri, metadata)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    Log.d("CreateMomentFragment", "Upload progress: $progress%")
                }
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        publishMomentToFirestore(momentId, momentText, downloadUri.toString(), userId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CreateMomentFragment", "Image upload failed", e)
                    Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.publishButton.isEnabled = true
                    binding.publishButton.text = "Publish"
                }
        }
    }

    private fun publishMomentToFirestore(momentId: String, momentText: String, imageUrl: String?, userId: String) {
        val momentData = hashMapOf(
            "id" to momentId,
            "userId" to userId,
            "momentText" to momentText,
            "imageUrl" to imageUrl,
            "timestamp" to Date(),
            "day" to SimpleDateFormat("dd", Locale.getDefault()).format(Date()),
            "month" to SimpleDateFormat("MMM", Locale.getDefault()).format(Date())
        )

        firestore.collection("moments").document(momentId)
            .set(momentData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Published successfully!", Toast.LENGTH_SHORT).show()
                // 返回上一页并刷新
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Publish failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.publishButton.isEnabled = true
                binding.publishButton.text = "Publish"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 