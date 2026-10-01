package com.example.familysafety

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.familysafety.databinding.ItemTaskAlertBinding
import java.text.DateFormat
import java.util.Date

/** STEP 4-4：只呈現任務警報，不提供 Resolve 或修改任務的操作。 */
class TaskAlertAdapter : ListAdapter<TaskAlertAdapter.Item, TaskAlertAdapter.TaskViewHolder>(DIFF) {
    // 將本次計算的逾期結果放在 UI 快照中，讓 DiffUtil 偵測時間跨越截止點。
    // 原始 TaskAlert.status 不會被改寫。
    data class Item(val alert: TaskAlert, val overdue: Boolean)

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item) =
                oldItem.alert.id == newItem.alert.id
            override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
        }
    }

    fun submitAlerts(alerts: List<TaskAlert>, now: Long) {
        submitList(alerts.map { Item(it, it.isOverdue(now)) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder =
        TaskViewHolder(ItemTaskAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(private val binding: ItemTaskAlertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) = with(binding) {
            val alert = item.alert
            val context = root.context
            textViewAlertTaskTitle.text = alert.title
            textViewAlertTaskDescription.text = alert.description
            textViewAlertTaskAssignee.text = context.getString(R.string.alert_task_assignee, alert.assigneeName)
            val locale = context.resources.configuration.locales[0]
            val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
                .format(Date(alert.dueDate))
            textViewAlertTaskDueDate.text = context.getString(R.string.alert_task_due_date, date)

            // 沿用既有任務狀態文字、Badge Drawable 及色票。
            val (label, background, color) = when (alert.status) {
                TaskStatus.PENDING -> Triple(R.string.task_status_pending, R.drawable.bg_badge_pending, R.color.warning_red)
                TaskStatus.IN_PROGRESS -> Triple(R.string.task_status_in_progress, R.drawable.bg_badge_running, R.color.primary_blue)
                TaskStatus.COMPLETED -> Triple(R.string.task_status_completed, R.drawable.bg_badge_completed, R.color.safe_green)
            }
            textViewAlertTaskStatus.setText(label)
            textViewAlertTaskStatus.setBackgroundResource(background)
            textViewAlertTaskStatus.setTextColor(ContextCompat.getColor(context, color))
            // 每次綁定都設定兩種情況，避免回收的卡片殘留逾期標示。
            textViewAlertTaskOverdue.visibility = if (item.overdue) View.VISIBLE else View.GONE
        }
    }
}
