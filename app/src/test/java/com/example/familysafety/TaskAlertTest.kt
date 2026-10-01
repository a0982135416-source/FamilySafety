package com.example.familysafety

import org.junit.Assert.*
import org.junit.Test

/** STEP 4-4：用固定時間驗證規則，不依賴測試當天日期或 Android 畫面。 */
class TaskAlertTest {
    private val now = 1_800_000_000_000L
    private fun mock(at: Long = now) = MockTaskAlertDataSource.create({ "resource-$it" }, at)

    @Test fun all_excludesCompleted_andSortsByDeadline() {
        val result = mock().filterTaskAlerts(TaskAlertFilter.ALL, now)
        assertEquals(listOf(4L, 2L, 1L, 3L), result.map { it.id })
        assertTrue(result.none { it.status == TaskStatus.COMPLETED })
    }

    @Test fun pending_includesBothFutureAndOverdue() {
        val result = mock().filterTaskAlerts(TaskAlertFilter.PENDING, now)
        assertEquals(setOf(1L, 2L), result.map { it.id }.toSet())
        assertEquals(1, result.count { it.isOverdue(now) })
    }

    @Test fun inProgress_includesBothFutureAndOverdue() {
        val result = mock().filterTaskAlerts(TaskAlertFilter.IN_PROGRESS, now)
        assertEquals(setOf(3L, 4L), result.map { it.id }.toSet())
        assertEquals(1, result.count { it.isOverdue(now) })
    }

    @Test fun overdue_crossesOriginalStatuses_withoutMutatingRecords() {
        val alerts = mock()
        val original = alerts.toList()
        val result = alerts.filterTaskAlerts(TaskAlertFilter.OVERDUE, now)
        assertEquals(setOf(2L, 4L), result.map { it.id }.toSet())
        assertEquals(setOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS), result.map { it.status }.toSet())
        assertEquals(original, alerts)
    }

    @Test fun deadlineBoundary_usesStrictGreaterThan_forBothUnfinishedStatuses() {
        for (status in listOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)) {
            val alert = mock().first().copy(dueDate = now, status = status)
            assertFalse(alert.isOverdue(now - 1))
            assertFalse(alert.isOverdue(now))
            assertTrue(alert.isOverdue(now + 1))
        }
    }

    @Test fun completed_isNeverOverdue_evenAfterDeadline() {
        val completed = mock().single { it.status == TaskStatus.COMPLETED }
        assertTrue(completed.dueDate < now)
        assertFalse(completed.isOverdue(now))
        TaskAlertFilter.entries.forEach { filter ->
            assertTrue(listOf(completed).filterTaskAlerts(filter, now).isEmpty())
        }
    }

    @Test fun overdue_isRecomputed_whenTimeMovesForwardOrBackward() {
        val alerts = mock()
        assertEquals(2, alerts.filterTaskAlerts(TaskAlertFilter.OVERDUE, now).size)
        assertEquals(4, alerts.filterTaskAlerts(TaskAlertFilter.OVERDUE, now + 5 * 3_600_000L).size)
        assertTrue(alerts.filterTaskAlerts(TaskAlertFilter.OVERDUE, now - 5 * 3_600_000L).isEmpty())
        assertEquals(2, alerts.filterTaskAlerts(TaskAlertFilter.PENDING, now + 5 * 3_600_000L).size)
    }

    @Test fun emptyInput_returnsEmpty_forEveryFilter() {
        TaskAlertFilter.entries.forEach { filter ->
            assertTrue(emptyList<TaskAlert>().filterTaskAlerts(filter, now).isEmpty())
        }
    }

    @Test fun mockDates_areRelativeToCreationTime() {
        val later = now + 365L * 24 * 3_600_000L
        assertEquals(mock().map { it.dueDate - now }, mock(later).map { it.dueDate - later })
        assertEquals(2, mock(later).filterTaskAlerts(TaskAlertFilter.OVERDUE, later).size)
    }

    @Test fun taskFiltering_doesNotChangeEnvironmentMockRecords() {
        val environment = MockEnvironmentAlertDataSource.create()
        val original = environment.toList()
        TaskAlertFilter.entries.forEach { mock().filterTaskAlerts(it, now) }
        assertEquals(original, environment)
        assertEquals(3, environment.size)
        assertEquals(2, environment.count { it.status == EnvironmentAlertStatus.PENDING })
        assertEquals(1, environment.count { it.status == EnvironmentAlertStatus.RESOLVED })
    }
}
