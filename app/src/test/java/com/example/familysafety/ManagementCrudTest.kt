package com.example.familysafety

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** STEP 7-D: stable IDs, permission boundaries, references and shared-data consistency. */
class ManagementCrudTest {
    private val text: (Int) -> String = { "resource:$it" }
    private val future get() = System.currentTimeMillis() + 86_400_000
    @Before fun reset() {
        MockMemberDataSource.resetForTests(); MockTaskItemDataSource.resetForTests()
        MockTaskDataSource.resetForTests(); MockAuthDataSource.resetForTests()
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
    }
    @Test fun memberUpdatePreservesIdCredentialAndSessionAssociation() {
        val updated = MockMemberDataSource.updateMember(1, "Alex Updated", "alex_new", "new@example.test", MemberRole.ADMIN)!!
        assertEquals(1, updated.id)
        assertEquals(updated, MockAuthDataSource.getCurrentMember())
        assertNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
        assertEquals(updated, MockAuthDataSource.login("alex_new", MockAuthDataSource.INITIAL_PASSWORD))
        assertFalse(MockAuthDataSource.emailExists("alex@example.test"))
        assertTrue(MockAuthDataSource.emailExists("new@example.test"))
        assertEquals(updated.name, MockTaskDataSource.getTasks(text).first().assignee)
    }
    @Test fun memberUpdateRequiresFieldsAndExistingId() {
        val before = MockMemberDataSource.getMembers()
        assertNull(MockMemberDataSource.updateMember(2, "", "jamie", "j@example.test", MemberRole.MEMBER))
        assertNull(MockMemberDataSource.updateMember(2, "Jamie", " ", "j@example.test", MemberRole.MEMBER))
        assertNull(MockMemberDataSource.updateMember(2, "Jamie", "jamie", "", MemberRole.MEMBER))
        assertNull(MockMemberDataSource.updateMember(99, "X", "x", "x@example.test", MemberRole.MEMBER))
        assertEquals(before, MockMemberDataSource.getMembers())
    }
    @Test fun unreferencedMemberCanDeleteAndIdsAreNeverReused() {
        assertTrue(MockMemberDataSource.deleteMember(3, text))
        assertFalse(MockAuthDataSource.emailExists("taylor@example.test"))
        val added = MockMemberDataSource.addMember("New", "new", "n@example.test", MemberRole.MEMBER)!!
        assertEquals(4, added.id)
        assertNull(MockAuthDataSource.login("new", MockAuthDataSource.INITIAL_PASSWORD))
        assertTrue(MockMemberDataSource.deleteMember(added.id, text))
        assertEquals(5, MockMemberDataSource.addMember("Next", "next", "next@example.test", MemberRole.MEMBER)!!.id)
    }
    @Test fun referencedMemberCannotDelete() {
        val before = MockMemberDataSource.getMembers()
        assertFalse(MockMemberDataSource.deleteMember(2, text))
        assertEquals(before, MockMemberDataSource.getMembers())
        assertTrue(MockTaskDataSource.getTasks(text).all { task -> task.assigneeId in before.map { it.id } })
    }
    @Test fun signedInAdminCannotDeleteSelfEvenWhenUnreferenced() {
        MockTaskDataSource.replaceTasks(MockTaskDataSource.getTasks(text).filterNot { it.assigneeId == 1 })
        assertFalse(MockTaskDataSource.referencesMember(1, text))
        assertFalse(MockMemberDataSource.deleteMember(1, text))
        assertEquals(1, MockAuthDataSource.getCurrentMember()!!.id)
    }
    @Test fun itemUpdateSynchronizesTasksAndSurvivesLocaleReads() {
        val task = MockTaskDataSource.addTask(1, 2, future, text)!!
        val updated = MockTaskItemDataSource.updateItem(1, "Updated title", "Updated description", text)!!
        assertEquals(1, updated.id)
        assertEquals(updated, MockTaskItemDataSource.getItems { "other:$it" }.first())
        val shared = MockTaskDataSource.getTasks { "other:$it" }.find { it.id == task.id }!!
        assertEquals(updated.title, shared.title); assertEquals(updated.description, shared.description)
        assertEquals(task.taskItemId, shared.taskItemId); assertEquals(task.status, shared.status)
    }
    @Test fun itemUpdateRequiresTitleAndExistingId() {
        val before = MockTaskItemDataSource.getItems(text)
        assertNull(MockTaskItemDataSource.updateItem(1, " ", "Text", text))
        assertNull(MockTaskItemDataSource.updateItem(99, "Title", "Text", text))
        assertEquals(before, MockTaskItemDataSource.getItems(text))
        assertEquals("", MockTaskItemDataSource.updateItem(1, "Title", "", text)!!.description)
    }
    @Test fun unreferencedItemCanDeleteAndIdsAreNeverReused() {
        assertTrue(MockTaskItemDataSource.deleteItem(3, text))
        assertFalse(MockTaskItemDataSource.getItems(text).any { it.id == 3 })
        val item = MockTaskItemDataSource.addItem("New", "", text)!!
        assertEquals(9, item.id)
        assertTrue(MockTaskItemDataSource.deleteItem(item.id, text))
        assertEquals(10, MockTaskItemDataSource.addItem("Next", "", text)!!.id)
    }
    @Test fun referencedItemCannotDeleteIncludingCompletedTasks() {
        val task = MockTaskDataSource.addTask(1, 2, future, text)!!
        var tasks = MockTaskDataSource.getTasks(text)
        tasks = MockTaskDataSource.updateStatus(tasks, task.id, TaskStatus.IN_PROGRESS)
        tasks = MockTaskDataSource.updateStatus(tasks, task.id, TaskStatus.COMPLETED)
        MockTaskDataSource.replaceTasks(tasks)
        assertFalse(MockTaskItemDataSource.deleteItem(1, text))
        assertFalse(MockMemberDataSource.deleteMember(2, text))
        assertEquals(TaskStatus.COMPLETED, MockTaskDataSource.getTasks(text).last().status)
    }
    @Test fun taskUpdateChangesReferencesDeadlineAndTextButPreservesStatusAndId() {
        val task = MockTaskDataSource.addTask(1, 1, future, text)!!
        MockTaskDataSource.replaceTasks(MockTaskDataSource.updateStatus(MockTaskDataSource.getTasks(text), task.id, TaskStatus.IN_PROGRESS))
        val item = MockTaskItemDataSource.addItem("Other item", "Details", text)!!
        val changedDue = future + 86_400_000
        val updated = MockTaskDataSource.updateTask(task.id, item.id, 3, changedDue, text)!!
        assertEquals(task.id, updated.id); assertEquals(item.id, updated.taskItemId)
        assertEquals(3, updated.assigneeId); assertEquals("Taylor", updated.assignee)
        assertEquals(item.title, updated.title); assertEquals(item.description, updated.description)
        assertEquals(changedDue, updated.dueDate); assertEquals(TaskStatus.IN_PROGRESS, updated.status)
        assertEquals(updated, MockTaskDataSource.getTasks(text).last())
    }
    @Test fun taskUpdateRejectsMissingReferencesAndChangedPastDeadline() {
        val task = MockTaskDataSource.addTask(1, 1, future, text)!!
        val before = MockTaskDataSource.getTasks(text)
        assertNull(MockTaskDataSource.updateTask(task.id, 99, 1, future, text))
        assertNull(MockTaskDataSource.updateTask(task.id, 1, 99, future, text))
        assertNull(MockTaskDataSource.updateTask(task.id, 1, 1, 0, text))
        assertNull(MockTaskDataSource.updateTask(task.id, null, 1, future, text))
        assertEquals(before, MockTaskDataSource.getTasks(text))
    }
    @Test fun legacyOverdueTaskCanKeepItsOptionalItemAndDeadlineDuringEdit() {
        // Explicit compatibility fixture; current initial data now has complete item references.
        val tasks = MockTaskDataSource.getTasks(text)
        val old = tasks.find { it.id == 5L }!!.copy(taskItemId = null)
        MockTaskDataSource.replaceTasks(tasks.map { if (it.id == old.id) old else it })
        val updated = MockTaskDataSource.updateTask(old.id, null, 3, old.dueDate, text)!!
        assertNull(updated.taskItemId); assertEquals(old.title, updated.title)
        assertEquals(old.dueDate, updated.dueDate); assertEquals(old.status, updated.status)
        assertEquals(updated, MockTaskDataSource.getTasks(text).find { it.id == old.id })
    }
    @Test fun deletedTaskDisappearsFromSharedListAndReleasesReferences() {
        val task = MockTaskDataSource.addTask(3, 3, future, text)!!
        assertFalse(MockTaskItemDataSource.deleteItem(3, text))
        assertFalse(MockMemberDataSource.deleteMember(3, text))
        assertTrue(MockTaskDataSource.deleteTask(task.id, text))
        assertFalse(MockTaskDataSource.getTasks(text).any { it.id == task.id })
        assertFalse(MockTaskDataSource.getTasks(text).filterTasks(TaskFilter.ALL, System.currentTimeMillis()).any { it.id == task.id })
        assertTrue(MockTaskItemDataSource.deleteItem(3, text)); assertTrue(MockMemberDataSource.deleteMember(3, text))
        assertEquals(task.id + 1, MockTaskDataSource.addTask(1, 1, future, text)!!.id)
    }
    @Test fun memberCannotCreateUpdateOrDeleteAnyEntity() {
        val member = MockMemberDataSource.getMembers().last()
        val item = MockTaskItemDataSource.getItems(text).last()
        val task = MockTaskDataSource.getTasks(text).last()
        val before = listOf(MockMemberDataSource.getMembers(), MockTaskItemDataSource.getItems(text), MockTaskDataSource.getTasks(text))
        MockAuthDataSource.login("jamie", MockAuthDataSource.INITIAL_PASSWORD)
        assertNull(MockMemberDataSource.addMember("N", "n", "n@test", MemberRole.MEMBER))
        assertNull(MockMemberDataSource.updateMember(member.id, "N", "n", "n@test", MemberRole.ADMIN))
        assertFalse(MockMemberDataSource.deleteMember(member.id, text))
        assertNull(MockTaskItemDataSource.addItem("N", "", text))
        assertNull(MockTaskItemDataSource.updateItem(item.id, "N", "", text)); assertFalse(MockTaskItemDataSource.deleteItem(item.id, text))
        assertNull(MockTaskDataSource.addTask(item.id, member.id, future, text))
        assertNull(MockTaskDataSource.updateTask(task.id, item.id, member.id, future, text)); assertFalse(MockTaskDataSource.deleteTask(task.id, text))
        assertEquals(before, listOf(MockMemberDataSource.getMembers(), MockTaskItemDataSource.getItems(text), MockTaskDataSource.getTasks(text)))
    }
    @Test fun adminCanCreateUpdateDeleteAllEntities() {
        val member = MockMemberDataSource.addMember("New", "new", "new@test", MemberRole.MEMBER)!!
        val item = MockTaskItemDataSource.addItem("Item", "", text)!!
        val task = MockTaskDataSource.addTask(item.id, member.id, future, text)!!
        assertNotNull(MockMemberDataSource.updateMember(member.id, "Edited", "edited", "edit@test", MemberRole.MEMBER))
        assertNotNull(MockTaskItemDataSource.updateItem(item.id, "Edited item", "", text))
        assertNotNull(MockTaskDataSource.updateTask(task.id, item.id, member.id, future, text))
        assertTrue(MockTaskDataSource.deleteTask(task.id, text))
        assertTrue(MockTaskItemDataSource.deleteItem(item.id, text)); assertTrue(MockMemberDataSource.deleteMember(member.id, text))
    }
    @Test fun selfRoleUpdateImmediatelyRemovesWritePermission() {
        assertNotNull(MockMemberDataSource.updateMember(1, "Alex", "alex", "alex@example.test", MemberRole.MEMBER))
        assertFalse(MockAuthDataSource.canManage())
        assertNull(MockMemberDataSource.addMember("N", "n", "n@test", MemberRole.MEMBER))
        assertNull(MockTaskItemDataSource.addItem("N", "", text))
        assertFalse(MockTaskDataSource.deleteTask(1, text))
    }
    @Test fun unknownDeleteIdsAreSafeNoOps() {
        assertFalse(MockMemberDataSource.deleteMember(99, text)); assertFalse(MockTaskItemDataSource.deleteItem(99, text))
        assertFalse(MockTaskDataSource.deleteTask(99, text))
    }
}
