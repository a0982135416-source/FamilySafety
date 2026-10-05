package com.example.familysafety

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.res.ColorStateList
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.familysafety.databinding.FragmentHomeBinding
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.DateFormat
import java.util.Date

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
    private var profileDialog: AlertDialog? = null
    private val environmentRefreshHandler = Handler(Looper.getMainLooper())
    private val environmentRefresh = object : Runnable {
        override fun run() {
            if (_binding == null || !isResumed) return
            renderEnvironment()
            environmentRefreshHandler.postDelayed(this, 250L)
        }
    }

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

        binding.recyclerViewHomeTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.cardViewHomeAlert.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation)
                .selectedItemId = R.id.navAlert
        }
        binding.frameLayoutHomeProfile.setOnClickListener { showProfile() }
        renderEnvironment()
        refreshTodayTasks()
    }

    // Shared environment presentation only. MainActivity retains warning ownership.
    override fun onResume() {
        super.onResume()
        environmentRefreshHandler.removeCallbacks(environmentRefresh)
        renderEnvironment()
        refreshTodayTasks()
        environmentRefreshHandler.postDelayed(environmentRefresh, 250L)
    }

    override fun onPause() {
        environmentRefreshHandler.removeCallbacks(environmentRefresh)
        super.onPause()
    }

    private fun renderEnvironment() {
        val views = _binding ?: return
        val source = MockEnvironmentDataSource
        val status = source.getSafetyStatus()
        val sensor = source.sensorState
        val reading = source.getCurrentEnvironmentData()
        val activeAlert = source.getActiveWarning()
        val (title, label, colorResource) = when (status) {
            EnvironmentSafetyStatus.SAFE -> Triple(R.string.home_status_safe,
                R.string.home_status_safe_label, R.color.safe_green)
            EnvironmentSafetyStatus.WARNING -> Triple(R.string.home_status_warning,
                R.string.home_status_warning_label, R.color.warning_orange)
            EnvironmentSafetyStatus.DANGER -> Triple(R.string.home_status_danger,
                R.string.home_status_danger_label, R.color.warning_red)
        }
        val color = ContextCompat.getColor(requireContext(), colorResource)
        val white = ContextCompat.getColor(requireContext(), R.color.white)
        val surface = ColorUtils.blendARGB(color, white, 0.92f)
        val stroke = ColorUtils.blendARGB(color, white, 0.72f)
        views.textViewHomeStatus.setText(title)
        views.textViewHomeStatus.setTextColor(color)
        ImageViewCompat.setImageTintList(views.imageViewHomeStatusIcon, ColorStateList.valueOf(color))
        views.cardViewHomeStatus.setCardBackgroundColor(surface)
        views.cardViewHomeStatus.strokeColor = stroke
        views.textViewHomeSafetyStatusLabel.setText(label)
        views.textViewHomeSafetyStatusLabel.setTextColor(color)
        views.textViewHomeSafetyStatusLabel.backgroundTintList = ColorStateList.valueOf(surface)
        views.cardViewHomeSafety.setCardBackgroundColor(surface)
        views.cardViewHomeSafety.strokeColor = stroke
        views.frameLayoutHomeSafetyIconBackground.backgroundTintList = ColorStateList.valueOf(color)
        // These safety sensors always represent Kitchen, independently of the current-reading location.
        views.textViewHomeSafetyLocation.setTextColor(color)
        views.textViewHomeSafetyLocation.backgroundTintList = ColorStateList.valueOf(surface)
        views.textViewHomeGasStatusValue.setText(if (sensor.gasOn)
            R.string.environment_status_on else R.string.environment_status_off)
        views.textViewHomeFireStatusValue.setText(if (sensor.flameDetected)
            R.string.environment_status_detected else R.string.environment_status_not_detected)
        views.textViewHomePersonStatusValue.setText(if (sensor.personDetected)
            R.string.environment_status_detected else R.string.environment_status_not_detected)
        val normal = ContextCompat.getColor(requireContext(), R.color.primary_blue)
        val inactive = ContextCompat.getColor(requireContext(), R.color.secondary_text)
        val danger = ContextCompat.getColor(requireContext(), R.color.warning_red)
        // Red follows an existing alert's semantics, never merely Gas ON / Flame detected.
        val gasLeak = activeAlert?.type == EnvironmentAlertType.GAS_LEAK_RISK
        views.textViewHomeGasStatusValue.setTextColor(if (gasLeak) danger else if (sensor.gasOn) normal else inactive)
        views.textViewHomeFireStatusValue.setTextColor(if (gasLeak) danger else if (sensor.flameDetected) normal else inactive)
        views.textViewHomePersonStatusValue.setTextColor(if (activeAlert?.type == EnvironmentAlertType.UNATTENDED_COOKING)
            danger else if (sensor.personDetected) normal else inactive)
        val locationLabel = when (source.selectedLocation) {
            "LIVING_ROOM" -> R.string.location_living_room
            "BEDROOM" -> R.string.location_bedroom
            else -> R.string.location_kitchen
        }
        views.textViewHomeEnvironmentLocation.text = getString(R.string.home_environment_location_format, getString(locationLabel))
        views.textViewHomeTemperatureValue.text = getString(R.string.environment_temperature_value_format, reading.temperature)
        views.textViewHomeHumidityValue.text = getString(R.string.environment_humidity_value_format, reading.humidity)
        val pending = MockEnvironmentAlertDataSource.getAlerts().filter { it.status == EnvironmentAlertStatus.PENDING }
        views.textViewHomeAlertCount.text = getString(R.string.home_alert_pending_count, pending.size)
        // Match existing warning priority; otherwise show the latest pending record.
        val summary = activeAlert ?: pending.maxByOrNull { it.createdAt }
        views.textViewHomeAlertMsg.setText(when (summary?.type) {
            EnvironmentAlertType.GAS_LEAK_RISK -> R.string.alert_message_gas_leak
            EnvironmentAlertType.UNATTENDED_COOKING -> R.string.environment_warning_unattended_message
            null -> R.string.home_alert_none
        })
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 04. 設定今日任務進度
    // Set Up Today's Task Progress
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private fun setupTaskProgress(tasks: List<Task>) {

        // Progress and list use the same current-day shared tasks, including completed tasks.
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
        val pendingTasks = totalTasks - completedTasks

        val completionRate = if (totalTasks == 0) 0 else (completedTasks * 100) / totalTasks

        binding.circularProgressIndicatorHomeTask.max = totalTasks
        binding.circularProgressIndicatorHomeTask.progress = completedTasks

        binding.textViewHomeProgressCenterText.text =
            getString(
                R.string.task_progress_fraction,
                completedTasks,
                totalTasks,
            )

        binding.textViewHomeTaskDone.text =
            getString(
                R.string.task_completed_count,
                completedTasks,
            )

        binding.textViewHomeTaskPending.text =
            getString(
                R.string.task_pending_count,
                pendingTasks,
            )

        binding.textViewHomeTaskRate.text =
            getString(
                R.string.task_completion_rate,
                completionRate,
            )
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 05. 設定今日任務列表
    // Set Up Today's Task List
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    private fun refreshTodayTasks() {
        val tasks = MockTaskDataSource.getTasks { getString(it) }.dueToday()
        setupTaskProgress(tasks)
        val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, resources.configuration.locales[0])
        binding.recyclerViewHomeTasks.adapter = TodayTaskAdapter(tasks.map {
            TodayTask(it.title, it.status.name, dateFormat.format(Date(it.dueDate)))
        })
        binding.textViewHomeTasksEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 06. Profile / Logout：只使用目前登入的 process-memory session
    private fun showProfile() {
        if (_binding == null || profileDialog?.isShowing == true) return
        val member = MockAuthDataSource.getCurrentMember() ?: return
        val role = getString(if (member.role == MemberRole.ADMIN)
            R.string.management_role_admin else R.string.management_role_member)
        fun line(key: Int, value: String) = getString(R.string.management_label_value, getString(key), value)
        val message = listOf(line(R.string.management_name, member.name),
            line(R.string.management_account, member.account), line(R.string.management_email, member.email),
            line(R.string.management_role, role)).joinToString("\n")
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.management_account)
            .setMessage(message).setNegativeButton(R.string.task_detail_close, null)
            .setPositiveButton(R.string.auth_logout) { _, _ ->
                MockAuthDataSource.logout()
                val activity = requireActivity()
                startActivity(Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                activity.finish()
            }.create()
        profileDialog = dialog
        dialog.setOnDismissListener { if (profileDialog === dialog) profileDialog = null }
        dialog.show()
    }

    // 07. 清除 ViewBinding / Dialog
    // Clear ViewBinding
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun onDestroyView() {
        environmentRefreshHandler.removeCallbacks(environmentRefresh)
        profileDialog?.dismiss()
        profileDialog = null
        super.onDestroyView()

        _binding = null
    }
}
