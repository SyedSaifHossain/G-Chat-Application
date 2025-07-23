package com.syedsaifhossain.g_chatapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.databinding.FragmentMePageBinding
import com.yalantis.ucrop.UCrop
import java.io.File

class MePageFragment : Fragment() {

    private var _binding: FragmentMePageBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val IMAGE_PICK_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserData()

        // 修改右侧箭头点击事件，改为更换 Pin 照片
        binding.meRightArrow.setOnClickListener {
            openImagePicker()
        }

        binding.settingsLayout.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, SettingsPageFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.momentLayout.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, MomentPageFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    val sourceUri = data?.data ?: return
                    val destUri = Uri.fromFile(
                        File(
                            requireContext().cacheDir,
                            "pinned_${System.currentTimeMillis()}.jpg"
                        )
                    )

                    val options = UCrop.Options().apply {
                        setToolbarColor(resources.getColor(android.R.color.black, null))
                        setStatusBarColor(resources.getColor(android.R.color.black, null))
                        setActiveControlsWidgetColor(resources.getColor(android.R.color.white, null))
                        setToolbarWidgetColor(resources.getColor(android.R.color.white, null))
                        setFreeStyleCropEnabled(true)
                        setHideBottomControls(true)
                        setShowCropFrame(true)
                        setShowCropGrid(false)
                        setCircleDimmedLayer(false)
                    }

                    UCrop.of(sourceUri, destUri)
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(1080, 1080)
                        .withOptions(options)
                        .start(requireContext(), this)
                }

                UCrop.REQUEST_CROP -> {
                    val resultUri = UCrop.getOutput(data!!)
                    resultUri?.let {
                        uploadPinnedImage(it)
                    }
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(
                requireContext(),
                "裁剪错误: ${cropError?.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun uploadPinnedImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val fileRef = storage.reference.child("pinned_images/${userId}.jpg")

        fileRef.putFile(imageUri).addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { uri ->
                updatePinnedImageUrl(uri.toString())
                Toast.makeText(requireContext(), "Pin 照片更新成功", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePinnedImageUrl(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .update("pinnedImageUrl", imageUrl)
            .addOnSuccessListener {
                // 更新本地显示
                loadUserData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                val name = document.getString("name") ?: "Unknown"
                val imageUrl = document.getString("profileImageUrl")
                    ?: document.getString("avatarUrl")

                binding.meUserName.text = name

                // 加载头像
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .override(70,70)
                    .into(binding.meProfileImg)
            }
            .addOnFailureListener { e ->
                // Handle error if needed
            }
    }

    override fun onResume() {
        super.onResume()
        (parentFragment as? HomeFragment)?.showBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}