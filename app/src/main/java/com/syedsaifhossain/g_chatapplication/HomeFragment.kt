package com.syedsaifhossain.g_chatapplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!! // Use non-null assertion

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Return non-nullable View
        // Inflate the layout using ViewBinding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initial fragment
        replaceFragment(ChatFragment())

        // Set up BottomNavigationView to handle fragment replacement
        binding.bottomNavigation.setOnItemSelectedListener { item -> // Use setOnItemSelectedListener
            when (item.itemId) {
                // Use the correct IDs from bottom_navigation.xml
                R.id.nav_chats -> {
                    replaceFragment(ChatFragment()) // Replace with ChatFragment
                    true
                }
                R.id.nav_contacts -> {
                    replaceFragment(ContactFragment()) // Replace with ContactFragment
                    true
                }
                R.id.nav_discover -> {
                    replaceFragment(DiscoverFragment()) // Replace with DiscoverFragment
                    true
                }
                R.id.nav_me -> {
                    replaceFragment(MeFragment()) // Replace with MeFragment
                    true
                }
                else -> false
            }
        }

        // Return the root view for the Fragment
        return binding.root
    }

    // Method to replace the fragment inside the FrameLayout container
    private fun replaceFragment(fragment: Fragment) {
        // Use childFragmentManager if FrameLayout is inside HomeFragment's layout
        // Use parentFragmentManager if FrameLayout is in the Activity's layout
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction() // Check if this is correct
        transaction.replace(R.id.nav_host_fragment, fragment) // Ensure frame_layout is the correct ID
        transaction.commit()
    }

    // Add onDestroyView to clean up binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}