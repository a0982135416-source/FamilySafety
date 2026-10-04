package com.example.familysafety

/** 同一 process 共用；預設項目隨語系顯示，使用者輸入保留原文。 */
object MockTaskItemDataSource {
    private var items: List<TaskItem>? = null

    private fun initial(getString: (Int) -> String) = listOf(
        TaskItem(1, getString(R.string.alert_task_mock_trash), getString(R.string.alert_task_mock_trash_description)),
        TaskItem(2, getString(R.string.alert_task_mock_kitchen), getString(R.string.alert_task_mock_kitchen_description)),
        TaskItem(3, getString(R.string.management_item_mock_gas), getString(R.string.management_item_mock_gas_description)),
    )

    fun getItems(getString: (Int) -> String): List<TaskItem> {
        val current = items ?: initial(getString)
        val localized = initial(getString).associateBy { it.id }
        return current.map { localized[it.id] ?: it }.also { items = it }
    }

    /** 標題必填，說明可空白；ID 為目前最大值加一。 */
    fun addItem(title: String, description: String, getString: (Int) -> String): TaskItem? {
        if (title.isBlank()) return null
        val current = getItems(getString)
        val item = TaskItem((current.maxOfOrNull { it.id } ?: 0) + 1, title.trim(), description.trim())
        items = current + item
        return item
    }

    internal fun resetForTests() { items = null }
}
