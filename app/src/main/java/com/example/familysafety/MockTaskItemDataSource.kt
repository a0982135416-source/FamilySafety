package com.example.familysafety

/** 同一 process 共用；預設項目隨語系顯示，使用者輸入保留原文。 */
object MockTaskItemDataSource {
    private var items: List<TaskItem>? = null
    private var nextId = 9
    private val editedIds = mutableSetOf<Int>()

    private fun initial(getString: (Int) -> String) = listOf(
        TaskItem(1, getString(R.string.alert_task_mock_trash), getString(R.string.alert_task_mock_trash_description)),
        TaskItem(2, getString(R.string.alert_task_mock_kitchen), getString(R.string.alert_task_mock_kitchen_description)),
        TaskItem(3, getString(R.string.management_item_mock_gas), getString(R.string.management_item_mock_gas_description)),
        TaskItem(4, getString(R.string.alert_task_mock_laundry), getString(R.string.alert_task_mock_laundry_description)),
        TaskItem(5, getString(R.string.alert_task_mock_plants), getString(R.string.alert_task_mock_plants_description)),
        TaskItem(6, getString(R.string.alert_task_mock_dishes), getString(R.string.alert_task_mock_dishes_description)),
        TaskItem(7, getString(R.string.task_mock_tidy), getString(R.string.task_mock_tidy_description)),
        TaskItem(8, getString(R.string.task_mock_completed), getString(R.string.task_mock_completed_description)),
    )

    fun getItems(getString: (Int) -> String): List<TaskItem> {
        val current = items ?: initial(getString)
        val localized = initial(getString).associateBy { it.id }
        return current.map { if (it.id in editedIds) it else localized[it.id] ?: it }.also { items = it }
    }

    /** 標題必填，說明可空白；ID 單調增加，不重用刪除的 ID。 */
    fun addItem(title: String, description: String, getString: (Int) -> String): TaskItem? {
        if (!MockAuthDataSource.canManage() || title.isBlank()) return null
        val current = getItems(getString)
        val item = TaskItem(nextId++, title.trim(), description.trim())
        items = current + item
        return item
    }

    fun updateItem(id: Int, title: String, description: String, getString: (Int) -> String): TaskItem? {
        if (!MockAuthDataSource.canManage() || title.isBlank()) return null
        val current = getItems(getString)
        val old = current.find { it.id == id } ?: return null
        val updated = old.copy(title = title.trim(), description = description.trim())
        editedIds.add(id)
        items = current.map { if (it.id == id) updated else it }
        // Reads of the shared task list resolve the current item by its stable ID.
        MockTaskDataSource.getTasks(getString)
        return updated
    }

    fun deleteItem(id: Int, getString: (Int) -> String): Boolean {
        if (!MockAuthDataSource.canManage() || MockTaskDataSource.referencesItem(id, getString)) return false
        val current = getItems(getString)
        if (current.none { it.id == id }) return false
        items = current.filterNot { it.id == id }
        editedIds.remove(id)
        return true
    }

    internal fun resetForTests() { items = null; nextId = 9; editedIds.clear() }
}
