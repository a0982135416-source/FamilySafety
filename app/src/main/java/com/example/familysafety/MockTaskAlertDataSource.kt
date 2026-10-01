package com.example.familysafety

/** STEP 4-4：本機測試資料，未連接 Server；文字由目前語系資源提供。 */
object MockTaskAlertDataSource {
    fun create(
        getString: (Int) -> String,
        now: Long = System.currentTimeMillis(),
    ): List<TaskAlert> {
        val hour = 60L * 60_000L
        fun task(id: Long, title: Int, description: Int, member: Int,
                 offset: Long, status: TaskStatus) = TaskAlert(
            id = id,
            taskId = 100L + id,
            title = getString(title),
            description = getString(description),
            assigneeName = getString(member),
            dueDate = now + offset,
            status = status,
        )

        return listOf(
            task(1, R.string.alert_task_mock_kitchen, R.string.alert_task_mock_kitchen_description,
                R.string.alert_task_mock_member_alex, 2 * hour, TaskStatus.PENDING),
            task(2, R.string.alert_task_mock_trash, R.string.alert_task_mock_trash_description,
                R.string.alert_task_mock_member_jamie, -hour, TaskStatus.PENDING),
            task(3, R.string.alert_task_mock_laundry, R.string.alert_task_mock_laundry_description,
                R.string.alert_task_mock_member_alex, 4 * hour, TaskStatus.IN_PROGRESS),
            task(4, R.string.alert_task_mock_plants, R.string.alert_task_mock_plants_description,
                R.string.alert_task_mock_member_jamie, -2 * hour, TaskStatus.IN_PROGRESS),
            // 超過截止時間但已完成：不得出現在 All 或 Overdue。
            task(5, R.string.alert_task_mock_dishes, R.string.alert_task_mock_dishes_description,
                R.string.alert_task_mock_member_alex, -3 * hour, TaskStatus.COMPLETED),
        )
    }
}
