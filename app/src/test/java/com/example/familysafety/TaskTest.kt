package com.example.familysafety

import org.junit.Assert.*
import org.junit.Test

/** STEP 5-1：驗證資料分布、逾期邊界與主列表規則。 */
class TaskTest {
    private val now = 1_000_000_000L
    private fun mock() = MockTaskDataSource.create({ it.toString() }, now)

    @Test fun filters_includeOverdueInOriginalStatus_andExcludeCompleted() {
        val tasks = mock()
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), tasks.filterTasks(TaskFilter.ALL, now).map { it.id })
        assertEquals(listOf(1L, 2L, 5L), tasks.filterTasks(TaskFilter.PENDING, now).map { it.id })
        assertEquals(listOf(3L, 4L, 6L), tasks.filterTasks(TaskFilter.IN_PROGRESS, now).map { it.id })
        assertEquals(listOf(5L, 6L), tasks.filterTasks(TaskFilter.OVERDUE, now).map { it.id })
        assertEquals(TaskStatus.PENDING, tasks[4].status)
        assertEquals(TaskStatus.IN_PROGRESS, tasks[5].status)
        assertEquals(setOf("PENDING", "IN_PROGRESS", "COMPLETED"), TaskStatus.entries.map { it.name }.toSet())
    }

    @Test fun overdue_usesStrictDeadline_andNeverIncludesCompleted() {
        val task = Task(1, "title", "description", "member", now, TaskStatus.PENDING)
        assertFalse(task.isOverdue(now))
        assertTrue(task.isOverdue(now + 1))
        assertFalse(task.copy(status = TaskStatus.COMPLETED).isOverdue(now + 1))
        assertFalse(task.isOverdue(now - 1))
    }

    @Test fun emptyAndCompletedOnly_produceEmptyForEveryFilter() {
        val completed = mock().filter { it.status == TaskStatus.COMPLETED }
        TaskFilter.entries.forEach {
            assertTrue(emptyList<Task>().filterTasks(it, now).isEmpty())
            assertTrue(completed.filterTasks(it, now).isEmpty())
        }
    }

    @Test fun mockDeadlines_moveWithCreationTime() {
        val later = MockTaskDataSource.create({ it.toString() }, now + 123_456)
        mock().zip(later).forEach { (first, second) ->
            assertEquals(123_456L, second.dueDate - first.dueDate)
        }
        assertEquals(7, mock().size)
        assertEquals(1, mock().count { it.status == TaskStatus.COMPLETED })
    }
}
