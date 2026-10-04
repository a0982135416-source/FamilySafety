package com.example.familysafety

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import org.hamcrest.Matcher
import java.text.DateFormat
import java.util.Date
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith

/** STEP 5-1：兩種語系驗證 Filter、Badge、Empty State、捲動與共用導覽。 */
@RunWith(AndroidJUnit4::class)
class TaskCenterIntegrationTest {
    // 每個案例獨立初始化 process Mock；不改動既有高度斷言。
    @Before fun resetMockTasks() { MockTaskDataSource.resetForTests() }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun waitForRows(scenario: ActivityScenario<MainActivity>, count: Int) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            instrumentation.waitForIdleSync()
            var ready = false
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.recyclerView_task_list)
                ready = list != null && list.adapter?.itemCount == count
            }
            if (ready) return
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("Task list did not reach $count rows")
    }

    @Test fun english_filtersBadgesEmptyStateAndNavigation() = verifyFlow("en")
    @Test fun traditionalChinese_filtersBadgesEmptyStateAndNavigation() = verifyFlow("zh-TW")

    @Test fun english_detailsTransitionsAndLifecycle() = verifyDetails("en")
    @Test fun traditionalChinese_detailsTransitionsAndLifecycle() = verifyDetails("zh-TW")

    private fun verifyDetails(locale: String) {
        val previous = AppCompatDelegate.getApplicationLocales()
        instrumentation.runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                onView(withId(R.id.navTask)).perform(click())
                waitForRows(scenario, 6)
                fun openFirst(): Task {
                    var task: Task? = null
                    scenario.onActivity { activity ->
                        val list = activity.findViewById<RecyclerView>(R.id.recyclerView_task_list)
                        task = (list.adapter as TaskAdapter).currentList.first().task
                        list.scrollToPosition(0)
                    }
                    instrumentation.waitForIdleSync()
                    onView(withId(R.id.recyclerView_task_list)).perform(object : ViewAction {
                        override fun getConstraints(): Matcher<View> = isDisplayed()
                        override fun getDescription() = "Click the first Task Card"
                        override fun perform(controller: UiController, view: View) {
                            controller.loopMainThreadUntilIdle()
                            val card = (view as RecyclerView).findViewHolderForAdapterPosition(0)!!.itemView
                            click().perform(controller, card)
                            controller.loopMainThreadUntilIdle()
                        }
                    })
                    onView(withText(R.string.task_detail_title)).check(matches(isDisplayed()))
                    var expected = ""
                    scenario.onActivity { activity ->
                        val current = task!!
                        val status = if (current.status == TaskStatus.PENDING)
                            R.string.task_status_pending else R.string.task_status_in_progress
                        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                            activity.resources.configuration.locales[0]).format(Date(current.dueDate))
                        expected = listOfNotNull(current.title, current.description,
                            activity.getString(R.string.task_assignee, current.assignee),
                            activity.getString(R.string.task_due_date, date),
                            activity.getString(R.string.task_detail_status, activity.getString(status)),
                            activity.getString(R.string.task_filter_overdue).takeIf { current.isOverdue() })
                            .joinToString("\n\n")
                    }
                    onView(withId(android.R.id.message)).check(matches(withText(expected)))
                    return task!!
                }
                fun action(label: Int) {
                    onView(withText(label)).perform(click())
                    instrumentation.waitForIdleSync()
                    onView(withText(R.string.task_detail_title)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
                }
                fun select(id: Int, count: Int) {
                    onView(withId(id)).perform(scrollTo(), click())
                    waitForRows(scenario, count)
                }
                // Close 保留原始資料，All 的開始操作只改 badge 不減少數量。
                val pending = openFirst()
                action(R.string.task_detail_close)
                assertEquals(pending, openFirst())
                action(R.string.task_action_start)
                waitForRows(scenario, 6)
                assertEquals(TaskStatus.IN_PROGRESS, openFirst().status)
                action(R.string.task_detail_close)
                select(R.id.button_task_pending, 2)
                openFirst()
                action(R.string.task_action_start)
                waitForRows(scenario, 1)
                openFirst()
                action(R.string.task_action_start)
                waitForRows(scenario, 0)
                onView(withId(R.id.textView_task_empty)).check(matches(isDisplayed()))
                select(R.id.button_task_overdue, 2)
                assertEquals(TaskStatus.IN_PROGRESS, openFirst().status)
                action(R.string.task_detail_close)
                // 注入單筆逾期 Pending，直接驗證 Pending -> In Progress 保留 Overdue。
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager
                        .findFragmentById(R.id.fragmentContainerView_main_content) as TaskFragment
                    fragment.submitTasks(MockTaskDataSource.create({ activity.getString(it) }).filter { it.id == 5L })
                }
                waitForRows(scenario, 1)
                assertEquals(TaskStatus.PENDING, openFirst().status)
                action(R.string.task_action_start)
                waitForRows(scenario, 1)
                assertEquals(TaskStatus.IN_PROGRESS, openFirst().status)
                action(R.string.task_detail_close)
                scenario.onActivity { activity ->
                    val list = activity.findViewById<RecyclerView>(R.id.recyclerView_task_list)
                    val card = list.findViewHolderForAdapterPosition(0)!!.itemView
                    assertEquals(View.VISIBLE, card.findViewById<View>(R.id.textView_task_item_overdue).visibility)
                    assertEquals(activity.getString(R.string.task_status_in_progress),
                        card.findViewById<TextView>(R.id.textView_task_item_status).text.toString())
                }
                openFirst()
                action(R.string.task_action_complete)
                waitForRows(scenario, 0)
                onView(withId(R.id.textView_task_empty)).check(matches(isDisplayed()))
                select(R.id.button_task_all, 0)
                // In Progress 完成後立即清空，Dialog 隨 View 銷毀而關閉。
                scenario.onActivity { activity ->
                    (activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) as TaskFragment)
                        .submitTasks(MockTaskDataSource.create({ activity.getString(it) }).filter { it.id == 3L })
                }
                select(R.id.button_task_in_progress, 1)
                openFirst()
                action(R.string.task_action_complete)
                waitForRows(scenario, 0)
                onView(withId(R.id.textView_task_empty)).check(matches(isDisplayed()))
                scenario.onActivity { activity ->
                    (activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) as TaskFragment)
                        .submitTasks(MockTaskDataSource.create({ activity.getString(it) }))
                }
                waitForRows(scenario, 3)
                openFirst()
                scenario.onActivity { it.findViewById<View>(R.id.navHome).performClick() }
                onView(withText(R.string.task_detail_title)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
                onView(withId(R.id.navTask)).perform(click())
                waitForRows(scenario, 6)
            }
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
        }
    }

    private fun verifyFlow(locale: String) {
        val previous = AppCompatDelegate.getApplicationLocales()
        // 設定語系後才 launch，避免切頁中途 Activity 重建。
        instrumentation.runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                onView(withId(R.id.navTask)).perform(click())
                waitForRows(scenario, 6)
                scenario.onActivity { activity ->
                    assertTrue(activity.findViewById<View>(R.id.button_task_all).isSelected)
                    assertEquals(activity.getString(R.string.task_title),
                        activity.findViewById<TextView>(R.id.textView_task_title).text.toString())
                    val adapter = activity.findViewById<RecyclerView>(R.id.recyclerView_task_list).adapter as TaskAdapter
                    assertFalse(adapter.currentList.any { it.task.status == TaskStatus.COMPLETED })
                }
                fun select(id: Int, count: Int, status: TaskStatus?) {
                    onView(withId(id)).perform(scrollTo(), click())
                    waitForRows(scenario, count)
                    scenario.onActivity { activity ->
                        val adapter = activity.findViewById<RecyclerView>(R.id.recyclerView_task_list).adapter as TaskAdapter
                        if (status != null) assertTrue(adapter.currentList.all { it.task.status == status })
                        assertTrue(activity.findViewById<View>(id).isSelected)
                    }
                }
                select(R.id.button_task_pending, 3, TaskStatus.PENDING)
                select(R.id.button_task_in_progress, 3, TaskStatus.IN_PROGRESS)
                select(R.id.button_task_overdue, 2, null)
                scenario.onActivity { activity ->
                    val list = activity.findViewById<RecyclerView>(R.id.recyclerView_task_list)
                    val adapter = list.adapter as TaskAdapter
                    assertTrue(adapter.currentList.all { it.overdue })
                    assertEquals(setOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS), adapter.currentList.map { it.task.status }.toSet())
                    val card = list.findViewHolderForAdapterPosition(0)!!.itemView
                    assertEquals(View.VISIBLE, card.findViewById<View>(R.id.textView_task_item_overdue).visibility)
                    assertEquals(activity.getString(R.string.task_status_pending),
                        card.findViewById<TextView>(R.id.textView_task_item_status).text.toString())
                }
                select(R.id.button_task_all, 6, null)
                scenario.onActivity {
                    (it.findViewById<RecyclerView>(R.id.recyclerView_task_list).layoutManager as androidx.recyclerview.widget.LinearLayoutManager)
                        .scrollToPositionWithOffset(5, 0)
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val list = activity.findViewById<RecyclerView>(R.id.recyclerView_task_list)
                    val card = list.findViewHolderForAdapterPosition(5)!!.itemView
                    val bounds = Rect()
                    val nav = Rect()
                    assertTrue(card.getGlobalVisibleRect(bounds))
                    activity.findViewById<View>(R.id.bottomNavigationView_main_navigation).getGlobalVisibleRect(nav)
                    assertTrue(bounds.bottom <= nav.top)
                    assertEquals(card.height, bounds.height())
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) as TaskFragment
                    fragment.submitTasks(emptyList())
                }
                onView(withId(R.id.textView_task_empty)).check(matches(isDisplayed()))
                scenario.onActivity { activity ->
                    assertEquals(View.GONE, activity.findViewById<View>(R.id.recyclerView_task_list).visibility)
                    assertEquals(activity.getString(R.string.task_empty), activity.findViewById<TextView>(R.id.textView_task_empty).text.toString())
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) as TaskFragment
                    fragment.submitTasks(MockTaskDataSource.create({ activity.getString(it) }))
                }
                waitForRows(scenario, 6)
                scenario.onActivity { assertEquals(View.GONE, it.findViewById<View>(R.id.textView_task_empty).visibility) }
                onView(withId(R.id.navHome)).perform(click())
                onView(withId(R.id.navTask)).perform(click())
                waitForRows(scenario, 6)
            }
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
        }
    }
}
