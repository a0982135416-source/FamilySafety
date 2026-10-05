package com.example.familysafety

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.familysafety.databinding.FragmentTaskBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DateFormat
import java.util.Date

/** STEP 5-1：Task Center 列表與篩選，保留簡單資料入口供後續資料層使用。 */
class TaskFragment : Fragment() {
    // 01. ViewBinding 與畫面狀態
    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private val taskAdapter = TaskAdapter(::showTaskDetail)
    private var detailDialog: AlertDialog? = null
    private var tasks: List<Task> = emptyList()
    private var currentFilter = TaskFilter.ALL
    private val refreshTasks = object : Runnable {
        override fun run() {
            val currentBinding = _binding ?: return
            renderTasks()
            currentBinding.root.postDelayed(this, 1_000L)
        }
    }

    // 02. 畫面與 Filter 初始化
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val basePadding = binding.root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            root.updatePadding(top = basePadding + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        binding.recyclerViewTaskList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTaskList.adapter = taskAdapter
        filterButtons().forEach { (button, filter) ->
            button.setOnClickListener {
                currentFilter = filter
                updateFilterStyles()
                renderTasks()
                binding.recyclerViewTaskList.scrollToPosition(0)
                button.parent.requestChildFocus(button, button)
            }
        }
        updateFilterStyles()
        submitTasks(MockTaskDataSource.getTasks { getString(it) })
    }

    // 03. 資料入口、Empty State 與衍生逾期刷新
    internal fun submitTasks(newTasks: List<Task>) {
        tasks = newTasks.toList()
        MockTaskDataSource.replaceTasks(tasks)
        if (_binding != null) renderTasks()
    }

    private fun renderTasks() {
        val now = System.currentTimeMillis()
        val filtered = tasks.filterTasks(currentFilter, now)
        taskAdapter.submitTasks(filtered, now)
        binding.recyclerViewTaskList.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.textViewTaskEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // STEP 5-2：Detail 純文字使用 Material Dialog 內建可捲動內容。
    private fun showTaskDetail(clicked: Task) {
        if (_binding == null || !isAdded || detailDialog?.isShowing == true) return
        val task = tasks.find { it.id == clicked.id } ?: return
        if (task.status == TaskStatus.COMPLETED) return
        val statusLabel = if (task.status == TaskStatus.PENDING)
            R.string.task_status_pending else R.string.task_status_in_progress
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM,
            resources.configuration.locales[0]).format(Date(task.dueDate))
        val message = listOfNotNull(
            task.title, task.description,
            getString(R.string.task_assignee, task.assignee),
            getString(R.string.task_due_date, date),
            getString(R.string.task_detail_status, getString(statusLabel)),
            getString(R.string.task_filter_overdue).takeIf { task.isOverdue() },
        ).joinToString("\n\n")
        val next = if (task.status == TaskStatus.PENDING) TaskStatus.IN_PROGRESS else TaskStatus.COMPLETED
        val action = if (task.status == TaskStatus.PENDING) R.string.task_action_start else R.string.task_action_complete
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.task_detail_title)
            .setMessage(message)
            .setNegativeButton(R.string.task_detail_close, null)
            .setPositiveButton(action) { _, _ ->
                if (_binding != null && isAdded) {
                    submitTasks(MockTaskDataSource.updateStatus(tasks, task.id, next))
                }
            }.create()
        detailDialog = dialog
        dialog.setOnDismissListener { if (detailDialog === dialog) detailDialog = null }
        dialog.show()
    }

    private fun filterButtons() = listOf(
        binding.buttonTaskAll to TaskFilter.ALL,
        binding.buttonTaskPending to TaskFilter.PENDING,
        binding.buttonTaskInProgress to TaskFilter.IN_PROGRESS,
        binding.buttonTaskOverdue to TaskFilter.OVERDUE,
    )

    private fun updateFilterStyles() {
        val purple = ContextCompat.getColor(requireContext(), R.color.alert_task_purple)
        val white = ContextCompat.getColor(requireContext(), R.color.white)
        filterButtons().forEach { (button, filter) ->
            val selected = currentFilter == filter
            button.isSelected = selected
            button.backgroundTintList = ColorStateList.valueOf(if (selected) purple else white)
            button.setTextColor(if (selected) white else purple)
            button.strokeColor = ColorStateList.valueOf(purple)
        }
    }

    // 04. Lifecycle 清理；只在可見時刷新，不留下背景 callback
    override fun onResume() {
        super.onResume()
        binding.root.post(refreshTasks)
    }
    override fun onPause() {
        _binding?.root?.removeCallbacks(refreshTasks)
        super.onPause()
    }
    override fun onDestroyView() {
        detailDialog?.dismiss()
        detailDialog = null
        _binding?.root?.removeCallbacks(refreshTasks)
        _binding?.recyclerViewTaskList?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
