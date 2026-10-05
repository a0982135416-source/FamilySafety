package com.example.familysafety

/** STEP 9-2: view data derived from the shared Task Center source; no separate mutable state. */
object MockTaskAlertDataSource {
    fun create(getString: (Int) -> String): List<TaskAlert> =
        MockTaskDataSource.getTasks(getString).map { task ->
            TaskAlert(task.id, task.id, task.title, task.description,
                task.assignee, task.dueDate, task.status)
        }
}
