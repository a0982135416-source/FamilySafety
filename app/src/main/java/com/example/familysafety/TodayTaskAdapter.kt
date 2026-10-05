package com.example.familysafety

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.familysafety.databinding.ItemTodayTaskBinding

/**
 * ========================================================================
 * 今日任務資料
 * Today's Task Data
 * ========================================================================
 */
data class TodayTask(
    val name: String,
    val status: String,
    val time: String
)


/**
 * ========================================================================
 * 今日任務 RecyclerView Adapter
 * Today's Task RecyclerView Adapter
 * ========================================================================
 */
class TodayTaskAdapter(
    private val taskList: List<TodayTask>
) : RecyclerView.Adapter<TodayTaskAdapter.TaskViewHolder>() {


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 01. ViewHolder
    // ViewHolder
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    class TaskViewHolder(
        val binding: ItemTodayTaskBinding
    ) : RecyclerView.ViewHolder(binding.root)


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 02. 建立每一筆任務畫面
    // Create Each Task Item View
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskViewHolder {

        val binding = ItemTodayTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TaskViewHolder(binding)
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 03. 綁定任務資料
    // Bind Task Data
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun onBindViewHolder(
        holder: TaskViewHolder,
        position: Int
    ) {

        val task = taskList[position]

        with(holder.binding) {

            textViewHomeTaskItemName.text = task.name
            // Native marquee runs only when rendered text exceeds the constrained width.
            textViewHomeTaskItemName.isSelected = true
            textViewHomeTaskItemTime.text = task.time


            // 將程式狀態轉換成使用者語言
            // Convert internal status into localized UI text
            when (task.status) {

                "IN_PROGRESS" -> {

                    textViewHomeTaskItemStatusBadge.text =
                        root.context.getString(
                            R.string.task_status_in_progress
                        )

                    textViewHomeTaskItemStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_running
                    )

                    textViewHomeTaskItemStatusBadge.setTextColor(
                        Color.parseColor("#1687E8")
                    )
                }


                "PENDING" -> {

                    textViewHomeTaskItemStatusBadge.text =
                        root.context.getString(
                            R.string.task_status_pending
                        )

                    textViewHomeTaskItemStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_pending
                    )

                    textViewHomeTaskItemStatusBadge.setTextColor(
                        Color.parseColor("#F0525C")
                    )
                }


                "COMPLETED" -> {

                    textViewHomeTaskItemStatusBadge.text =
                        root.context.getString(
                            R.string.task_status_completed
                        )

                    textViewHomeTaskItemStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_completed
                    )

                    textViewHomeTaskItemStatusBadge.setTextColor(
                        Color.parseColor("#19B394")
                    )
                }
            }
        }
    }


    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
    // 04. 回傳任務數量
    // Return Task Count
    // ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝

    override fun getItemCount(): Int {
        return taskList.size
    }
}