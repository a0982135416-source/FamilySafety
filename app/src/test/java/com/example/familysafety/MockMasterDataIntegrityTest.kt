package com.example.familysafety

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Initial master data contracts and synchronization regressions; no UI changes. */
class MockMasterDataIntegrityTest {
    private val text: (Int) -> String = { "resource:$it" }
    private val now = 1_000_000_000L
    private val expectedItems = listOf(2, 1, 4, 5, 6, 7, 8)
    private val expectedMembers = listOf(1, 2, 1, 2, 1, 2, 1)
    private val expectedStatuses = listOf(TaskStatus.PENDING, TaskStatus.PENDING,
        TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS, TaskStatus.PENDING,
        TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED)

    @Before fun reset() {
        MockAuthDataSource.resetForTests()
        MockMemberDataSource.resetForTests()
        MockTaskItemDataSource.resetForTests()
        MockTaskDataSource.resetForTests()
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
    }

    @Test fun initialCountsIdsMappingsStatusesAndDeadlinesRemainCorrect() {
        val tasks = MockTaskDataSource.create(text, now)
        val items = MockTaskItemDataSource.getItems(text)
        assertEquals(7, tasks.size)
        assertEquals(8, items.size)
        assertEquals(3, MockMemberDataSource.getMembers().size)
        assertEquals((1L..7L).toList(), tasks.map { it.id })
        assertEquals((1..8).toList(), items.map { it.id })
        assertEquals(expectedItems, tasks.map { it.taskItemId })
        assertEquals(expectedMembers, tasks.map { it.assigneeId })
        assertEquals(expectedStatuses, tasks.map { it.status })
        assertEquals(listOf(2L, 3L, 4L, 5L, -1L, -2L, -3L)
            .map { now + it * 3_600_000L }, tasks.map { it.dueDate })
        assertEquals(6, tasks.filterTasks(TaskFilter.ALL, now).size)
    }

    @Test fun everyInitialTaskReferencesExistingMastersWithMatchingText() {
        val items = MockTaskItemDataSource.getItems(text).associateBy { it.id }
        val members = MockMemberDataSource.getMembers().associateBy { it.id }
        MockTaskDataSource.create(text, now).forEach { task ->
            assertNotNull(task.taskItemId)
            assertNotNull(task.assigneeId)
            val item = items[task.taskItemId]
            val member = members[task.assigneeId]
            assertNotNull(item); assertNotNull(member)
            assertEquals(item!!.title, task.title)
            assertEquals(item.description, task.description)
            assertEquals(member!!.name, task.assignee)
        }
    }

    @Test fun rawFixtureMemberNamesDoNotUseLocalizedLegacyAliases() {
        val localized: (Int) -> String = {
            when (it) {
                R.string.alert_task_mock_member_alex -> "小安"
                R.string.alert_task_mock_member_jamie -> "小佳"
                else -> "localized:$it"
            }
        }
        val members = MockMemberDataSource.getMembers().associateBy { it.id }
        MockTaskDataSource.create(localized, now).forEach {
            assertEquals(members[it.assigneeId]!!.name, it.assignee)
        }
    }

    @Test fun sharedLocaleReadsPreserveAllInitialIdentityAndScheduleFields() {
        val original = MockTaskDataSource.create(text, now)
        MockTaskDataSource.replaceTasks(original)
        val localized: (Int) -> String = { "localized:$it" }
        val items = MockTaskItemDataSource.getItems(localized).associateBy { it.id }
        val tasks = MockTaskDataSource.getTasks(localized)
        assertEquals(7, tasks.size)
        original.zip(tasks).forEach { (before, after) ->
            assertEquals(before.id, after.id)
            assertEquals(before.taskItemId, after.taskItemId)
            assertEquals(before.assigneeId, after.assigneeId)
            assertEquals(before.dueDate, after.dueDate)
            assertEquals(before.status, after.status)
            assertEquals(items[after.taskItemId]!!.title, after.title)
            assertEquals(items[after.taskItemId]!!.description, after.description)
        }
    }

    @Test fun everyInitialTaskSynchronizesEditedItemWithoutChangingOtherFields() {
        val original = MockTaskDataSource.create(text, now)
        MockTaskDataSource.replaceTasks(original)
        original.forEach { task ->
            assertNotNull(MockTaskItemDataSource.updateItem(task.taskItemId!!,
                "Updated item ${task.taskItemId}", "Updated description ${task.taskItemId}", text))
        }
        val items = MockTaskItemDataSource.getItems(text).associateBy { it.id }
        val tasks = MockTaskDataSource.getTasks(text)
        assertEquals(original.size, tasks.size)
        original.zip(tasks).forEach { (before, after) ->
            val item = items[before.taskItemId]!!
            assertEquals(before.copy(title = item.title, description = item.description), after)
        }
        assertEquals(TaskStatus.COMPLETED, tasks.single { it.id == 7L }.status)
    }

    @Test fun sameItemSynchronizesPendingInProgressAndCompletedTasks() {
        val original = MockTaskDataSource.create(text, now)
        val trash = original.single { it.id == 2L }
        val linked = listOf(trash,
            trash.copy(id = 20L, status = TaskStatus.IN_PROGRESS),
            trash.copy(id = 21L, status = TaskStatus.COMPLETED))
        val unrelated = original.filterNot { it.id == trash.id }
        MockTaskDataSource.replaceTasks(unrelated + linked)
        val updated = MockTaskItemDataSource.updateItem(1, "New trash title", "New trash description", text)!!
        val tasks = MockTaskDataSource.getTasks(text).associateBy { it.id }
        assertEquals(unrelated.size + linked.size, tasks.size)
        linked.forEach { before ->
            assertEquals(before.copy(title = updated.title, description = updated.description), tasks[before.id])
        }
        unrelated.forEach { assertEquals(it, tasks[it.id]) }
    }
}
