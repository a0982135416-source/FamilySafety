package com.example.familysafety

import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

class HomeTaskSummaryTest {
    @Test fun localDateBoundariesIncludeCompletedAndExcludeOtherDays() {
        val start = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val next = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        fun task(id: Long, at: Long, status: TaskStatus = TaskStatus.PENDING) =
            Task(id, "title", "description", "member", at, status)
        val tasks = listOf(task(1, start.timeInMillis - 1), task(2, start.timeInMillis),
            task(3, next - 1, TaskStatus.COMPLETED), task(4, next))
        assertEquals(listOf(2L, 3L), tasks.dueToday(start.timeInMillis + 1).map { it.id })
        assertEquals(1, tasks.dueToday(start.timeInMillis).count { it.status == TaskStatus.COMPLETED })
        assertTrue(emptyList<Task>().dueToday(start.timeInMillis).isEmpty())
    }
}
