package com.example.familysafety

import androidx.test.espresso.matcher.ViewMatchers.withText

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.MotionEvent
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/** STEP 4-5：沿用現有 AndroidJUnit4 / Espresso，在裝置驗證真實 Fragment。 */
@RunWith(AndroidJUnit4::class)
class AlertCenterIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun withLocale(tag: String, block: (ActivityScenario<MainActivity>) -> Unit) {
        instrumentation.runOnMainSync {
            MockTaskDataSource.resetForTests()
            MockEnvironmentDataSource.resetForTests()
            MockEnvironmentAlertDataSource.resetForTests()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var previous = LocaleListCompat.getEmptyLocaleList()
            try {
                // 先建立 Activity，確保 AppCompat 能取得 Android 13+ 的 LocaleManager。
                scenario.onActivity {
                    previous = AppCompatDelegate.getApplicationLocales()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                }
                val deadline = SystemClock.uptimeMillis() + 5_000
                var language = ""
                do {
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { language = it.resources.configuration.locales[0].language }
                    if (language == Locale.forLanguageTag(tag).language) break
                    SystemClock.sleep(50)
                } while (SystemClock.uptimeMillis() < deadline)
                assertEquals(Locale.forLanguageTag(tag).language, language)
                navigateAlert(scenario)
                waitForRows(scenario, 3)
                block(scenario)
            } finally {
                scenario.onActivity {
                    AppCompatDelegate.setApplicationLocales(previous)
                    MockTaskDataSource.resetForTests()
                    MockEnvironmentDataSource.resetForTests()
                    MockEnvironmentAlertDataSource.resetForTests()
                }
            }
        }
    }

    // ListAdapter 的 DiffUtil 在背景執行；有期限地等待實際列表提交，不猜測固定延遲。
    private fun waitForRows(scenario: ActivityScenario<MainActivity>, expected: Int, taskIds: Set<Long>? = null) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        do {
            instrumentation.waitForIdleSync()
            var count = -1
            var idsMatch = taskIds == null
            scenario.onActivity {
                val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter
                count = adapter?.itemCount ?: -1
                if (taskIds != null && adapter is TaskAlertAdapter) {
                    idsMatch = adapter.currentList.map { row -> row.alert.id }.toSet() == taskIds
                }
            }
            if (count == expected && idsMatch) return
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("ListAdapter did not reach $expected rows")
    }

    private fun navigateAlert(scenario: ActivityScenario<MainActivity>) {
        val deadline = SystemClock.uptimeMillis() + 5_000L
        do {
            onView(withId(R.id.navAlert)).perform(click())
            instrumentation.waitForIdleSync()
            var ready = false
            scenario.onActivity { ready = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list) != null }
            if (ready) return
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("Alert destination did not become ready")
    }

    private fun counts(scenario: ActivityScenario<MainActivity>, task: Boolean, all: Int, pending: Int, third: Int, overdue: Int = 0) {
        scenario.onActivity { activity ->
            fun check(id: Int, label: Int, count: Int) {
                assertEquals(activity.getString(label, count), activity.findViewById<TextView>(id).text.toString())
            }
            check(R.id.button_alert_all, R.string.alert_filter_all_count, all)
            check(R.id.button_alert_pending, R.string.alert_filter_pending_count, pending)
            if (task) {
                check(R.id.button_alert_in_progress, R.string.alert_filter_in_progress_count, third)
                check(R.id.button_alert_overdue, R.string.alert_filter_overdue_count, overdue)
            } else check(R.id.button_alert_resolved, R.string.alert_filter_resolved_count, third)
        }
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "step45").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun english_categoryFiltersAutoResolveAndEmptyState() = verifyFlow("en")

    @Test fun traditionalChinese_categoryFiltersAutoResolveAndEmptyState() = verifyFlow("zh-TW")

    @Test fun english_selectedFilterScrollsFullyIntoView() = verifyFilterAutoScroll("en")

    @Test fun traditionalChinese_selectedFilterScrollsFullyIntoView() = verifyFilterAutoScroll("zh-TW")

    // 不使用 Espresso scrollTo()：直接觸發點選，確保捲動來自正式程式。
    private fun verifyFilterAutoScroll(tag: String) = withLocale(tag) { scenario ->
        onView(withId(R.id.button_alert_task)).perform(click())
        waitForRows(scenario, 6)
        fun selectAndCheck(id: Int) {
            val visible = android.graphics.Rect()
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<View>(id).getGlobalVisibleRect(visible))
                val scroll = activity.findViewById<HorizontalScrollView>(R.id.horizontalScrollView_alert_filters)
                val position = IntArray(2)
                scroll.getLocationOnScreen(position)
                assertTrue(visible.intersect(position[0] + scroll.paddingLeft, position[1],
                    position[0] + scroll.width - scroll.paddingRight, position[1] + scroll.height - scroll.paddingBottom))
            }
            // 點擊目前可見的部分，不預先捲動；重現使用者點擊被裁切按鈕的操作。
            val downTime = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, visible.exactCenterX(), visible.exactCenterY(), 0)
            val up = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, visible.exactCenterX(), visible.exactCenterY(), 0)
            instrumentation.sendPointerSync(down)
            instrumentation.sendPointerSync(up)
            down.recycle()
            up.recycle()
            val deadline = SystemClock.uptimeMillis() + 5_000
            var complete = false
            do {
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val scroll = activity.findViewById<HorizontalScrollView>(R.id.horizontalScrollView_alert_filters)
                    val button = activity.findViewById<TextView>(id)
                    val buttonPosition = IntArray(2)
                    val scrollPosition = IntArray(2)
                    button.getLocationOnScreen(buttonPosition)
                    scroll.getLocationOnScreen(scrollPosition)
                    complete = buttonPosition[0] >= scrollPosition[0] + scroll.paddingLeft &&
                        buttonPosition[0] + button.width <= scrollPosition[0] + scroll.width - scroll.paddingRight
                    if (complete) {
                        assertEquals(1, button.lineCount)
                        assertEquals(0, button.layout.getEllipsisCount(0))
                        assertTrue(button.layout.getLineWidth(0) <= button.width - button.compoundPaddingLeft - button.compoundPaddingRight)
                    }
                }
                if (complete) break
                SystemClock.sleep(50)
            } while (SystemClock.uptimeMillis() < deadline)
            assertTrue("Selected filter must include both rounded edges inside the padded viewport", complete)
        }
        selectAndCheck(R.id.button_alert_all)
        selectAndCheck(R.id.button_alert_pending)
        selectAndCheck(R.id.button_alert_in_progress)
        // 重現原問題：回到最左側後直接選取 Overdue，不讓測試先替它捲動。
        scenario.onActivity { activity ->
            val scroll = activity.findViewById<HorizontalScrollView>(R.id.horizontalScrollView_alert_filters)
            scroll.scrollTo(0, 0)
            if (scroll.getChildAt(0).width > scroll.width - scroll.paddingLeft - scroll.paddingRight) {
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.textView_alert_filter_hint).visibility)
            }
        }
        selectAndCheck(R.id.button_alert_overdue)
        waitForRows(scenario, 2, setOf(5L, 6L))
        screenshot("${tag}_overdue_auto_scroll")
        selectAndCheck(R.id.button_alert_all)
        waitForRows(scenario, 6)
        scenario.onActivity { assertEquals(0, it.findViewById<HorizontalScrollView>(R.id.horizontalScrollView_alert_filters).scrollX) }
        onView(withId(R.id.button_alert_environment)).perform(click())
        waitForRows(scenario, 3)
        selectAndCheck(R.id.button_alert_all)
        selectAndCheck(R.id.button_alert_pending)
        selectAndCheck(R.id.button_alert_resolved)
        counts(scenario, false, 3, 0, 3)
        onView(withId(R.id.button_alert_task)).perform(click())
        waitForRows(scenario, 6)
        selectAndCheck(R.id.button_alert_overdue)
    }

    private fun verifyFlow(tag: String) = withLocale(tag) { scenario ->
        var originalRecycler: RecyclerView? = null
        scenario.onActivity { activity ->
            assertEquals(Locale.forLanguageTag(tag).language, activity.resources.configuration.locales[0].language)
            originalRecycler = activity.findViewById(R.id.recyclerView_alert_list)
            assertTrue(originalRecycler!!.adapter is EnvironmentAlertAdapter)
            val header = activity.findViewById<View>(R.id.constraintLayout_alert_header)
            val title = activity.findViewById<TextView>(R.id.textView_alert_title)
            assertTrue(header.height >= header.paddingTop + (60 * activity.resources.displayMetrics.density).toInt())
            assertEquals(activity.getString(R.string.alert_header_title), title.text.toString())
        }
        counts(scenario, false, 3, 0, 3)
        screenshot("${tag}_environment")

        // Environmental → Task → Environmental → Task；同一 RecyclerView、不累加資料。
        repeat(2) {
            onView(withId(R.id.button_alert_task)).perform(click())
            waitForRows(scenario, 6)
            counts(scenario, true, 6, 3, 3, 2)
            scenario.onActivity { activity ->
                val recycler = activity.findViewById<RecyclerView>(R.id.recyclerView_alert_list)
                assertSame(originalRecycler, recycler)
                assertTrue(recycler.adapter is TaskAlertAdapter)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.button_alert_resolved).visibility)
                assertEquals(0, activity.findViewById<HorizontalScrollView>(R.id.horizontalScrollView_alert_filters).scrollX)
            }
            if (it == 0) {
                onView(withId(R.id.button_alert_environment)).perform(click())
                waitForRows(scenario, 3)
            }
        }
        screenshot("${tag}_task_all")
        scenario.onActivity {
            (it.findViewById<RecyclerView>(R.id.recyclerView_alert_list).layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(5, 0)
        }
        instrumentation.waitForIdleSync()
        screenshot("${tag}_task_bottom")
        scenario.onActivity { activity ->
            val recycler = activity.findViewById<RecyclerView>(R.id.recyclerView_alert_list)
            val lastCard = recycler.findViewHolderForAdapterPosition(5)!!.itemView
            val cardBounds = android.graphics.Rect()
            val navBounds = android.graphics.Rect()
            assertTrue(lastCard.getGlobalVisibleRect(cardBounds))
            activity.findViewById<View>(R.id.bottomNavigationView_main_navigation).getGlobalVisibleRect(navBounds)
            assertTrue(cardBounds.bottom <= navBounds.top)
            assertEquals(lastCard.height, cardBounds.height())
        }
        onView(withId(R.id.button_alert_pending)).perform(scrollTo(), click())
        waitForRows(scenario, 3, setOf(1L, 2L, 5L))
        scenario.onActivity {
            val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list).adapter as TaskAlertAdapter
            assertTrue(adapter.currentList.all { row -> row.alert.status == TaskStatus.PENDING })
        }
        onView(withId(R.id.button_alert_in_progress)).perform(scrollTo(), click())
        waitForRows(scenario, 3, setOf(3L, 4L, 6L))
        scenario.onActivity { it.findViewById<View>(R.id.button_alert_overdue).performClick() }
        waitForRows(scenario, 2, setOf(5L, 6L))
        scenario.onActivity {
            val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list).adapter as TaskAlertAdapter
            assertTrue(adapter.currentList.all { row -> row.overdue })
        }
        onView(withId(R.id.button_alert_overdue)).check(matches(isDisplayed()))
        screenshot("${tag}_task_overdue")

        onView(withId(R.id.button_alert_environment)).perform(click())
        waitForRows(scenario, 3)
        counts(scenario, false, 3, 0, 3)
        // Sensor input creates and resolves the record; the history UI has no manual safety declaration.
        scenario.onActivity { MockEnvironmentDataSource.updateSensors(EnvironmentSensorState(gasOn = true)) }
        waitForRows(scenario, 4)
        acknowledgeActivityWarning(scenario)
        counts(scenario, false, 4, 1, 3)
        onView(withId(R.id.button_alert_pending)).perform(scrollTo(), click())
        waitForRows(scenario, 1)
        scenario.onActivity {
            val recycler = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)
            assertEquals(View.GONE, recycler.findViewHolderForAdapterPosition(0)!!.itemView
                .findViewById<View>(R.id.button_alert_item_resolve).visibility)
            MockEnvironmentDataSource.updateSensors(EnvironmentSensorState())
        }
        waitForRows(scenario, 0)
        counts(scenario, false, 4, 0, 4)
        onView(withId(R.id.linearLayout_alert_empty)).check(matches(isDisplayed()))
        scenario.onActivity {
            assertEquals(View.GONE, it.findViewById<View>(R.id.recyclerView_alert_list).visibility)
            assertEquals(it.getString(R.string.alert_empty_environment), it.findViewById<TextView>(R.id.textView_alert_empty_message).text.toString())
        }
        screenshot("${tag}_environment_empty")
        onView(withId(R.id.button_alert_resolved)).perform(scrollTo(), click())
        waitForRows(scenario, 4)
        scenario.onActivity {
            val recycler = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)
            val adapter = recycler.adapter as EnvironmentAlertAdapter
            assertTrue(adapter.currentList.all { row -> row.status == EnvironmentAlertStatus.RESOLVED })
            assertEquals(View.GONE, recycler.findViewHolderForAdapterPosition(0)!!.itemView.findViewById<View>(R.id.button_alert_item_resolve).visibility)
        }
    }

    @Test fun reentryAndRecreationPreserveAutoResolvedHistory() = withLocale("en") { scenario ->
        scenario.onActivity { MockEnvironmentDataSource.updateSensors(EnvironmentSensorState(gasOn = true)) }
        waitForRows(scenario, 4)
        acknowledgeActivityWarning(scenario)
        onView(withId(R.id.navTask)).perform(click())
        instrumentation.waitForIdleSync()
        onView(withId(R.id.textView_task_title)).check(matches(isDisplayed()))
        navigateAlert(scenario)
        waitForRows(scenario, 4)
        counts(scenario, false, 4, 1, 3)
        scenario.onActivity { MockEnvironmentDataSource.updateSensors(EnvironmentSensorState()) }
        waitForRows(scenario, 4)
        // The adapter contents, not just the unchanged total count, must reflect reconciliation.
        val deadline = SystemClock.uptimeMillis() + 5_000L
        var resolved = false
        do {
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list).adapter as EnvironmentAlertAdapter
                resolved = adapter.currentList.all { row -> row.status == EnvironmentAlertStatus.RESOLVED }
            }
            if (resolved) break
            SystemClock.sleep(50)
        } while (SystemClock.uptimeMillis() < deadline)
        assertTrue(resolved)
        scenario.recreate()
        navigateAlert(scenario)
        waitForRows(scenario, 4)
        counts(scenario, false, 4, 0, 4)
    }
    // App-level warnings now also appear over Alert Center; acknowledgement keeps the record pending.
    private fun acknowledgeActivityWarning(scenario: ActivityScenario<MainActivity>) {
        val limit = SystemClock.uptimeMillis() + 5_000L
        var showing = false
        do {
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val controller = MainActivity::class.java.getDeclaredField("warningController")
                    .apply { isAccessible = true }.get(it)
                val dialog = controller.javaClass.getDeclaredField("dialog")
                    .apply { isAccessible = true }.get(controller) as? androidx.appcompat.app.AlertDialog
                showing = dialog?.isShowing == true
            }
            if (showing) break
            SystemClock.sleep(25L)
        } while (SystemClock.uptimeMillis() < limit)
        assertTrue("Foreground gas warning must appear over Alert Center", showing)
        onView(withText(R.string.environment_warning_got_it))
            .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
        scenario.onActivity {
            assertEquals(EnvironmentAlertStatus.PENDING, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
    }
}
