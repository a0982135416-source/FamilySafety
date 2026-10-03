package com.example.familysafety

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.familysafety.databinding.ItemTaskBinding
import java.text.DateFormat
import java.util.Date

/** STEP 5-1：逾期 UI 快照可讓 DiffUtil 偵測時間跨越，不改 stored status。 */
class TaskAdapter : ListAdapter<TaskAdapter.Item, TaskAdapter.TaskViewHolder>(DIFF) {
    data class Item(val task: Task, val overdue: Boolean)
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.task.id == newItem.task.id
            override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
        }
    }
    fun submitTasks(tasks: List<Task>, now: Long) = submitList(tasks.map { Item(it, it.isOverdue(now)) })
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TaskViewHolder(
        ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) = holder.bind(getItem(position))

    class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) = with(binding) {
            val context = root.context
            val task = item.task
            textViewTaskItemTitle.text = task.title
            textViewTaskItemDescription.text = task.description
            textViewTaskItemAssignee.text = context.getString(R.string.task_assignee, task.assignee)
            val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                context.resources.configuration.locales[0]).format(Date(task.dueDate))
            textViewTaskItemDueDate.text = context.getString(R.string.task_due_date, date)
            val (label, background, color) = when (task.status) {
                TaskStatus.PENDING -> Triple(R.string.task_status_pending, R.drawable.bg_badge_pending, R.color.warning_red)
                TaskStatus.IN_PROGRESS -> Triple(R.string.task_status_in_progress, R.drawable.bg_badge_running, R.color.primary_blue)
                TaskStatus.COMPLETED -> Triple(R.string.task_status_completed, R.drawable.bg_badge_completed, R.color.safe_green)
            }
            textViewTaskItemStatus.setText(label)
            textViewTaskItemStatus.setBackgroundResource(background)
            textViewTaskItemStatus.setTextColor(ContextCompat.getColor(context, color))
            textViewTaskItemOverdue.visibility = if (item.overdue) View.VISIBLE else View.GONE
        }
    }
}
