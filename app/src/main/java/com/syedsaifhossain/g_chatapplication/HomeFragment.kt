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

        // Show PopupMenu when addButton is clicked
        binding.addButton.setOnClickListener {
            showPopupMenu(it)
        }

        // Return the root view for the Fragment
        return binding.root
    }

    // Method to replace the fragment inside the FrameLayout container
    private fun replaceFragment(fragment: Fragment) {
        // Use childFragmentManager if FrameLayout is inside HomeFragment's layout
        // Use parentFragmentManager if FrameLayout is in the Activity's layout
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction() // Check if this is correct
        transaction.replace(R.id.frame_layout, fragment) // Ensure frame_layout is the correct ID
        transaction.commit()
    }

    // Method to show PopupMenu
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        // Inflate the menu resource file (popup_menu.xml)
        popupMenu.inflate(R.menu.popup_menu)

        // Force icons to show (for API 26 and above)
        popupMenu.setForceShowIcon(true)

        // Set a listener for the menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.newChats -> {
                    findNavController().navigate(R.id.action_homeFragment_to_newChatsFragment)
                    true
                }
                R.id.addContacts -> {
                    findNavController().navigate(R.id.action_homeFragment_to_addContactsFragment)
                    true
                }
                R.id.scan -> {
                    findNavController().navigate(R.id.action_homeFragment_to_scanFragment)
                    true
                }
                else -> false
            }
        }

        // Show the PopupMenu
        popupMenu.show()
    }

    // Add onDestroyView to clean up binding
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}