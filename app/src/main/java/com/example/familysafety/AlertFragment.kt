package com.example.familysafety

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.familysafety.databinding.FragmentAlertBinding

/**
 * ========================================================================
 * 警報中心頁面
 * Alert Center Fragment
 * ========================================================================
 */
class AlertFragment : Fragment() {

    // ================================================================
    // Enums for Category and Filter
    // ================================================================

    enum class AlertCategory {
        ENVIRONMENT,
        TASK,
    }

    enum class EnvironmentFilter {
        ALL,
        PENDING,
        RESOLVED,
    }

    // ================================================================
    // ViewBinding & State
    // ================================================================

    private var _binding: FragmentAlertBinding? = null
    private val binding get() = _binding!!

    private var currentCategory: AlertCategory = AlertCategory.ENVIRONMENT
    private var currentEnvFilter: EnvironmentFilter = EnvironmentFilter.ALL
    private var currentTaskAlertFilter: TaskAlertFilter = TaskAlertFilter.ALL

    // STEP 8-1: read/resolve the same process-memory source as Environment safety.
    private var environmentAlerts = MockEnvironmentAlertDataSource.getAlerts()
    private lateinit var environmentAlertAdapter: EnvironmentAlertAdapter

    // STEP 9-2: refreshed view data from the shared tasks, separate from environment history.
    private var taskAlerts: List<TaskAlert> = emptyList()
    private lateinit var taskAlertAdapter: TaskAlertAdapter
    private var taskSnapshotTime = 0L

    // 畫面可見時更新逾期條件；離開畫面即移除，避免持有已銷毀的 View。
    private val refreshTaskAlerts = object : Runnable {
        override fun run() {
            val currentBinding = _binding ?: return
            updateUI()
            currentBinding.root.postDelayed(this, 1_000L)
        }
    }

    // Counts are derived from the current mock records.
    private var envAllCount = 0
    private var envPendingCount = 0
    private var envResolvedCount = 0

    private var taskAllCount = 0
    private var taskPendingCount = 0
    private var taskInProgressCount = 0
    private var taskOverdueCount = 0

    // ================================================================
    // Fragment Lifecycle
    // ================================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlertBinding.inflate(
            inflater,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupRecyclerView()
        setupCategoryClickListeners()
        setupFilterClickListeners()
        setupFilterScrollHint()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        binding.root.removeCallbacks(refreshTaskAlerts)
        binding.root.post(refreshTaskAlerts)
    }

    override fun onPause() {
        _binding?.root?.removeCallbacks(refreshTaskAlerts)
        super.onPause()
    }

    override fun onDestroyView() {
        // STEP 4-5：Dialog 與畫面一起清理，避免離開後仍能操作舊紀錄。
        _binding?.root?.removeCallbacks(refreshTaskAlerts)
        _binding?.recyclerViewAlertList?.adapter = null
        super.onDestroyView()
        _binding = null
    }

    // ================================================================
    // Setup Methods
    // ================================================================

    // ================================================================
    // Header Insets / 避免標題與系統狀態列重疊
    // ================================================================
    private fun setupWindowInsets() {
        val header = binding.constraintLayoutAlertHeader

        // XML 中 Header 原始內容高度為 60dp。
        // Keep the original 60dp content area below the status bar.
        val contentHeightPx = (60 * resources.displayMetrics.density + 0.5f).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(header) { headerView, insets ->
            val statusBarTop = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
            ).top

            // Padding 與總高度必須同步調整，避免內容被壓縮。
            // Update both padding and total height (not padding alone).
            headerView.updateLayoutParams<ViewGroup.LayoutParams> {
                height = contentHeightPx + statusBarTop
            }
            headerView.updatePadding(top = statusBarTop)

            insets
        }

        // 確保 View 已附加後會取得 Insets。
        // Request inset dispatch after the view is attached.
        header.post { ViewCompat.requestApplyInsets(header) }
    }

    private fun setupRecyclerView() {
        taskAlertAdapter = TaskAlertAdapter()
        environmentAlertAdapter = EnvironmentAlertAdapter()
        binding.recyclerViewAlertList.layoutManager =
            LinearLayoutManager(requireContext())
        binding.recyclerViewAlertList.adapter = environmentAlertAdapter
    }

    private fun setupCategoryClickListeners() {
        binding.buttonAlertEnvironment.setOnClickListener {
            if (currentCategory != AlertCategory.ENVIRONMENT) {
                currentCategory = AlertCategory.ENVIRONMENT
                currentEnvFilter = EnvironmentFilter.ALL
                updateUI()
                binding.horizontalScrollViewAlertFilters.scrollTo(0, 0)
            }
        }

        binding.buttonAlertTask.setOnClickListener {
            if (currentCategory != AlertCategory.TASK) {
                currentCategory = AlertCategory.TASK
                currentTaskAlertFilter = TaskAlertFilter.ALL
                updateUI()
                binding.horizontalScrollViewAlertFilters.scrollTo(0, 0)
            }
        }
    }

    private fun setupFilterClickListeners() {
        binding.buttonAlertAll.setOnClickListener {
            if (currentCategory == AlertCategory.ENVIRONMENT) {
                currentEnvFilter = EnvironmentFilter.ALL
            } else {
                currentTaskAlertFilter = TaskAlertFilter.ALL
            }
            updateUI()
            revealFilter(binding.buttonAlertAll)
        }

        binding.buttonAlertPending.setOnClickListener {
            if (currentCategory == AlertCategory.ENVIRONMENT) {
                currentEnvFilter = EnvironmentFilter.PENDING
            } else {
                currentTaskAlertFilter = TaskAlertFilter.PENDING
            }
            updateUI()
            revealFilter(binding.buttonAlertPending)
        }

        binding.buttonAlertResolved.setOnClickListener {
            if (currentCategory == AlertCategory.ENVIRONMENT) {
                currentEnvFilter = EnvironmentFilter.RESOLVED
            }
            updateUI()
            revealFilter(binding.buttonAlertResolved)
        }

        binding.buttonAlertInProgress.setOnClickListener {
            if (currentCategory == AlertCategory.TASK) {
                currentTaskAlertFilter = TaskAlertFilter.IN_PROGRESS
            }
            updateUI()
            revealFilter(binding.buttonAlertInProgress)
        }

        binding.buttonAlertOverdue.setOnClickListener {
            if (currentCategory == AlertCategory.TASK) {
                currentTaskAlertFilter = TaskAlertFilter.OVERDUE
            }
            updateUI()
            revealFilter(binding.buttonAlertOverdue)
        }
    }

    // STEP 4-5：待文字與 Layout 更新後，將選取按鈕（含圓角）完整捲入可視範圍。
    // 只在點選時捲動；每秒逾期刷新不會搶走使用者的手動捲動位置。
    private fun revealFilter(button: View) {
        val currentBinding = _binding ?: return
        val scroll = currentBinding.horizontalScrollViewAlertFilters
        scroll.doOnLayout {
            if (_binding !== currentBinding || button.visibility != View.VISIBLE) return@doOnLayout
            val container = currentBinding.linearLayoutAlertFilters
            val left = container.left + button.left
            val right = container.left + button.right
            val target = when {
                left < scroll.scrollX + scroll.paddingLeft -> left - scroll.paddingLeft
                right > scroll.scrollX + scroll.width - scroll.paddingRight ->
                    right - scroll.width + scroll.paddingRight
                else -> scroll.scrollX
            }
            val maxScroll = (container.width - scroll.width + scroll.paddingLeft + scroll.paddingRight)
                .coerceAtLeast(0)
            val destination = target.coerceIn(0, maxScroll)
            // 已完整可見時不啟動零距離動畫，避免攔截下一次快速點擊。
            if (destination != scroll.scrollX) scroll.smoothScrollTo(destination, 0)
        }
    }

    // 只有內容超出可視寬度時顯示雙語提示，不靠裁切的文字提示還有更多篩選。
    private fun setupFilterScrollHint() {
        val currentBinding = binding
        fun updateHint() {
            if (_binding !== currentBinding) return
            val scroll = currentBinding.horizontalScrollViewAlertFilters
            val overflows = currentBinding.linearLayoutAlertFilters.width >
                scroll.width - scroll.paddingLeft - scroll.paddingRight
            currentBinding.textViewAlertFilterHint.visibility = if (overflows) View.VISIBLE else View.GONE
        }
        currentBinding.horizontalScrollViewAlertFilters.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateHint() }
        currentBinding.linearLayoutAlertFilters.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateHint() }
    }

    // ================================================================
    // Dynamic UI Updates
    // ================================================================

    private fun updateUI() {
        MockEnvironmentDataSource.refresh()
        environmentAlerts = MockEnvironmentAlertDataSource.getAlerts()
        taskAlerts = MockTaskAlertDataSource.create { getString(it) }
        taskSnapshotTime = System.currentTimeMillis()
        updateTaskCounts()
        updateEnvironmentCounts()
        updateCategoryTabStyles()
        updateFilterVisibilityAndText()
        updateFilterSelectionStyles()
        updateListAndEmptyState()
    }

    private fun updateEnvironmentCounts() {
        envAllCount = environmentAlerts.size
        envPendingCount = environmentAlerts.count {
            it.status == EnvironmentAlertStatus.PENDING
        }
        envResolvedCount = environmentAlerts.count {
            it.status == EnvironmentAlertStatus.RESOLVED
        }
    }

    // 數量與列表共用同一組篩選規則、同一個 now。
    private fun updateTaskCounts() {
        taskAllCount = taskAlerts.filterTaskAlerts(TaskAlertFilter.ALL, taskSnapshotTime).size
        taskPendingCount = taskAlerts.filterTaskAlerts(TaskAlertFilter.PENDING, taskSnapshotTime).size
        taskInProgressCount = taskAlerts.filterTaskAlerts(TaskAlertFilter.IN_PROGRESS, taskSnapshotTime).size
        taskOverdueCount = taskAlerts.filterTaskAlerts(TaskAlertFilter.OVERDUE, taskSnapshotTime).size
    }

    private fun updateCategoryTabStyles() {
        val primaryBlue = ContextCompat.getColor(requireContext(), R.color.primary_blue)
        val taskPurple = ContextCompat.getColor(requireContext(), R.color.alert_task_purple)
        val lightBlue = ContextCompat.getColor(requireContext(), R.color.light_blue)
        val titleBlue = ContextCompat.getColor(requireContext(), R.color.title_blue)

        if (currentCategory == AlertCategory.ENVIRONMENT) {
            // Environment selected
            binding.buttonAlertEnvironment.backgroundTintList = ColorStateList.valueOf(primaryBlue)
            binding.buttonAlertEnvironment.setTextColor(Color.WHITE)

            binding.buttonAlertTask.backgroundTintList = ColorStateList.valueOf(lightBlue)
            binding.buttonAlertTask.setTextColor(titleBlue)
        } else {
            // Task selected
            binding.buttonAlertEnvironment.backgroundTintList = ColorStateList.valueOf(lightBlue)
            binding.buttonAlertEnvironment.setTextColor(titleBlue)

            binding.buttonAlertTask.backgroundTintList = ColorStateList.valueOf(taskPurple)
            binding.buttonAlertTask.setTextColor(Color.WHITE)
        }
    }

    private fun updateFilterVisibilityAndText() {
        if (currentCategory == AlertCategory.ENVIRONMENT) {
            binding.buttonAlertResolved.visibility = View.VISIBLE
            binding.buttonAlertInProgress.visibility = View.GONE
            binding.buttonAlertOverdue.visibility = View.GONE

            binding.buttonAlertAll.text = getString(R.string.alert_filter_all_count, envAllCount)
            binding.buttonAlertPending.text = getString(R.string.alert_filter_pending_count, envPendingCount)
            binding.buttonAlertResolved.text = getString(R.string.alert_filter_resolved_count, envResolvedCount)
        } else {
            binding.buttonAlertResolved.visibility = View.GONE
            binding.buttonAlertInProgress.visibility = View.VISIBLE
            binding.buttonAlertOverdue.visibility = View.VISIBLE

            binding.buttonAlertAll.text = getString(R.string.alert_filter_all_count, taskAllCount)
            binding.buttonAlertPending.text = getString(R.string.alert_filter_pending_count, taskPendingCount)
            binding.buttonAlertInProgress.text = getString(R.string.alert_filter_in_progress_count, taskInProgressCount)
            binding.buttonAlertOverdue.text = getString(R.string.alert_filter_overdue_count, taskOverdueCount)
        }
    }

    private fun updateFilterSelectionStyles() {
        val activeColor = if (currentCategory == AlertCategory.ENVIRONMENT) {
            ContextCompat.getColor(requireContext(), R.color.primary_blue)
        } else {
            ContextCompat.getColor(requireContext(), R.color.alert_task_purple)
        }
        val inactiveBg = Color.WHITE
        val inactiveText = ContextCompat.getColor(requireContext(), R.color.title_blue)
        val strokeColor = "#D0E4F7".toColorInt()

        fun applyStyle(button: com.google.android.material.button.MaterialButton, isSelected: Boolean) {
            if (isSelected) {
                button.backgroundTintList = ColorStateList.valueOf(activeColor)
                button.setTextColor(Color.WHITE)
                button.strokeWidth = 0
            } else {
                button.backgroundTintList = ColorStateList.valueOf(inactiveBg)
                button.setTextColor(inactiveText)
                button.strokeColor = ColorStateList.valueOf(strokeColor)
                button.strokeWidth = 2
            }
        }

        if (currentCategory == AlertCategory.ENVIRONMENT) {
            applyStyle(binding.buttonAlertAll, currentEnvFilter == EnvironmentFilter.ALL)
            applyStyle(binding.buttonAlertPending, currentEnvFilter == EnvironmentFilter.PENDING)
            applyStyle(binding.buttonAlertResolved, currentEnvFilter == EnvironmentFilter.RESOLVED)
        } else {
            applyStyle(binding.buttonAlertAll, currentTaskAlertFilter == TaskAlertFilter.ALL)
            applyStyle(binding.buttonAlertPending, currentTaskAlertFilter == TaskAlertFilter.PENDING)
            applyStyle(binding.buttonAlertInProgress, currentTaskAlertFilter == TaskAlertFilter.IN_PROGRESS)
            applyStyle(binding.buttonAlertOverdue, currentTaskAlertFilter == TaskAlertFilter.OVERDUE)
        }
    }

    private fun updateListAndEmptyState() {
        if (currentCategory == AlertCategory.TASK) {
            if (binding.recyclerViewAlertList.adapter !== taskAlertAdapter) {
                binding.recyclerViewAlertList.adapter = taskAlertAdapter
            }
            val filtered = taskAlerts.filterTaskAlerts(currentTaskAlertFilter, taskSnapshotTime)
            taskAlertAdapter.submitAlerts(filtered, taskSnapshotTime)
            binding.recyclerViewAlertList.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
            binding.linearLayoutAlertEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.textViewAlertEmptyMessage.setText(R.string.alert_empty_task)
            return
        }

        if (binding.recyclerViewAlertList.adapter !== environmentAlertAdapter) {
            binding.recyclerViewAlertList.adapter = environmentAlertAdapter
        }

        val filtered = environmentAlerts.filter { alert ->
            when (currentEnvFilter) {
                EnvironmentFilter.ALL -> true
                EnvironmentFilter.PENDING ->
                    alert.status == EnvironmentAlertStatus.PENDING
                EnvironmentFilter.RESOLVED ->
                    alert.status == EnvironmentAlertStatus.RESOLVED
            }
        }.sortedByDescending { it.createdAt }

        environmentAlertAdapter.submitList(filtered.toList())
        binding.recyclerViewAlertList.visibility =
            if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.linearLayoutAlertEmpty.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.textViewAlertEmptyMessage.setText(R.string.alert_empty_environment)
    }
}
