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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.familysafety.databinding.FragmentTaskBinding

/** STEP 5-1：Task Center 列表與篩選，保留簡單資料入口供後續資料層使用。 */
class TaskFragment : Fragment() {
    // 01. ViewBinding 與畫面狀態
    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private val taskAdapter = TaskAdapter()
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
        submitTasks(MockTaskDataSource.create({ getString(it) }))
    }

    // 03. 資料入口、Empty State 與衍生逾期刷新
    internal fun submitTasks(newTasks: List<Task>) {
        tasks = newTasks.toList()
        if (_binding != null) renderTasks()
    }

    private fun renderTasks() {
        val now = System.currentTimeMillis()
        val filtered = tasks.filterTasks(currentFilter, now)
        taskAdapter.submitTasks(filtered, now)
        binding.recyclerViewTaskList.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.textViewTaskEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
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
        _binding?.root?.removeCallbacks(refreshTasks)
        _binding?.recyclerViewTaskList?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}