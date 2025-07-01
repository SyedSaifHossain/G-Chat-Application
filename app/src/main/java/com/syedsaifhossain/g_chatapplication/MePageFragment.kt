package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.syedsaifhossain.g_chatapplication.databinding.FragmentMePageBinding

class MePageFragment : Fragment() {


    private var _binding: FragmentMePageBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

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

        binding.meRightArrow.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                        ?: snapshot.child("avatarUrl").getValue(String::class.java)

                    binding.meUserName.text = name

                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.default_avatar)
                        .override(70,70)
                        .into(binding.meProfileImg)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                }
            })
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