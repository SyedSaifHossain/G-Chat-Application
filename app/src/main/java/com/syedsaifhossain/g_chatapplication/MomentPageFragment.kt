package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnScrollChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.syedsaifhossain.g_chatapplication.adapter.MomentAdapter
import com.syedsaifhossain.g_chatapplication.databinding.FragmentMomentPageBinding
import com.syedsaifhossain.g_chatapplication.models.Moment

class MomentPageFragment : Fragment() {

    private var _binding: FragmentMomentPageBinding? = null
    private var scrollListener: OnScrollChangedListener? = null
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val momentList = listOf(
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure"),
        Moment("18", "Jul", R.drawable.cityimg, "Visited the city today!"),
        Moment("17", "Jul", R.drawable.cityimg, "Sunset view"),
        Moment("16", "Jul", R.drawable.cityimg, "Hiking adventure")
    )

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
            momentRecyclerView.adapter = MomentAdapter(momentList)

            momentBackImg.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            
            // 加载用户信息
            loadUserData()

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
    
    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                
                val name = document.getString("name") ?: "Unknown"
                val imageUrl = document.getString("profileImageUrl") 
                    ?: document.getString("avatarUrl")
                
                _binding?.momentUserName?.text = name
                
                // 加载用户头像
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .override(50, 50)
                    .into(_binding?.momentProfileImg ?: return@addOnSuccessListener)
            }
            .addOnFailureListener { e ->
                // 处理错误
            }
    }

    override fun onDestroyView() {
        _binding?.momentScrollView?.viewTreeObserver?.removeOnScrollChangedListener(scrollListener)
        scrollListener = null
        _binding = null
        super.onDestroyView()
    }
}