package com.example.familysafety

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.familysafety.databinding.FragmentHomeBinding

/**
 * ========================================================================
 * 家庭安全管理系統－首頁
 * Family Safety Management System - Home Fragment
 * ========================================================================
 *
 * HomeFragment 負責首頁 Dashboard 的 UI 與畫面資料。
 * HomeFragment manages the Home Dashboard UI and screen data.
 *
 * BottomNavigationView 不由 HomeFragment 管理。
 * BottomNavigationView is managed by MainActivity.
 */
class HomeFragment : Fragment() {

    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 01. ViewBinding
    // ViewBinding
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private var _binding: FragmentHomeBinding? = null

    private val binding: FragmentHomeBinding
        get() = _binding!!


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 02. 建立 Fragment 畫面
    // Create Fragment View
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentHomeBinding.inflate(
            inflater,
            container,
            false,
        )

        return binding.root
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 03. 初始化首頁
    // Initialize Home Screen
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupTaskProgress()
        setupTodayTaskList()
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 04. 設定今日任務進度
    // Set Up Today's Task Progress
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private fun setupTaskProgress() {

        // 測試資料，之後改由 ViewModel / API 提供
        // Mock data; this will later come from ViewModel / API
        val totalTasks = 5
        val completedTasks = 3
        val pendingTasks = totalTasks - completedTasks

        val completionRate = (completedTasks * 100) / totalTasks

        binding.CircularProgressIndicatorTask.max = totalTasks
        binding.CircularProgressIndicatorTask.progress = completedTasks

        binding.TextViewProgressCenterText.text =
            getString(
                R.string.task_progress_fraction,
                completedTasks,
                totalTasks,
            )

        binding.TextViewTaskDone.text =
            getString(
                R.string.task_completed_count,
                completedTasks,
            )

        binding.TextViewTaskPending.text =
            getString(
                R.string.task_pending_count,
                pendingTasks,
            )

        binding.TextViewTaskRate.text =
            getString(
                R.string.task_completion_rate,
                completionRate,
            )
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 05. 設定今日任務列表
    // Set Up Today's Task List
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private fun setupTodayTaskList() {

        /*
         * 目前先使用 Mock Data。
         * 任務名稱屬於動態資料，所以未來應由 Server / Database 提供。
         *
         * Mock data is used for now.
         * Task names are dynamic data and will later come from
         * the Server / Database.
         */
        val tasks = listOf(
            TodayTask(
                name = "Clean kitchen",
                status = "IN_PROGRESS",
                time = "09:00",
            ),
            TodayTask(
                name = "Take out trash",
                status = "PENDING",
                time = "10:30",
            ),
            TodayTask(
                name = "Clean bedroom",
                status = "COMPLETED",
                time = "14:00",
            ),
            TodayTask(
                name = "Wash dishes",
                status = "PENDING",
                time = "16:00",
            ),
        )

        binding.RecyclerViewTasks.layoutManager =
            LinearLayoutManager(requireContext())

        binding.RecyclerViewTasks.adapter =
            TodayTaskAdapter(tasks)
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 06. 清除 ViewBinding
    // Clear ViewBinding
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
