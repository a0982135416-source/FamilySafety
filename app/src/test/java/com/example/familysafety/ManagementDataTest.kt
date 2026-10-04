package com.example.familysafety

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ManagementDataTest {
    private val text: (Int) -> String = { it.toString() }
    @Before fun reset() { MockTaskItemDataSource.resetForTests(); MockMemberDataSource.resetForTests(); MockTaskDataSource.resetForTests() }

    @Test fun initialMembersHaveExpectedRoles_andUniqueIds() {
        val members = MockMemberDataSource.getMembers()
        assertEquals(3, members.size)
        assertEquals(1, members.count { it.role == MemberRole.ADMIN })
        assertEquals(2, members.count { it.role == MemberRole.MEMBER })
        assertEquals(members.size, members.map { it.id }.toSet().size)
    }

    @Test fun requiredMemberFieldsRejectWhitespace_withoutMutation() {
        val initial = MockMemberDataSource.getMembers()
        listOf(Triple(" ", "account", "email"), Triple("name", "", "email"),
            Triple("name", "account", "\n")).forEach { (name, account, email) ->
            assertNull(MockMemberDataSource.addMember(name, account, email, MemberRole.MEMBER))
        }
        assertEquals(initial, MockMemberDataSource.getMembers())
    }

    @Test fun addMembersAssignsUniqueIdsAndTrimsFields() {
        val member = MockMemberDataSource.addMember(" Morgan ", " morgan ", " m@example.test ", MemberRole.ADMIN)!!
        assertEquals("Morgan", member.name)
        assertEquals("morgan", member.account)
        assertEquals("m@example.test", member.email)
        assertEquals(MemberRole.ADMIN, member.role)
        MockMemberDataSource.addMember("Robin", "robin", "r@example.test", MemberRole.MEMBER)
        val list = MockMemberDataSource.getMembers()
        assertEquals(5, list.size)
        assertEquals(5, list.map { it.id }.toSet().size)
    }

    @Test fun addTaskUsesSelectedMemberSharedStateAndPending() {
        val member = MockMemberDataSource.addMember("Morgan", "morgan", "m@example.test", MemberRole.MEMBER)!!
        val now = System.currentTimeMillis()
        val original = MockTaskDataSource.getTasks(text)
        val item = MockTaskItemDataSource.addItem(" New task ", " Details ", text)!!
        val added = MockTaskDataSource.addTask(item.id, member.id, now + 86_400_000, text, now)!!
        assertEquals(TaskStatus.PENDING, added.status)
        assertEquals(item.id, added.taskItemId)
        assertEquals(item.title, added.title)
        assertEquals(item.description, added.description)
        assertEquals(member.id, added.assigneeId)
        assertEquals(member.name, added.assignee)
        assertFalse(added.isOverdue(now))
        val shared = MockTaskDataSource.getTasks(text)
        assertEquals(original.size + 1, shared.size)
        assertEquals(added, shared.last())
        assertTrue(shared.filterTasks(TaskFilter.ALL, now).contains(added))
        assertFalse(shared.filterTasks(TaskFilter.ALL, now).any { it.status == TaskStatus.COMPLETED })
    }

    @Test fun invalidTasksDoNotMutateSharedState() {
        val tasks = MockTaskDataSource.getTasks(text)
        val now = System.currentTimeMillis()
        assertNull(MockTaskDataSource.addTask(99, 1, now + 1_000, text, now))
        assertNull(MockTaskDataSource.addTask(0, 1, now + 1_000, text, now))
        assertNull(MockTaskDataSource.addTask(1, 99, now + 1_000, text, now))
        assertNull(MockTaskDataSource.addTask(1, 1, now, text, now))
        assertEquals(tasks, MockTaskDataSource.getTasks(text))
    }

    @Test fun sharedTasksKeepDeadlinesStatusAndUserTextAcrossLocaleReads() {
        val now = System.currentTimeMillis()
        val item = MockTaskItemDataSource.addItem("User title", "User text", text)!!
        val added = MockTaskDataSource.addTask(item.id, 1, now + 1_000_000, text, now)!!
        var tasks = MockTaskDataSource.getTasks(text)
        tasks = MockTaskDataSource.updateStatus(tasks, added.id, TaskStatus.IN_PROGRESS)
        MockTaskDataSource.replaceTasks(tasks)
        val localized = MockTaskDataSource.getTasks { "localized:$it" }
        assertEquals(added.copy(status = TaskStatus.IN_PROGRESS), localized.last())
        val expired = localized.last().dueDate + 1
        assertTrue(localized.last().isOverdue(expired))
        MockTaskDataSource.replaceTasks(MockTaskDataSource.updateStatus(localized, added.id, TaskStatus.COMPLETED))
        val completed = MockTaskDataSource.getTasks(text).last()
        assertFalse(completed.isOverdue(expired))
        assertNull(completed.transitionTo(TaskStatus.PENDING))
        assertFalse(MockTaskDataSource.getTasks(text).filterTasks(TaskFilter.ALL, expired).contains(completed))
    }
    @Test fun taskItemsHaveThreeLocalizedSeedsAndUniqueSequentialIds() {
        val initial = MockTaskItemDataSource.getItems(text)
        assertEquals(3, initial.size)
        assertEquals(3, initial.map { it.id }.toSet().size)
        val added = MockTaskItemDataSource.addItem(" Custom item ", "", text)!!
        val next = MockTaskItemDataSource.addItem("Second item", " Details ", text)!!
        assertEquals(initial.maxOf { it.id } + 1, added.id)
        assertEquals(added.id + 1, next.id)
        assertEquals("Custom item", added.title)
        assertEquals("", added.description)
        assertEquals("Details", next.description)
        val localized = MockTaskItemDataSource.getItems { "localized:$it" }
        assertEquals(5, localized.size)
        assertEquals(5, localized.map { it.id }.toSet().size)
        assertTrue(localized.first().title.startsWith("localized:"))
        assertEquals(added, localized[3])
        assertEquals(next, localized[4])
    }

    @Test fun blankItemTitleDoesNotMutateList() {
        val initial = MockTaskItemDataSource.getItems(text)
        assertNull(MockTaskItemDataSource.addItem(" ", "description", text))
        assertEquals(initial, MockTaskItemDataSource.getItems(text))
    }

    @Test fun taskWithOptionalItemDescriptionIsValid_andLegacyTasksRemainCompatible() {
        val item = MockTaskItemDataSource.addItem("Optional description", "", text)!!
        val now = System.currentTimeMillis()
        val task = MockTaskDataSource.addTask(item.id, 2, now + 1_000, text, now)!!
        assertEquals(item.id, task.taskItemId)
        assertEquals("", task.description)
        assertEquals(TaskStatus.PENDING, task.status)
        assertEquals(setOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED), TaskStatus.entries.toSet())
        assertTrue(MockTaskDataSource.create(text).all { it.taskItemId == null })
    }
}
