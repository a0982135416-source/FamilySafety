package com.example.familysafety

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/** STEP 6-2：以真實 UI 建立成員、指派任務並跨頁開始／完成。 */
@RunWith(AndroidJUnit4::class)
class ManagementCoreIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Before fun reset() { MockAuthDataSource.resetForTests(); MockTaskItemDataSource.resetForTests(); MockMemberDataSource.resetForTests(); MockTaskDataSource.resetForTests(); assertNotNull(MockAuthDataSource.login("alex", MockAuthDataSource.INITIAL_PASSWORD)) }
    @Test fun english_addMemberAssignTaskAndComplete() = verify("en")
    @Test fun traditionalChinese_addMemberAssignTaskAndComplete() = verify("zh-TW")

    private fun verify(locale: String) {
        val previous = AppCompatDelegate.getApplicationLocales()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale)) }
                fun awaitReady(check: (MainActivity) -> Boolean) {
                    val deadline = SystemClock.uptimeMillis() + 5_000
                    do {
                        instrumentation.waitForIdleSync()
                        var ready = false
                        scenario.onActivity { ready = check(it) }
                        if (ready) return
                        SystemClock.sleep(50)
                    } while (SystemClock.uptimeMillis() < deadline)
                    fail("UI did not become ready")
                }
                awaitReady { it.resources.configuration.locales[0].language == locale.substringBefore('-') }
                onView(withId(R.id.navManagement)).perform(click())
                awaitReady {
                    // 語系切換可能重建 MainActivity 並暫時回到 Home；就緒後再確認導覽。
                    val nav = it.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation)
                    if (nav.selectedItemId != R.id.navManagement) nav.selectedItemId = R.id.navManagement
                    it.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) is ManagementFragment
                }
                onView(withId(R.id.button_management_member)).perform(scrollTo(), click())
                onView(withId(R.id.recyclerView_management_member)).check(matches(isDisplayed()))
                onView(withId(android.R.id.button1)).check(matches(withText(R.string.management_add_member))).perform(click())
                onView(withId(android.R.id.button1)).perform(click())
                onView(withId(R.id.editText_management_member_name)).check { view, error ->
                    if (error != null) throw error
                    assertNotNull((view as EditText).error)
                }
                fun fill(id: Int, text: String) = onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard())
                val memberName = "Morgan"
                fill(R.id.editText_management_member_name, memberName)
                fill(R.id.editText_management_member_account, "morgan")
                fill(R.id.editText_management_member_email, "morgan@example.test")
                onView(withId(android.R.id.button1)).perform(click())
                onView(withId(R.id.recyclerView_management_member)).check(matches(isDisplayed()))
                assertEquals(4, MockMemberDataSource.getMembers().size)
                assertEquals(MemberRole.MEMBER, MockMemberDataSource.getMembers().last().role)
                onView(withId(R.id.recyclerView_management_member)).perform(scrollLast())
                onView(withText(startsWith(memberName))).check(matches(isDisplayed()))
                onView(withId(android.R.id.button2)).perform(click())
                val taskName = "MVP shared task"
                val taskDescription = "Verify shared task state"
                onView(withId(R.id.button_management_task_item)).perform(scrollTo(), click())
                onView(withId(R.id.recyclerView_management_task_item)).check(matches(isDisplayed()))
                assertEquals(8, MockTaskItemDataSource.getItems { instrumentation.targetContext.getString(it) }.size)
                onView(withId(android.R.id.button1)).check(matches(withText(R.string.management_add_task_item))).perform(click())
                onView(withId(android.R.id.button1)).perform(click())
                onView(withId(R.id.editText_management_task_item_title)).check { view, error ->
                    if (error != null) throw error
                    assertNotNull((view as EditText).error)
                }
                fill(R.id.editText_management_task_item_title, taskName)
                fill(R.id.editText_management_task_item_description, taskDescription)
                onView(withId(android.R.id.button1)).perform(click())
                onView(withId(R.id.recyclerView_management_task_item)).perform(scrollLast())
                onView(withText(startsWith(taskName))).check(matches(isDisplayed()))
                val addedItem = MockTaskItemDataSource.getItems { instrumentation.targetContext.getString(it) }.last()
                assertEquals(9, addedItem.id)
                assertEquals(taskDescription, addedItem.description)
                onView(withId(android.R.id.button2)).perform(click())
                onView(withId(R.id.button_management_task)).perform(scrollTo(), click())
                onView(withId(R.id.recyclerView_management_task)).check(matches(isDisplayed()))
                onView(withId(android.R.id.button1)).check(matches(withText(R.string.management_add_task))).perform(click())
                onView(withId(R.id.editText_management_task_title)).check(doesNotExist())
                onView(withId(R.id.editText_management_task_description)).check(doesNotExist())
                onView(withId(R.id.textView_management_task_status)).check { view, error ->
                    if (error != null) throw error
                    assertTrue(view is TextView && view !is EditText)
                    assertFalse(view.isClickable)
                    assertFalse(view.isFocusable)
                    fun countSpinners(node: View): Int = (if (node is Spinner) 1 else 0) +
                        (if (node is ViewGroup) (0 until node.childCount).sumOf { countSpinners(node.getChildAt(it)) } else 0)
                    assertEquals(2, countSpinners(view.rootView))
                    assertEquals(view.context.getString(R.string.task_detail_status,
                        view.context.getString(R.string.task_status_pending)), (view as TextView).text.toString())
                }
                // 只有工作項目與成員兩個 Spinner；沒有可選的 Status。
                onView(withId(R.id.spinner_management_task_item)).check { view, error ->
                    if (error != null) throw error
                    assertEquals(9, (view as Spinner).count)
                }.perform(scrollTo(), click())
                onData(equalTo(taskName)).inRoot(isPlatformPopup()).perform(click())
                onView(withId(R.id.spinner_management_task_assignee)).perform(scrollTo(), click())
                onData(equalTo(memberName)).inRoot(isPlatformPopup()).perform(click())
                onView(withId(R.id.button_management_task_due)).perform(scrollTo(), click())
                val selected = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 2) }
                onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
                    override fun getConstraints(): Matcher<View> = isAssignableFrom(DatePicker::class.java)
                    override fun getDescription() = "Choose a future date"
                    override fun perform(controller: UiController, view: View) {
                        (view as DatePicker).updateDate(selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH))
                        controller.loopMainThreadUntilIdle()
                    }
                })
                onView(withId(android.R.id.button1)).perform(click())
                onView(withId(android.R.id.button1)).perform(click())
                onView(withId(R.id.recyclerView_management_task)).check(matches(isDisplayed())).perform(scrollLast())
                onView(withText(startsWith(taskName))).check(matches(isDisplayed()))
                var added: Task? = null
                scenario.onActivity { added = MockTaskDataSource.getTasks { key -> it.getString(key) }.last() }
                assertEquals(addedItem.id, added!!.taskItemId)
                assertEquals(taskName, added!!.title)
                assertEquals(taskDescription, added!!.description)
                assertEquals(memberName, added!!.assignee)
                assertEquals(MockMemberDataSource.getMembers().last().id, added!!.assigneeId)
                assertEquals(TaskStatus.PENDING, added!!.status)
                assertFalse(added!!.isOverdue())
                onView(withId(android.R.id.button2)).perform(click())
                onView(withId(R.id.navTask)).perform(click())
                fun awaitRows(count: Int) = awaitReady {
                    it.findViewById<RecyclerView>(R.id.recyclerView_task_list)?.adapter?.itemCount == count
                }
                awaitRows(7)
                fun openNewTask() {
                    onView(withId(R.id.recyclerView_task_list)).perform(scrollLast())
                    onView(allOf(withId(R.id.cardView_task_item), hasDescendant(withText(taskName)))).perform(click())
                    onView(withText(R.string.task_detail_title)).check(matches(isDisplayed()))
                }
                openNewTask()
                onView(withId(android.R.id.button1)).check(matches(withText(R.string.task_action_start))).perform(click())
                awaitReady {
                    (it.findViewById<RecyclerView>(R.id.recyclerView_task_list)?.adapter as? TaskAdapter)
                        ?.currentList?.any { row -> row.task.id == added!!.id && row.task.status == TaskStatus.IN_PROGRESS } == true
                }
                openNewTask()
                onView(withId(android.R.id.button1)).check(matches(withText(R.string.task_action_complete))).perform(click())
                awaitRows(6)
                onView(withId(R.id.navManagement)).perform(click())
                awaitReady { it.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) is ManagementFragment }
                onView(withId(R.id.button_management_task)).perform(scrollTo(), click())
                scenario.onActivity { activity ->
                    assertEquals(TaskStatus.COMPLETED, MockTaskDataSource.getTasks { activity.getString(it) }.last().status)
                }
                onView(withId(R.id.recyclerView_management_task)).perform(scrollLast())
                onView(withText(startsWith(taskName))).check(matches(isDisplayed()))
                // 離開畫面時所有 Dialog 必須消失，原 BottomNavigation 可再使用。
                scenario.onActivity { it.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation).selectedItemId = R.id.navHome }
                awaitReady { it.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) is HomeFragment }
                onView(withId(R.id.recyclerView_management_task)).check(doesNotExist())
                onView(withId(R.id.navManagement)).perform(click())
                onView(withId(R.id.textView_management_title)).check(matches(isDisplayed()))
            }
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
            instrumentation.waitForIdleSync()
        }
    }

    private fun scrollLast() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)
        override fun getDescription() = "Scroll to the last list entry"
        override fun perform(controller: UiController, view: View) {
            val list = view as RecyclerView
            list.scrollToPosition((list.adapter?.itemCount ?: 1) - 1)
            controller.loopMainThreadUntilIdle()
        }
    }
}
