package com.example.familysafety

/** STEP 5-1：獨立 Task Center 模型，沿用共用狀態 enum。 */
data class Task(val id: Long, val title: String, val description: String,
                val assignee: String, val dueDate: Long, val status: TaskStatus) {
    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean =
        now > dueDate && status != TaskStatus.COMPLETED
}

enum class TaskFilter { ALL, PENDING, IN_PROGRESS, OVERDUE }

/** 只顯示未完成工作，保留資料來源順序與原始狀態。 */
fun List<Task>.filterTasks(filter: TaskFilter, now: Long): List<Task> = filter { task ->
    task.status != TaskStatus.COMPLETED && when (filter) {
        TaskFilter.ALL -> true
        TaskFilter.PENDING -> task.status == TaskStatus.PENDING
        TaskFilter.IN_PROGRESS -> task.status == TaskStatus.IN_PROGRESS
        TaskFilter.OVERDUE -> task.isOverdue(now)
    }
}
