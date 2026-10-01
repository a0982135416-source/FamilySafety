package com.example.familysafety

/** STEP 4-4：任務原始狀態；逾期是時間條件，不是儲存狀態。 */
enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }

enum class TaskAlertFilter { ALL, PENDING, IN_PROGRESS, OVERDUE }

data class TaskAlert(
    val id: Long,
    val taskId: Long,
    val title: String,
    val description: String,
    val assigneeName: String,
    val dueDate: Long,
    val status: TaskStatus,
) {
    // 可傳入同一個 now，讓數量、篩選與卡片使用一致的時間。
    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean =
        now > dueDate && status != TaskStatus.COMPLETED
}

/** 警報中心只列出未完成任務；逾期任務仍保有原本狀態。 */
fun List<TaskAlert>.filterTaskAlerts(filter: TaskAlertFilter, now: Long): List<TaskAlert> =
    filter { alert ->
        alert.status != TaskStatus.COMPLETED && when (filter) {
            TaskAlertFilter.ALL -> true
            TaskAlertFilter.PENDING -> alert.status == TaskStatus.PENDING
            TaskAlertFilter.IN_PROGRESS -> alert.status == TaskStatus.IN_PROGRESS
            TaskAlertFilter.OVERDUE -> alert.isOverdue(now)
        }
    }.sortedBy { it.dueDate }
