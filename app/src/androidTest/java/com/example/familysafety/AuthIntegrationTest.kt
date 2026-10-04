package com.example.familysafety

import android.app.Activity
import android.graphics.Rect
import android.content.Context
import android.view.InputDevice
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.os.SystemClock
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputLayout
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** STEP 7：兩種語系的登入、錯誤驗證、完整重設及返回堆疊測試。 */
@RunWith(AndroidJUnit4::class)
class AuthIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Before fun reset() { MockTaskItemDataSource.resetForTests(); MockTaskDataSource.resetForTests(); MockMemberDataSource.resetForTests(); MockAuthDataSource.resetForTests() }

    @Test fun english_loginSuccess() = withLogin("en") { loginSuccess() }
    @Test fun traditionalChinese_loginSuccess() = withLogin("zh-TW") { loginSuccess() }
    @Test fun english_invalidLogin() = withLogin("en") { invalidLogin() }
    @Test fun traditionalChinese_invalidLogin() = withLogin("zh-TW") { invalidLogin() }
    @Test fun english_completeResetFlow() = withLogin("en") { resetFlow() }
    @Test fun traditionalChinese_completeResetFlow() = withLogin("zh-TW") { resetFlow() }
    @Test fun backAndCancelReturnThroughAuthFlow() = withLogin("en") {
        onView(withId(R.id.textView_login_forgot_password)).perform(scrollTo(), touchClick())
        awaitActivity(ForgotPasswordActivity::class.java)
        onView(withId(R.id.textView_forgot_back_login)).perform(scrollTo(), touchClick())
        awaitActivity(LoginActivity::class.java)
        onView(withId(R.id.textView_login_forgot_password)).perform(scrollTo(), touchClick())
        awaitActivity(ForgotPasswordActivity::class.java)
        fill(R.id.editText_forgot_email, "alex@example.test")
        onView(withId(R.id.button_forgot_send_code)).perform(scrollTo(), touchClick())
        awaitActivity(VerifyCodeActivity::class.java)
        Espresso.pressBack()
        awaitActivity(ForgotPasswordActivity::class.java)
        Espresso.pressBack()
        awaitActivity(LoginActivity::class.java)
    }

    @Test fun english_profileAndLogout() = withLogin("en") { profileAndLogout() }
    @Test fun traditionalChinese_profileAndLogout() = withLogin("zh-TW") { profileAndLogout() }
    @Test fun english_keyboardAndSamePasswordReset() = withLogin("en") { keyboardAndSamePasswordReset() }
    @Test fun traditionalChinese_keyboardAndSamePasswordReset() = withLogin("zh-TW") { keyboardAndSamePasswordReset() }

    @Test fun english_rolePermissionsAcrossLogout() = withLogin("en") { rolePermissionsAcrossLogout() }
    @Test fun traditionalChinese_rolePermissionsAcrossLogout() = withLogin("zh-TW") { rolePermissionsAcrossLogout() }
    @Test fun staleAdminFormsCannotSubmitAsMember() = withLogin("en") {
        credentials(MockAuthDataSource.INITIAL_PASSWORD)
        assertHome()
        val main = openManagement()
        val actions = listOf(R.id.button_management_member, R.id.button_management_task_item, R.id.button_management_task)
        actions.forEach { action ->
            assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
            onView(withId(action)).perform(scrollTo(), touchClick())
            onView(withId(android.R.id.button1)).perform(touchClick())
            when (action) {
                R.id.button_management_member -> {
                    fill(R.id.editText_management_member_name, "Blocked member")
                    fill(R.id.editText_management_member_account, "blocked")
                    fill(R.id.editText_management_member_email, "blocked@example.test")
                }
                R.id.button_management_task_item -> fill(R.id.editText_management_task_item_title, "Blocked item")
            }
            val before = dataSnapshot(main)
            assertNotNull(MockAuthDataSource.login("jamie", MockAuthDataSource.INITIAL_PASSWORD))
            onView(withId(android.R.id.button1)).perform(touchClick())
            assertEquals(before, dataSnapshot(main))
            onView(withId(android.R.id.button2)).perform(touchClick())
        }
        // An old visible ADMIN list button must also re-check the current session.
        MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD)
        onView(withId(R.id.button_management_member)).perform(scrollTo(), touchClick())
        MockAuthDataSource.login("jamie", MockAuthDataSource.INITIAL_PASSWORD)
        val beforeListAction = dataSnapshot(main)
        onView(withId(android.R.id.button1)).perform(touchClick())
        onView(withId(R.id.editText_management_member_name)).check(doesNotExist())
        // AlertDialog automatically dismisses its positive button; the permission contract is no write.
        assertEquals(beforeListAction, dataSnapshot(main))
        assertBlockedEntries(main)
    }

    @Test fun english_managementCrudAndReferences() = withLogin("en") { managementCrudAndReferences() }
    @Test fun traditionalChinese_managementCrudAndReferences() = withLogin("zh-TW") { managementCrudAndReferences() }

    private fun managementCrudAndReferences() {
        credentials(MockAuthDataSource.INITIAL_PASSWORD)
        assertHome()
        val main = awaitActivity(MainActivity::class.java)
        val member = MockMemberDataSource.addMember("CRUD Member", "crud", "crud@example.test", MemberRole.MEMBER)!!
        val item = MockTaskItemDataSource.addItem("CRUD Item", "Original details") { main.getString(it) }!!
        val task = MockTaskDataSource.addTask(item.id, member.id, System.currentTimeMillis() + 86_400_000,
            { main.getString(it) })!!
        MockTaskDataSource.replaceTasks(MockTaskDataSource.updateStatus(MockTaskDataSource.getTasks { main.getString(it) },
            task.id, TaskStatus.IN_PROGRESS))
        openManagement()
        fun open(entry: Int) = onView(withId(entry)).perform(scrollTo(), touchClick())
        fun close() = onView(withId(android.R.id.button2)).perform(touchClick())
        fun confirmDelete() = onView(withId(android.R.id.button1)).check(matches(withText(R.string.management_delete))).perform(touchClick())
        fun blocked(message: Int) {
            onView(withId(android.R.id.message)).check(matches(withText(message)))
            onView(withId(android.R.id.button1)).check(matches(withText(R.string.auth_ok))).perform(touchClick())
        }
        fun choose(spinner: Int, label: String) {
            onView(withId(spinner)).perform(scrollTo(), touchClick())
            androidx.test.espresso.Espresso.onData(org.hamcrest.Matchers.equalTo(label))
                .inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(touchClick())
        }
        open(R.id.button_management_member)
        rowAction(R.id.recyclerView_management_member, "member:${member.id}", R.string.management_edit)
        fill(R.id.editText_management_member_name, "Updated CRUD Member")
        fill(R.id.editText_management_member_account, "crud_updated")
        fill(R.id.editText_management_member_email, "updated@example.test")
        choose(R.id.spinner_management_member_role, main.getString(R.string.management_role_admin))
        onView(withId(android.R.id.button1)).check(matches(withText(R.string.management_save))).perform(touchClick())
        val updatedMember = MockMemberDataSource.getMembers().find { it.id == member.id }!!
        assertEquals("crud_updated", updatedMember.account); assertEquals("updated@example.test", updatedMember.email)
        assertEquals(MemberRole.ADMIN, updatedMember.role)
        assertTrue(MockTaskDataSource.getTasks { main.getString(it) }.find { it.id == task.id }!!.assignee == updatedMember.name)
        rowAction(R.id.recyclerView_management_member, "member:${member.id}", R.string.management_delete)
        confirmDelete(); blocked(R.string.management_member_referenced)
        assertTrue(MockMemberDataSource.getMembers().any { it.id == member.id })
        rowAction(R.id.recyclerView_management_member, "member:1", R.string.management_delete)
        confirmDelete(); blocked(R.string.management_delete_self)
        assertEquals(1, MockAuthDataSource.getCurrentMember()!!.id)
        close()

        open(R.id.button_management_task_item)
        rowAction(R.id.recyclerView_management_task_item, "item:${item.id}", R.string.management_edit)
        fill(R.id.editText_management_task_item_title, " ")
        onView(withId(android.R.id.button1)).perform(touchClick())
        onView(withId(R.id.editText_management_task_item_title)).check { view, failure ->
            if (failure != null) throw failure
            assertNotNull((view as EditText).error)
        }
        fill(R.id.editText_management_task_item_title, "Updated CRUD Item")
        fill(R.id.editText_management_task_item_description, "Updated details")
        onView(withId(android.R.id.button1)).perform(touchClick())
        val synchronized = MockTaskDataSource.getTasks { main.getString(it) }.find { it.id == task.id }!!
        assertEquals("Updated CRUD Item", synchronized.title); assertEquals("Updated details", synchronized.description)
        rowAction(R.id.recyclerView_management_task_item, "item:${item.id}", R.string.management_delete)
        confirmDelete(); blocked(R.string.management_item_referenced)
        assertTrue(MockTaskItemDataSource.getItems { main.getString(it) }.any { it.id == item.id })
        close()

        open(R.id.button_management_task)
        rowAction(R.id.recyclerView_management_task, "task:${task.id}", R.string.management_edit)
        onView(withId(R.id.textView_management_task_status)).check(matches(withText(
            main.getString(R.string.task_detail_status, main.getString(R.string.task_status_in_progress)))))
        val otherItem = MockTaskItemDataSource.getItems { main.getString(it) }.find { it.id == 2 }!!
        choose(R.id.spinner_management_task_item, otherItem.title)
        choose(R.id.spinner_management_task_assignee, "Taylor")
        onView(withId(R.id.button_management_task_due)).perform(scrollTo(), touchClick())
        val chosenDate = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_MONTH, 3)
            set(java.util.Calendar.HOUR_OF_DAY, 23); set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59); set(java.util.Calendar.MILLISECOND, 0)
        }
        onView(isAssignableFrom(android.widget.DatePicker::class.java)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> = isAssignableFrom(android.widget.DatePicker::class.java)
            override fun getDescription() = "Choose a future CRUD deadline"
            override fun perform(controller: androidx.test.espresso.UiController, view: View) {
                (view as android.widget.DatePicker).updateDate(chosenDate.get(java.util.Calendar.YEAR),
                    chosenDate.get(java.util.Calendar.MONTH), chosenDate.get(java.util.Calendar.DAY_OF_MONTH))
                controller.loopMainThreadUntilIdle()
            }
        })
        onView(withId(android.R.id.button1)).perform(touchClick())
        onView(withId(android.R.id.button1)).perform(touchClick())
        val updatedTask = MockTaskDataSource.getTasks { main.getString(it) }.find { it.id == task.id }!!
        assertEquals(otherItem.id, updatedTask.taskItemId); assertEquals(3, updatedTask.assigneeId)
        assertEquals(otherItem.title, updatedTask.title); assertEquals(otherItem.description, updatedTask.description)
        assertEquals("Taylor", updatedTask.assignee); assertEquals(chosenDate.timeInMillis, updatedTask.dueDate)
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.status)
        close()
        fun awaitTaskCenter(present: Boolean) {
            onView(withId(R.id.navTask)).perform(touchClick())
            awaitCondition {
                val adapter = main.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView_task_list)?.adapter as? TaskAdapter
                adapter != null && (if (present) adapter.currentList.any { it.task == updatedTask }
                    else adapter.currentList.none { it.task.id == task.id })
            }
        }
        awaitTaskCenter(true)
        openManagement(); open(R.id.button_management_task)
        rowAction(R.id.recyclerView_management_task, "task:${task.id}", R.string.management_delete)
        close() // Cancel confirmation must preserve the record.
        assertTrue(MockTaskDataSource.getTasks { main.getString(it) }.any { it.id == task.id })
        open(R.id.button_management_task)
        rowAction(R.id.recyclerView_management_task, "task:${task.id}", R.string.management_delete)
        confirmDelete()
        assertFalse(MockTaskDataSource.getTasks { main.getString(it) }.any { it.id == task.id })
        close(); awaitTaskCenter(false)
        openManagement(); open(R.id.button_management_task_item)
        rowAction(R.id.recyclerView_management_task_item, "item:${item.id}", R.string.management_delete)
        confirmDelete()
        assertFalse(MockTaskItemDataSource.getItems { main.getString(it) }.any { it.id == item.id })
        close(); open(R.id.button_management_member)
        rowAction(R.id.recyclerView_management_member, "member:${member.id}", R.string.management_delete)
        confirmDelete()
        assertFalse(MockMemberDataSource.getMembers().any { it.id == member.id })
        close()
    }

    private fun rowAction(listId: Int, key: String, action: Int) {
        val id = key.substringAfter(':').toLong()
        onView(withId(listId)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> = isAssignableFrom(androidx.recyclerview.widget.RecyclerView::class.java)
            override fun getDescription() = "Scroll to stable record ID $key"
            override fun perform(controller: androidx.test.espresso.UiController, view: View) {
                val list = view as androidx.recyclerview.widget.RecyclerView
                val adapter = list.adapter!!
                val index = (0 until adapter.itemCount).first { adapter.getItemId(it) == id }
                list.scrollToPosition(index)
                controller.loopMainThreadUntilIdle()
            }
        })
        onView(org.hamcrest.Matchers.allOf(withText(action), isDescendantOfA(withTagValue(org.hamcrest.Matchers.equalTo(key))), isDisplayed()))
            .perform(touchClick())
    }

    private fun dataSnapshot(main: MainActivity): List<Any> = listOf(
        MockMemberDataSource.getMembers(), MockTaskItemDataSource.getItems { main.getString(it) },
        MockTaskDataSource.getTasks { main.getString(it) }
    )

    private fun assertBlockedEntries(main: MainActivity) {
        val before = dataSnapshot(main)
        instrumentation.runOnMainSync {
            val fragment = main.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content)!!
            // Bypass visibility deliberately to exercise the real private form entry guards.
            listOf("addMember", "addTaskItem", "addTask").forEach {
                fragment.javaClass.getDeclaredMethod(it).apply { isAccessible = true }.invoke(fragment)
            }
            listOf("editMember", "deleteMember", "editTaskItem", "deleteTaskItem").forEach {
                fragment.javaClass.getDeclaredMethod(it, Int::class.javaPrimitiveType).apply { isAccessible = true }.invoke(fragment, 1)
            }
            listOf("editTask", "deleteTask").forEach {
                fragment.javaClass.getDeclaredMethod(it, Long::class.javaPrimitiveType).apply { isAccessible = true }.invoke(fragment, 1L)
            }
        }
        listOf(R.id.editText_management_member_name, R.id.editText_management_task_item_title,
            R.id.spinner_management_task_item).forEach { onView(withId(it)).check(doesNotExist()) }
        assertEquals(before, dataSnapshot(main))
    }

    private fun openManagement(): MainActivity {
        val main = awaitActivity(MainActivity::class.java)
        onView(withId(R.id.navManagement)).perform(touchClick())
        awaitCondition {
            main.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) is ManagementFragment
        }
        return main
    }

    private fun checkManagementLists(admin: Boolean) {
        val lists = listOf(
            Triple(R.id.button_management_member, R.id.recyclerView_management_member, R.string.management_add_member),
            Triple(R.id.button_management_task_item, R.id.recyclerView_management_task_item, R.string.management_add_task_item),
            Triple(R.id.button_management_task, R.id.recyclerView_management_task, R.string.management_add_task)
        )
        lists.forEach { (entry, list, add) ->
            onView(withId(entry)).perform(scrollTo(), touchClick())
            onView(withId(list)).check(matches(isDisplayed())).check { view, failure ->
                if (failure != null) throw failure
                val expected = when (entry) {
                    R.id.button_management_member -> MockMemberDataSource.getMembers().size
                    R.id.button_management_task_item -> MockTaskItemDataSource.getItems { view.context.getString(it) }.size
                    else -> MockTaskDataSource.getTasks { view.context.getString(it) }.size
                }
                assertEquals(expected, (view as androidx.recyclerview.widget.RecyclerView).adapter!!.itemCount)
            }
            if (admin) onView(withId(android.R.id.button1)).check(matches(isDisplayed())).check(matches(withText(add)))
            else {
                onView(withId(android.R.id.button1)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                onView(org.hamcrest.Matchers.allOf(withText(R.string.management_edit), isDisplayed())).check(doesNotExist())
                onView(org.hamcrest.Matchers.allOf(withText(R.string.management_delete), isDisplayed())).check(doesNotExist())
            }
            onView(withId(android.R.id.button2)).perform(touchClick())
        }
    }

    private fun logoutFromManagement() {
        onView(withId(R.id.navHome)).perform(touchClick())
        awaitCondition {
            awaitMainFragment() is HomeFragment
        }
        onView(withId(R.id.frameLayout_home_profile)).perform(scrollTo(), touchClick())
        onView(withId(android.R.id.button1)).perform(touchClick())
        awaitActivity(LoginActivity::class.java)
        assertNull(MockAuthDataSource.getCurrentMember())
        assertFalse(MockAuthDataSource.canManage())
    }

    private fun awaitMainFragment() = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().firstOrNull()
        ?.supportFragmentManager?.findFragmentById(R.id.fragmentContainerView_main_content)

    private fun rolePermissionsAcrossLogout() {
        listOf("alex" to true, "jamie" to false, "alex" to true, "taylor" to false).forEach { (account, admin) ->
            credentials(MockAuthDataSource.INITIAL_PASSWORD, account)
            assertHome()
            assertEquals(admin, MockAuthDataSource.canManage())
            val main = openManagement()
            checkManagementLists(admin)
            if (!admin) assertBlockedEntries(main)
            logoutFromManagement()
        }
    }

    private fun profileAndLogout() {
        credentials(MockAuthDataSource.INITIAL_PASSWORD, "jamie")
        assertHome()
        val main = awaitActivity(MainActivity::class.java)
        val member = MockAuthDataSource.getCurrentMember()!!
        assertEquals("jamie", member.account)
        onView(withId(R.id.imageButton_home_menu)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        fun openProfile() = onView(withId(R.id.frameLayout_home_profile)).perform(scrollTo(), touchClick())
        openProfile()
        val role = main.getString(R.string.management_role_member)
        fun line(key: Int, value: String) = main.getString(R.string.management_label_value, main.getString(key), value)
        val expected = listOf(line(R.string.management_name, member.name), line(R.string.management_account, member.account),
            line(R.string.management_email, member.email), line(R.string.management_role, role)).joinToString("\n")
        onView(withId(android.R.id.message)).check(matches(withText(expected)))
        onView(withId(android.R.id.button2)).check(matches(withText(R.string.task_detail_close))).perform(touchClick())
        assertEquals(member, MockAuthDataSource.getCurrentMember())
        openProfile()
        onView(withId(android.R.id.button1)).check(matches(withText(R.string.auth_logout))).perform(touchClick())
        awaitActivity(LoginActivity::class.java)
        assertNull(MockAuthDataSource.getCurrentMember())
        assertTrue(main.isFinishing || main.isDestroyed)
        Espresso.pressBackUnconditionally()
        awaitNoResumedActivity()
        assertNull(MockAuthDataSource.getCurrentMember())
    }

    private fun keyboardAndSamePasswordReset() {
        checkKeyboard(R.id.editText_login_password, R.id.button_login_sign_in)
        onView(withId(R.id.button_login_sign_in)).perform(scrollTo(), touchClick())
        assertPasswordIcons(R.id.textInputLayout_login_password)
        togglePassword(R.id.textInputLayout_login_password)
        onView(withId(R.id.button_login_sign_in)).perform(scrollTo(), touchClick())
        assertPasswordIcons(R.id.textInputLayout_login_password)
        togglePassword(R.id.textInputLayout_login_password)
        onView(withId(R.id.textView_login_forgot_password)).perform(scrollTo(), touchClick())
        awaitActivity(ForgotPasswordActivity::class.java)
        checkKeyboard(R.id.editText_forgot_email, R.id.button_forgot_send_code)
        fill(R.id.editText_forgot_email, "alex@example.test")
        onView(withId(R.id.button_forgot_send_code)).perform(scrollTo(), touchClick())
        awaitActivity(VerifyCodeActivity::class.java)
        checkKeyboard(R.id.editText_verify_otp6, R.id.button_verify_submit)
        code(MockAuthDataSource.RESET_CODE)
        awaitActivity(ResetPasswordActivity::class.java)
        checkKeyboard(R.id.editText_reset_new_password, R.id.button_reset_submit)
        checkKeyboard(R.id.editText_reset_confirm_password, R.id.button_reset_submit)
        onView(withId(R.id.button_reset_submit)).perform(scrollTo(), touchClick())
        assertPasswordIcons(R.id.textInputLayout_reset_new_password)
        assertPasswordIcons(R.id.textInputLayout_reset_confirm_password)
        fill(R.id.editText_reset_new_password, MockAuthDataSource.INITIAL_PASSWORD)
        fill(R.id.editText_reset_confirm_password, MockAuthDataSource.INITIAL_PASSWORD)
        onView(withId(R.id.button_reset_submit)).perform(scrollTo(), touchClick())
        awaitActivity(LoginActivity::class.java)
        onView(withText(R.string.auth_reset_success)).check(matches(isDisplayed()))
        assertFalse(MockAuthDataSource.isResetVerified("alex@example.test"))
        credentials(MockAuthDataSource.INITIAL_PASSWORD)
        assertHome()
        assertNotNull(MockAuthDataSource.getCurrentMember())
    }

    private fun togglePassword(layoutId: Int) = onView(org.hamcrest.Matchers.allOf(
        withId(com.google.android.material.R.id.text_input_end_icon), isDescendantOfA(withId(layoutId))
    )).perform(touchClick())

    private fun assertPasswordIcons(id: Int) = onView(withId(id)).check { view, failure ->
        if (failure != null) throw failure
        val layout = view as TextInputLayout
        assertNotNull(layout.error)
        assertNull(layout.errorIconDrawable)
        assertTrue(layout.isEndIconVisible)
        assertNotNull(layout.endIconDrawable)
        assertNull(layout.editText!!.error)
    }

    /** 實際顯示 IME，驗證 viewport 縮小、可捲動及輸入欄位位於鍵盤上方；不比對固定像素。 */
    private fun checkKeyboard(fieldId: Int, buttonId: Int) {
        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
        }
        val current = activity!!
        var field: EditText? = null
        var scroll: NestedScrollView? = null
        var initialHeight = 0
        instrumentation.runOnMainSync {
            field = current.findViewById(fieldId)
            scroll = generateSequence(field!!.parent as? View) { it.parent as? View }.filterIsInstance<NestedScrollView>().first()
        }
        // 新 Activity 可能自動聚焦並開啟 IME；先取得完成 layout 的無鍵盤基準。
        Espresso.closeSoftKeyboard()
        awaitCondition {
            ViewCompat.getRootWindowInsets(current.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false &&
                scroll!!.height > 0 && !scroll!!.isLayoutRequested && !current.window.decorView.isLayoutRequested
        }
        instrumentation.runOnMainSync { initialHeight = scroll!!.height }
        onView(withId(fieldId)).perform(scrollTo(), touchClick())
        instrumentation.runOnMainSync {
            field!!.requestFocus()
            (current.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
        }
        awaitCondition {
            val insets = ViewCompat.getRootWindowInsets(current.window.decorView)
            insets?.isVisible(WindowInsetsCompat.Type.ime()) == true && scroll!!.height < initialHeight
        }
        onView(withId(fieldId)).perform(scrollTo()).check(matches(isCompletelyDisplayed()))
        instrumentation.runOnMainSync {
            val insets = ViewCompat.getRootWindowInsets(current.window.decorView)!!
            val origin = IntArray(2)
            current.window.decorView.getLocationOnScreen(origin)
            val keyboardTop = origin[1] + current.window.decorView.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val visible = Rect()
            assertTrue(field!!.getGlobalVisibleRect(visible))
            assertTrue(field!!.hasFocus())
            assertTrue(visible.bottom <= keyboardTop)
            assertTrue(scroll!!.canScrollVertically(1) || scroll!!.canScrollVertically(-1))
        }
        onView(withId(buttonId)).perform(scrollTo()).check(matches(isCompletelyDisplayed()))
        Espresso.closeSoftKeyboard()
        awaitCondition { ViewCompat.getRootWindowInsets(current.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false }
    }

    private fun awaitCondition(check: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            instrumentation.waitForIdleSync()
            var ready = false
            instrumentation.runOnMainSync { ready = check() }
            if (ready) return
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("UI state did not become ready")
    }

    private fun awaitNoResumedActivity() = awaitCondition {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).isEmpty()
    }

    private fun withLogin(locale: String, flow: () -> Unit) {
        val previous = AppCompatDelegate.getApplicationLocales()
        val resolver = instrumentation.targetContext.contentResolver
        val handwritingKey = "stylus_handwriting_enabled"
        val previousHandwriting = android.provider.Settings.Secure.getString(resolver, handwritingKey)
        // Test a docked software keyboard, not the emulator's floating handwriting toolbar.
        // Always restore the device setting, including on a test failure.
        fun setHandwriting(value: String?) {
            instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS)
            try { android.provider.Settings.Secure.putString(resolver, handwritingKey, value) }
            finally { instrumentation.uiAutomation.dropShellPermissionIdentity() }
        }
        try {
            setHandwriting("0")
            val context = instrumentation.targetContext
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            assertEquals(LoginActivity::class.java.name, launch.component!!.className)
            // 真實 launcher 啟動；Login 正常 finish 後，使用 lifecycle monitor 清理後續 Activity。
            instrumentation.startActivitySync(launch)
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale)) }
            awaitActivity(LoginActivity::class.java) { it.resources.configuration.locales[0].language == locale.substringBefore('-') }
            onView(withId(R.id.textView_login_title)).check(matches(withText(R.string.app_name)))
            onView(withId(R.id.checkBox_login_remember_me)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            flow()
        } finally {
            instrumentation.runOnMainSync {
                val monitor = ActivityLifecycleMonitorRegistry.getInstance()
                listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED)
                    .flatMap { monitor.getActivitiesInStage(it).toList() }.distinct().forEach { it.finish() }
            }
            instrumentation.waitForIdleSync()
            setHandwriting(previousHandwriting)
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
            instrumentation.waitForIdleSync()
        }
    }

    private fun <T : Activity> awaitActivity(type: Class<T>, ready: (T) -> Boolean = { true }): T {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            instrumentation.waitForIdleSync()
            var found: T? = null
            instrumentation.runOnMainSync {
                found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filter { type.isInstance(it) }.mapNotNull { type.cast(it) }.firstOrNull(ready)
            }
            found?.let { return it }
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("Activity did not become ready: ${type.simpleName}")
    }

    // SOURCE_UNKNOWN taps can trigger Gboard handwriting on API 35; model a real finger tap.
    private fun touchClick() = click(InputDevice.SOURCE_TOUCHSCREEN, 0)

    private fun fill(id: Int, value: String) = onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
    private fun error(id: Int, resource: Int) = onView(withId(id)).check { view, failure ->
        if (failure != null) throw failure
        val field = view as EditText
        val layout = generateSequence(field.parent as? View) { it.parent as? View }.filterIsInstance<TextInputLayout>().firstOrNull()
        assertEquals(view.context.getString(resource), (layout?.error ?: field.error)?.toString())
    }
    private fun credentials(password: String, account: String = "alex") {
        fill(R.id.editText_login_account, account)
        fill(R.id.editText_login_password, password)
        onView(withId(R.id.button_login_sign_in)).perform(scrollTo(), touchClick())
    }
    private fun assertHome() {
        val main = awaitActivity(MainActivity::class.java)
        instrumentation.runOnMainSync {
            main.supportFragmentManager.executePendingTransactions()
            assertTrue(main.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) is HomeFragment)
            assertEquals(R.id.navHome, main.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation).selectedItemId)
        }
        onView(withId(R.id.navHome)).check(matches(isDisplayed()))
    }
    private fun loginSuccess() {
        val login = awaitActivity(LoginActivity::class.java)
        credentials(MockAuthDataSource.INITIAL_PASSWORD)
        assertHome()
        assertTrue(login.isFinishing || login.isDestroyed)
        assertNotNull(MockAuthDataSource.getCurrentMember())
        Espresso.pressBackUnconditionally()
        awaitNoResumedActivity()
    }
    private fun invalidLogin() {
        onView(withId(R.id.button_login_sign_in)).perform(scrollTo(), touchClick())
        error(R.id.editText_login_account, R.string.management_required)
        error(R.id.editText_login_password, R.string.management_required)
        credentials("Wrong123")
        awaitActivity(LoginActivity::class.java)
        error(R.id.editText_login_password, R.string.auth_login_failed)
        credentials(MockAuthDataSource.INITIAL_PASSWORD, "unknown")
        awaitActivity(LoginActivity::class.java)
        error(R.id.editText_login_password, R.string.auth_login_failed)
    }
    private fun code(value: String) {
        val ids = listOf(R.id.editText_verify_otp1, R.id.editText_verify_otp2, R.id.editText_verify_otp3,
            R.id.editText_verify_otp4, R.id.editText_verify_otp5, R.id.editText_verify_otp6)
        ids.forEachIndexed { index, id -> onView(withId(id)).perform(scrollTo(), replaceText(value.getOrNull(index)?.toString().orEmpty())) }
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.button_verify_submit)).perform(scrollTo(), touchClick())
    }
    private fun resetFlow() {
        val originalLogin = awaitActivity(LoginActivity::class.java)
        onView(withId(R.id.textView_login_forgot_password)).perform(scrollTo(), touchClick())
        val forgot = awaitActivity(ForgotPasswordActivity::class.java)
        onView(withId(R.id.textView_forgot_header_title)).check(matches(withText(R.string.auth_forgot_title)))
        onView(withId(R.id.button_forgot_send_code)).perform(scrollTo(), touchClick())
        error(R.id.editText_forgot_email, R.string.management_required)
        fill(R.id.editText_forgot_email, "invalid")
        onView(withId(R.id.button_forgot_send_code)).perform(scrollTo(), touchClick())
        error(R.id.editText_forgot_email, R.string.auth_email_invalid)
        fill(R.id.editText_forgot_email, "unknown@example.test")
        onView(withId(R.id.button_forgot_send_code)).perform(scrollTo(), touchClick())
        error(R.id.editText_forgot_email, R.string.auth_email_unknown)
        fill(R.id.editText_forgot_email, "alex@example.test")
        onView(withId(R.id.button_forgot_send_code)).perform(scrollTo(), touchClick())
        val verify = awaitActivity(VerifyCodeActivity::class.java)
        assertEquals(setOf(AuthNavigation.EXTRA_EMAIL), verify.intent.extras!!.keySet())
        onView(withId(R.id.textView_verify_header_title)).check(matches(withText(R.string.auth_verify_title)))
        onView(withId(R.id.textView_verify_masked_email)).check(matches(withText("alex@example.test")))
        code("12345")
        error(R.id.editText_verify_otp1, R.string.auth_code_invalid)
        code("000000")
        awaitActivity(VerifyCodeActivity::class.java)
        onView(withText(R.string.auth_code_error_title)).check(matches(isDisplayed()))
        onView(withId(android.R.id.message)).check(matches(withText(R.string.auth_code_error_message)))
        onView(withId(android.R.id.button1)).check(matches(withText(R.string.auth_ok))).perform(touchClick())
        assertSame(verify, awaitActivity(VerifyCodeActivity::class.java))
        listOf(R.id.editText_verify_otp1, R.id.editText_verify_otp2, R.id.editText_verify_otp3,
            R.id.editText_verify_otp4, R.id.editText_verify_otp5, R.id.editText_verify_otp6).forEach { id ->
            onView(withId(id)).check { view, failure ->
                if (failure != null) throw failure
                assertEquals("", (view as EditText).text.toString())
                assertNull(view.error)
            }
        }
        onView(withId(R.id.editText_verify_otp1)).check(matches(hasFocus()))
        code(MockAuthDataSource.RESET_CODE)
        val reset = awaitActivity(ResetPasswordActivity::class.java)
        assertEquals(setOf(AuthNavigation.EXTRA_EMAIL), reset.intent.extras!!.keySet())
        onView(withId(R.id.textView_reset_header_title)).check(matches(withText(R.string.auth_reset_title)))
        onView(withId(R.id.button_reset_submit)).perform(scrollTo(), touchClick())
        error(R.id.editText_reset_new_password, R.string.management_required)
        fill(R.id.editText_reset_new_password, "New12345")
        fill(R.id.editText_reset_confirm_password, "Mismatch1")
        onView(withId(R.id.button_reset_submit)).perform(scrollTo(), touchClick())
        awaitActivity(ResetPasswordActivity::class.java)
        error(R.id.editText_reset_confirm_password, R.string.auth_password_mismatch)
        assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD))
        fill(R.id.editText_reset_confirm_password, "New12345")
        onView(withId(R.id.button_reset_submit)).perform(scrollTo(), touchClick())
        assertSame(originalLogin, awaitActivity(LoginActivity::class.java))
        onView(withText(R.string.auth_reset_success)).check(matches(isDisplayed()))
        assertTrue(forgot.isFinishing || forgot.isDestroyed)
        assertTrue(verify.isFinishing || verify.isDestroyed)
        assertTrue(reset.isFinishing || reset.isDestroyed)
        assertFalse(MockAuthDataSource.isResetVerified("alex@example.test"))
        Espresso.pressBackUnconditionally()
        awaitNoResumedActivity()
        val context = instrumentation.targetContext
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!)
        awaitActivity(LoginActivity::class.java)
        credentials(MockAuthDataSource.INITIAL_PASSWORD)
        awaitActivity(LoginActivity::class.java)
        error(R.id.editText_login_password, R.string.auth_login_failed)
        credentials("New12345")
        assertHome()
    }
}
