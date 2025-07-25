package com.syedsaifhossain.g_chatapplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.syedsaifhossain.g_chatapplication.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // 🔧 Disable ripple & active indicator (if your material version supports it)
        binding.bottomNavigation.apply {
            // removes ripple entirely
            itemRippleColor = null

            // Material 1.8.0+ only
            try {
                // removes the rounded “pill” selection indicator
                isItemActiveIndicatorEnabled = false
            } catch (_: Throwable) {
                // you're probably on an older material version – use the styles solution below
            }
        }

        // Initial fragment
        replaceFragment(ChatFragment())

        // Bottom nav listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    replaceFragment(ChatFragment())
                    true
                }
                R.id.nav_contacts -> {
                    replaceFragment(ContactFragment())
                    true
                }
                R.id.nav_discover -> {
                    replaceFragment(DiscoverPageFragment())
                    true
                }
                R.id.nav_me -> {
                    replaceFragment(MePageFragment())
                    true
                }
                else -> false
            }
        }

        return binding.root
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        val currentFragment = childFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (currentFragment is ChatFragment ||
            currentFragment is ContactFragment ||
            currentFragment is DiscoverPageFragment ||
            currentFragment is MePageFragment
        ) {
            binding.bottomNavigation.visibility = View.VISIBLE
        }
    }

    fun showBottomNav() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }
}
