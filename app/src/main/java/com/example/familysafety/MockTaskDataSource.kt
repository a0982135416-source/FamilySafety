package com.example.familysafety

/** STEP 5-1：獨立 Mock 資料來源，截止時間相對於建立時刻。 */
object MockTaskDataSource {
    // STEP 6-2：唯一共享清單；create 保留為獨立 fixture factory。
    private var sharedTasks: List<Task>? = null
    private var nextId = 8L

    fun getTasks(getString: (Int) -> String): List<Task> {
        val tasks = sharedTasks ?: create(getString).also { sharedTasks = it }
        val localized = create(getString).associateBy { it.id }
        val items = MockTaskItemDataSource.getItems(getString).associateBy { it.id }
        val members = MockMemberDataSource.getMembers().associateBy { it.id }
        return tasks.map { task ->
            val item = items[task.taskItemId]
            val seed = localized[task.id]
            task.copy(title = item?.title ?: seed?.title ?: task.title,
                description = item?.description ?: seed?.description ?: task.description,
                assignee = members[task.assigneeId]?.name ?: task.assignee)
        }.also { sharedTasks = it }
    }

    internal fun replaceTasks(tasks: List<Task>) { sharedTasks = tasks.toList(); nextId = maxOf(nextId, (tasks.maxOfOrNull { it.id } ?: 7L) + 1) }
    internal fun resetForTests() { sharedTasks = null; nextId = 8L }

    fun addTask(taskItemId: Int, memberId: Int, dueDate: Long,
                getString: (Int) -> String, now: Long = System.currentTimeMillis()): Task? {
        if (!MockAuthDataSource.canManage()) return null
        val item = MockTaskItemDataSource.getItems(getString).find { it.id == taskItemId } ?: return null
        val member = MockMemberDataSource.getMembers().find { it.id == memberId } ?: return null
        if (dueDate <= now) return null
        val tasks = getTasks(getString)
        val task = Task(nextId++,
            item.title, item.description, member.name, dueDate, TaskStatus.PENDING, member.id, item.id)
        sharedTasks = tasks + task
        return task
    }

    fun referencesMember(id: Int, getString: (Int) -> String) = getTasks(getString).any { it.assigneeId == id }
    fun referencesItem(id: Int, getString: (Int) -> String) = getTasks(getString).any { it.taskItemId == id }

    /** Management edits preserve status; only Task Center performs status transitions. */
    fun updateTask(id: Long, taskItemId: Int?, memberId: Int, dueDate: Long,
                   getString: (Int) -> String, now: Long = System.currentTimeMillis()): Task? {
        if (!MockAuthDataSource.canManage()) return null
        val tasks = getTasks(getString)
        val old = tasks.find { it.id == id } ?: return null
        // Existing overdue deadlines may stay unchanged; a changed deadline must be in the future.
        if (dueDate <= now && dueDate != old.dueDate) return null
        val item = taskItemId?.let { key -> MockTaskItemDataSource.getItems(getString).find { it.id == key } ?: return null }
        if (item == null && old.taskItemId != null) return null
        val member = MockMemberDataSource.getMembers().find { it.id == memberId } ?: return null
        val updated = old.copy(taskItemId = item?.id, title = item?.title ?: old.title, description = item?.description ?: old.description,
            assigneeId = member.id, assignee = member.name, dueDate = dueDate)
        sharedTasks = tasks.map { if (it.id == id) updated else it }
        return updated
    }

    fun deleteTask(id: Long, getString: (Int) -> String): Boolean {
        if (!MockAuthDataSource.canManage()) return false
        val tasks = getTasks(getString)
        if (tasks.none { it.id == id }) return false
        sharedTasks = tasks.filterNot { it.id == id }
        return true
    }

    /** 狀態更新入口：以最新資料驗證，不修改傳入清單或其他任務。 */
    fun updateStatus(tasks: List<Task>, id: Long, next: TaskStatus): List<Task> {
        val current = tasks.find { it.id == id } ?: return tasks
        val updated = current.transitionTo(next) ?: return tasks
        return tasks.map { if (it.id == id) updated else it }
    }

    fun create(getString: (Int) -> String, now: Long = System.currentTimeMillis()): List<Task> {
        val hour = 60L * 60_000L
        fun task(id: Long, title: Int, description: Int, memberId: Int, taskItemId: Int,
                 offset: Long, status: TaskStatus) = Task(
            id, getString(title), getString(description),
            if (memberId == 1) "Alex" else "Jamie", now + offset, status,
            memberId, taskItemId,
        )
        return listOf(
            task(1, R.string.alert_task_mock_kitchen, R.string.alert_task_mock_kitchen_description,
                memberId = 1, taskItemId = 2, 2 * hour, TaskStatus.PENDING),
            task(2, R.string.alert_task_mock_trash, R.string.alert_task_mock_trash_description,
                memberId = 2, taskItemId = 1, 3 * hour, TaskStatus.PENDING),
            task(3, R.string.alert_task_mock_laundry, R.string.alert_task_mock_laundry_description,
                memberId = 1, taskItemId = 4, 4 * hour, TaskStatus.IN_PROGRESS),
            task(4, R.string.alert_task_mock_plants, R.string.alert_task_mock_plants_description,
                memberId = 2, taskItemId = 5, 5 * hour, TaskStatus.IN_PROGRESS),
            task(5, R.string.alert_task_mock_dishes, R.string.alert_task_mock_dishes_description,
                memberId = 1, taskItemId = 6, -hour, TaskStatus.PENDING),
            task(6, R.string.task_mock_tidy, R.string.task_mock_tidy_description,
                memberId = 2, taskItemId = 7, -2 * hour, TaskStatus.IN_PROGRESS),
            task(7, R.string.task_mock_completed, R.string.task_mock_completed_description,
                memberId = 1, taskItemId = 8, -3 * hour, TaskStatus.COMPLETED),
        )
    }
}
