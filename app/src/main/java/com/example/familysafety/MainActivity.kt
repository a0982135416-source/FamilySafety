package com.example.familysafety

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.familysafety.databinding.ActivityMainBinding

/**
 * ========================================================================
 * 家庭安全管理系統－主 Activity
 * Family Safety Management System - Main Activity
 * ========================================================================
 *
 * MainActivity 負責：
 * MainActivity is responsible for:
 *
 * 1. 管理主要 Fragment 容器
 *    Managing the main Fragment container
 *
 * 2. 管理 BottomNavigationView
 *    Managing BottomNavigationView
 *
 * 3. 切換主要功能頁面
 *    Switching between main feature screens
 */
class MainActivity : AppCompatActivity() {

    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 01. ViewBinding
    // ViewBinding
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private lateinit var binding: ActivityMainBinding


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 02. 建立 Activity
    // Create Activity
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)


        // 第一次啟動時顯示首頁
        // Show HomeFragment when the app starts for the first time
        if (savedInstanceState == null) {

            showFragment(
                HomeFragment()
            )
        }


        // 預設選擇首頁
        // Select Home by default
        binding.bottomNavigationViewMainNavigation.selectedItemId =
            R.id.navHome


        // 設定 Bottom Navigation
        // Set up Bottom Navigation
        setupBottomNavigation()
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 03. 設定底部導覽列
    // Set Up Bottom Navigation
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

// ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
// 03. 設定底部導覽列
// Set Up Bottom Navigation
// ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private fun setupBottomNavigation() {

        binding.bottomNavigationViewMainNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                // 首頁
                // Home
                R.id.navHome -> {

                    showFragment(
                        HomeFragment()
                    )

                    true
                }


                // 環境監控
                // Environment Monitoring
                R.id.navEnvironment -> {

                    showFragment(
                        EnvironmentFragment()
                    )

                    true
                }


                // 警報中心
                // Alert Center
                R.id.navAlert -> {

                    showFragment(
                        AlertFragment()
                    )

                    true
                }


                // 任務中心
                // Task Center
                R.id.navTask -> {

                    showFragment(
                        TaskFragment()
                    )

                    true
                }


                // 管理中心
                // Management Center
                R.id.navManagement -> {

                    showFragment(
                        ManagementFragment()
                    )

                    true
                }


                else -> false
            }
        }
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 04. 顯示 Fragment
    // Display Fragment
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private fun showFragment(fragment: Fragment) {

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainerView_main_content,
                fragment
            )
            .commit()
    }
}