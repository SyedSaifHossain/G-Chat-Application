package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.syedsaifhossain.g_chatapplication.databinding.FragmentMePageBinding

class MePageFragment : Fragment() {


    private var _binding: FragmentMePageBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, ProfileFragment())
                .addToBackStack(null)
                .commit()
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