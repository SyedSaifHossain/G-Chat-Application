package com.syedsaifhossain.g_chatapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.syedsaifhossain.g_chatapplication.adapter.MomentAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentMomentPageBinding
import com.syedsaifhossain.g_chatapplication.models.Moment
import com.yalantis.ucrop.UCrop
import java.io.File

class MomentPageFragment : Fragment() {

    private var _binding: FragmentMomentPageBinding? = null
    private var scrollListener: OnScrollChangedListener? = null
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val momentList = mutableListOf<Moment>()
    private lateinit var momentAdapter: MomentAdapter
    
    private val IMAGE_PICK_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMomentPageBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding?.apply {
            momentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            momentAdapter = MomentAdapter(momentList)
            momentRecyclerView.adapter = momentAdapter

            momentBackImg.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            
            // 添加相机按钮点击事件
            addMomentButton.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, CreateMomentFragment())
                    .addToBackStack(null)
                    .commit()
            }
            
            // 添加 Pin 照片右侧箭头点击事件
            pinaArrowImg.setOnClickListener {
                openImagePicker()
            }
            
            // 加载用户信息
            loadUserData()
            
            // 加载动态数据
            loadMoments()

            // Hide initially
            momentTitle.visibility = View.GONE
            momentMoreBtn.visibility = View.GONE

            scrollListener = OnScrollChangedListener {
                val scrollY = momentScrollView.scrollY

                if (scrollY > 100 && momentTitle.visibility == View.GONE) {
                    momentHeaderLayout.setBackgroundColor(android.graphics.Color.parseColor("#000000"))
                    momentTitle.visibility = View.VISIBLE
                    momentMoreBtn.visibility = View.VISIBLE
                    momentTitle.alpha = 0f
                    momentMoreBtn.alpha = 0f
                    momentTitle.animate().alpha(1f).setDuration(200).start()
                    momentMoreBtn.animate().alpha(1f).setDuration(200).start()
                } else if (scrollY <= 100 && momentTitle.visibility == View.VISIBLE) {
                    momentTitle.animate().alpha(0f).setDuration(200).withEndAction {
                        momentTitle.visibility = View.GONE
                    }.start()
                    momentMoreBtn.animate().alpha(0f).setDuration(200).withEndAction {
                        momentMoreBtn.visibility = View.GONE
                    }.start()

                    // Reset background to transparent
                    momentHeaderLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }

            momentScrollView.viewTreeObserver.addOnScrollChangedListener(scrollListener)
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
    
    override fun onResume() {
        super.onResume()
        // 每次页面恢复时重新加载数据
        loadMoments()
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
                val pinnedImageUrl = document.getString("pinnedImageUrl")
                
                _binding?.momentUserName?.text = name
                
                // 加载用户头像
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .override(50, 50)
                    .into(_binding?.momentProfileImg ?: return@addOnSuccessListener)
                
                // 加载 Pin 照片（如果存在）
                if (!pinnedImageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(pinnedImageUrl)
                        .placeholder(R.drawable.default_image)
                        .into(_binding?.pinImg ?: return@addOnSuccessListener)
                    
                    // 同时更新顶部横幅照片
                    Glide.with(requireContext())
                        .load(pinnedImageUrl)
                        .placeholder(R.drawable.cityimg)
                        .into(_binding?.bannerImageView ?: return@addOnSuccessListener)
                } else {
                    // 如果没有 Pin 照片，显示默认图片
                    _binding?.pinImg?.setImageResource(R.drawable.cityimg)
                    _binding?.bannerImageView?.setImageResource(R.drawable.cityimg)
                }
            }
            .addOnFailureListener { e ->
                // 处理错误
            }
    }
    
        private fun loadMoments() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        firestore.collection("moments")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                momentList.clear()
                for (document in documents) {
                    val moment = document.toObject(Moment::class.java)
                    moment?.let {
                        val momentWithId = it.copy(id = document.id)
                        momentList.add(momentWithId)
                        // 添加调试日志
                        android.util.Log.d("MomentPageFragment", "Loaded moment: id=${momentWithId.id}, text='${momentWithId.momentText}', imageUrl='${momentWithId.imageUrl}'")
                    }
                }
                android.util.Log.d("MomentPageFragment", "Total moments loaded: ${momentList.size}")
                momentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MomentPageFragment", "Failed to load moments", e)
            }
    }

    override fun onDestroyView() {
        _binding?.momentScrollView?.viewTreeObserver?.removeOnScrollChangedListener(scrollListener)
        scrollListener = null
        _binding = null
        super.onDestroyView()
    }
}