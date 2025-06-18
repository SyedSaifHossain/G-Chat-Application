package com.syedsaifhossain.g_chatapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.View

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 1. 监听destination变化，控制底部导航栏显示/隐藏
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.chatFragment5, R.id.contactFragment3, R.id.discoverFragment3, R.id.mePageFragment -> {
                    bottomNav.visibility = View.VISIBLE
                    // 高亮对应item（每次都强制设置）
                    val itemId = when(destination.id) {
                        R.id.chatFragment5 -> R.id.nav_chats
                        R.id.contactFragment3 -> R.id.nav_contacts
                        R.id.discoverFragment3 -> R.id.nav_discover
                        R.id.mePageFragment -> R.id.nav_me
                        else -> R.id.nav_chats
                    }
                    bottomNav.menu.findItem(itemId)?.isChecked = true
                }
                else -> {
                    bottomNav.visibility = View.GONE
                }
            }
        }

        // 2. 底部导航栏点击切换主页面
        bottomNav.setOnItemSelectedListener { item ->
            val destId = when(item.itemId) {
                R.id.nav_chats -> R.id.chatFragment5
                R.id.nav_contacts -> R.id.contactFragment3
                R.id.nav_discover -> R.id.discoverFragment3
                R.id.nav_me -> R.id.mePageFragment
                else -> null
            }
            destId?.let {
                // 避免重复跳转
                if (navController.currentDestination?.id != it) {
                    navController.navigate(it)
                }
                true
            } ?: false
        }
    }
}