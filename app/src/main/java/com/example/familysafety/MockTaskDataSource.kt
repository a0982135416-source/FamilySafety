package com.example.familysafety

/** STEP 5-1：獨立 Mock 資料來源，截止時間相對於建立時刻。 */
object MockTaskDataSource {
    // STEP 6-2：唯一共享清單；create 保留為獨立 fixture factory。
    private var sharedTasks: List<Task>? = null

    fun getTasks(getString: (Int) -> String): List<Task> {
        val tasks = sharedTasks ?: create(getString).also { sharedTasks = it }
        val localized = create(getString).associateBy { it.id }
        return tasks.map { task ->
            localized[task.id]?.let { label ->
                task.copy(title = label.title, description = label.description, assignee = label.assignee)
            } ?: task
        }.also { sharedTasks = it }
    }

    internal fun replaceTasks(tasks: List<Task>) { sharedTasks = tasks.toList() }
    internal fun resetForTests() { sharedTasks = null }

    fun addTask(taskItemId: Int, memberId: Int, dueDate: Long,
                getString: (Int) -> String, now: Long = System.currentTimeMillis()): Task? {
        val item = MockTaskItemDataSource.getItems(getString).find { it.id == taskItemId } ?: return null
        val member = MockMemberDataSource.getMembers().find { it.id == memberId } ?: return null
        if (dueDate <= now) return null
        val tasks = getTasks(getString)
        val task = Task((tasks.maxOfOrNull { it.id } ?: 7L).coerceAtLeast(7L) + 1,
            item.title, item.description, member.name, dueDate, TaskStatus.PENDING, member.id, item.id)
        sharedTasks = tasks + task
        return task
    }

    /** 狀態更新入口：以最新資料驗證，不修改傳入清單或其他任務。 */
    fun updateStatus(tasks: List<Task>, id: Long, next: TaskStatus): List<Task> {
        val current = tasks.find { it.id == id } ?: return tasks
        val updated = current.transitionTo(next) ?: return tasks
        return tasks.map { if (it.id == id) updated else it }
    }

    fun create(getString: (Int) -> String, now: Long = System.currentTimeMillis()): List<Task> {
        val hour = 60L * 60_000L
        fun task(id: Long, title: Int, description: Int, member: Int,
                 offset: Long, status: TaskStatus) = Task(
            id, getString(title), getString(description), getString(member), now + offset, status,
            if (member == R.string.alert_task_mock_member_alex) 1 else 2,
        )
        return listOf(
            task(1, R.string.alert_task_mock_kitchen, R.string.alert_task_mock_kitchen_description,
                R.string.alert_task_mock_member_alex, 2 * hour, TaskStatus.PENDING),
            task(2, R.string.alert_task_mock_trash, R.string.alert_task_mock_trash_description,
                R.string.alert_task_mock_member_jamie, 3 * hour, TaskStatus.PENDING),
            task(3, R.string.alert_task_mock_laundry, R.string.alert_task_mock_laundry_description,
                R.string.alert_task_mock_member_alex, 4 * hour, TaskStatus.IN_PROGRESS),
            task(4, R.string.alert_task_mock_plants, R.string.alert_task_mock_plants_description,
                R.string.alert_task_mock_member_jamie, 5 * hour, TaskStatus.IN_PROGRESS),
            task(5, R.string.alert_task_mock_dishes, R.string.alert_task_mock_dishes_description,
                R.string.alert_task_mock_member_alex, -hour, TaskStatus.PENDING),
            task(6, R.string.task_mock_tidy, R.string.task_mock_tidy_description,
                R.string.alert_task_mock_member_jamie, -2 * hour, TaskStatus.IN_PROGRESS),
            task(7, R.string.task_mock_completed, R.string.task_mock_completed_description,
                R.string.alert_task_mock_member_alex, -3 * hour, TaskStatus.COMPLETED),
        )
    }
}
