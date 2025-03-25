package com.syedsaifhossain.g_chatapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
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

        // Return the root view for the Fragment
        return binding.root
    }

    // Method to replace the fragment inside the FrameLayout container
    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.commit()
    }
}