package com.syedsaifhossain.g_chatapplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.fragment.findNavController
import com.syedsaifhossain.g_chatapplication.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using ViewBinding
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initial fragment
        replaceFragment(ChatFragment())

        // Set up BottomNavigationView to handle fragment replacement
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_chat -> {
                    replaceFragment(ChatFragment()) // Replace with ChatFragment
                    true
                }
                R.id.navigation_contact -> {
                    replaceFragment(ContactFragment()) // Replace with ContactFragment
                    true
                }
                R.id.navigation_discover -> {
                    replaceFragment(DiscoverFragment()) // Replace with DiscoverFragment
                    true
                }
                R.id.navigation_me -> {
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
        val transaction: FragmentTransaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, fragment)
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
                    // Handle Add Contacts option click
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
}