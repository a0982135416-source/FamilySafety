package com.example.familysafety

import android.os.SystemClock
import android.view.InputDevice
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Environment controls, shared readings and Home views in both supported locales. */
@RunWith(AndroidJUnit4::class)
class HomeEnvironmentIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var now = 1_000L
    private fun tap() = click(InputDevice.SOURCE_TOUCHSCREEN, 0)
    private fun text(activity: MainActivity, id: Int) = activity.findViewById<TextView>(id).text.toString()
    private fun controller(activity: MainActivity): Any = MainActivity::class.java
        .getDeclaredField("warningController").apply { isAccessible = true }.get(activity)!!
    private fun dialog(activity: MainActivity): AlertDialog? {
        val owner = controller(activity)
        return owner.javaClass.getDeclaredField("dialog").apply { isAccessible = true }.get(owner) as? AlertDialog
    }
    private fun await(scenario: ActivityScenario<MainActivity>, predicate: (MainActivity) -> Boolean) {
        val end = SystemClock.uptimeMillis() + 5_000L
        do {
            instrumentation.waitForIdleSync()
            var ready = false
            scenario.onActivity { ready = predicate(it) }
            if (ready) return
            SystemClock.sleep(25L)
        } while (SystemClock.uptimeMillis() < end)
        fail("Home/Environment view state did not settle")
    }
    private fun navigate(scenario: ActivityScenario<MainActivity>, destination: Int, readyId: Int) {
        // Locale recreation can restore Home after navigation. Await the actual destination.
        val end = SystemClock.uptimeMillis() + 5_000L
        do {
            var ready = false
            scenario.onActivity {
                if (it.findViewById<View>(readyId) == null) {
                    dialog(it)?.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick()
                    it.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation).selectedItemId = destination
                    it.supportFragmentManager.executePendingTransactions()
                }
                ready = it.findViewById<View>(readyId) != null
            }
            if (ready) return
            instrumentation.waitForIdleSync()
            SystemClock.sleep(25L)
        } while (SystemClock.uptimeMillis() < end)
        fail("Destination $destination did not settle")
    }
    private fun home(scenario: ActivityScenario<MainActivity>) = navigate(scenario, R.id.navHome, R.id.textView_home_status)
    private fun environment(scenario: ActivityScenario<MainActivity>) =
        navigate(scenario, R.id.navEnvironment, R.id.cardView_environment_gas_status)
    private fun sensor(scenario: ActivityScenario<MainActivity>, id: Int) {
        scenario.onActivity { dialog(it)?.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick() }
        onView(withId(id)).perform(scrollTo(), tap())
        instrumentation.waitForIdleSync()
        scenario.onActivity { dialog(it)?.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick() }
    }
    private fun withHome(tag: String, block: (ActivityScenario<MainActivity>) -> Unit) {
        instrumentation.runOnMainSync {
            MockTaskDataSource.resetForTests()
            now = 1_000L
            MockEnvironmentDataSource.resetForTests { now }
            MockEnvironmentAlertDataSource.resetForTests()
            EnvironmentWarningController.reminderIntervalForTests = null
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var previous = LocaleListCompat.getEmptyLocaleList()
            try {
                scenario.onActivity {
                    previous = AppCompatDelegate.getApplicationLocales()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                }
                await(scenario) { it.resources.configuration.locales[0].language == if (tag == "en") "en" else "zh" }
                home(scenario)
                block(scenario)
            } finally {
                scenario.onActivity {
                    MockTaskDataSource.resetForTests()
                    MockEnvironmentDataSource.resetForTests()
                    MockEnvironmentAlertDataSource.resetForTests()
                    AppCompatDelegate.setApplicationLocales(previous)
                    EnvironmentWarningController.reminderIntervalForTests = null
                }
            }
        }
    }
    private fun assertHome(scenario: ActivityScenario<MainActivity>, status: Int, color: Int,
        gas: Int, flame: Int, person: Int) {
        await(scenario) { it.findViewById<TextView>(R.id.textView_home_status)?.text == it.getString(status) }
        scenario.onActivity {
            assertEquals(it.getString(gas), text(it, R.id.textView_home_gas_status_value))
            assertEquals(it.getString(flame), text(it, R.id.textView_home_fire_status_value))
            assertEquals(it.getString(person), text(it, R.id.textView_home_person_status_value))
            assertEquals(ContextCompat.getColor(it, color), it.findViewById<TextView>(R.id.textView_home_status).currentTextColor)
            assertEquals(it.getString(R.string.home_location_kitchen), text(it, R.id.textView_home_safety_location))
            val today = MockTaskDataSource.getTasks { key -> it.getString(key) }.dueToday()
            val completed = today.count { row -> row.status == TaskStatus.COMPLETED }
            assertEquals(today.size, it.findViewById<RecyclerView>(R.id.recyclerView_home_tasks).adapter!!.itemCount)
            assertEquals(it.getString(R.string.task_progress_fraction, completed, today.size), text(it, R.id.textView_home_progress_center_text))
            val pending = MockEnvironmentAlertDataSource.getAlerts().count { row -> row.status == EnvironmentAlertStatus.PENDING }
            assertEquals(it.getString(R.string.home_alert_pending_count, pending), text(it, R.id.textView_home_alert_count))
            assertEquals(it.getString(when (MockEnvironmentDataSource.getActiveWarning()?.type) {
                EnvironmentAlertType.GAS_LEAK_RISK -> R.string.alert_message_gas_leak
                EnvironmentAlertType.UNATTENDED_COOKING -> R.string.environment_warning_unattended_message
                null -> R.string.home_alert_none
            }), text(it, R.id.textView_home_alert_msg))
        }
    }

    private fun sensorsAndStatus(tag: String) = withHome(tag) { scenario ->
        assertHome(scenario, R.string.home_status_safe, R.color.safe_green,
            R.string.environment_status_off, R.string.environment_status_not_detected, R.string.environment_status_not_detected)
        environment(scenario)
        sensor(scenario, R.id.cardView_environment_gas_status)
        home(scenario)
        assertHome(scenario, R.string.home_status_danger, R.color.warning_red,
            R.string.environment_status_on, R.string.environment_status_not_detected, R.string.environment_status_not_detected)
        environment(scenario)
        sensor(scenario, R.id.cardView_environment_flame_status)
        home(scenario)
        assertHome(scenario, R.string.home_status_warning, R.color.warning_orange,
            R.string.environment_status_on, R.string.environment_status_detected, R.string.environment_status_not_detected)
        environment(scenario)
        sensor(scenario, R.id.cardView_environment_person_status)
        home(scenario)
        assertHome(scenario, R.string.home_status_safe, R.color.safe_green,
            R.string.environment_status_on, R.string.environment_status_detected, R.string.environment_status_detected)
        scenario.onActivity {
            assertEquals(ContextCompat.getColor(it, R.color.primary_blue),
                it.findViewById<TextView>(R.id.textView_home_gas_status_value).currentTextColor)
            assertEquals(ContextCompat.getColor(it, R.color.primary_blue),
                it.findViewById<TextView>(R.id.textView_home_fire_status_value).currentTextColor)
            assertNull(MockEnvironmentDataSource.getActiveWarning())
            assertEquals(EnvironmentAlertStatus.RESOLVED, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
    }
    @Test fun englishSensorsAndThreeSafetyStatesRefreshAcrossNavigation() = sensorsAndStatus("en")
    @Test fun traditionalChineseSensorsAndThreeSafetyStatesRefreshAcrossNavigation() = sensorsAndStatus("zh-TW")

    private fun locations(tag: String) = withHome(tag) { scenario ->
        listOf("KITCHEN" to R.string.location_kitchen, "LIVING_ROOM" to R.string.location_living_room,
            "BEDROOM" to R.string.location_bedroom).forEach { (location, label) ->
            environment(scenario)
            onView(withId(R.id.button_environment_location)).perform(scrollTo(), tap())
            onView(withText(label)).perform(tap())
            var temperature = ""
            var humidity = ""
            scenario.onActivity {
                assertEquals(location, MockEnvironmentDataSource.selectedLocation)
                temperature = text(it, R.id.textView_environment_temperature_value)
                humidity = text(it, R.id.textView_environment_humidity_value)
                val data = MockEnvironmentDataSource.getCurrentEnvironmentData()
                assertEquals(it.getString(R.string.environment_temperature_value_format, data.temperature), temperature)
                assertEquals(it.getString(R.string.environment_humidity_value_format, data.humidity), humidity)
            }
            home(scenario)
            scenario.onActivity {
                assertEquals(temperature, text(it, R.id.textView_home_temperature_value))
                assertEquals(humidity, text(it, R.id.textView_home_humidity_value))
                assertEquals(it.getString(R.string.home_environment_location_format, it.getString(label)),
                    text(it, R.id.textView_home_environment_location))
                assertEquals(it.getString(R.string.home_location_kitchen), text(it, R.id.textView_home_safety_location))
            }
        }
        // Selection survives Fragment replacement and Activity recreation in the same process.
        scenario.recreate()
        home(scenario)
        scenario.onActivity { assertEquals("BEDROOM", MockEnvironmentDataSource.selectedLocation) }
        environment(scenario)
        scenario.onActivity { assertEquals(it.getString(R.string.location_bedroom_selector), text(it, R.id.button_environment_location)) }
    }
    @Test fun englishThreeLocationsShareReadingsAndRetainSelection() = locations("en")
    @Test fun traditionalChineseThreeLocationsShareReadingsAndRetainSelection() = locations("zh-TW")

    @Test fun countdownExpiresWhileHomeVisibleAndActiveDangerOutranksRestartThenResolves() = withHome("en") { scenario ->
        environment(scenario)
        sensor(scenario, R.id.cardView_environment_gas_status)
        sensor(scenario, R.id.cardView_environment_flame_status)
        home(scenario)
        assertHome(scenario, R.string.home_status_warning, R.color.warning_orange,
            R.string.environment_status_on, R.string.environment_status_detected, R.string.environment_status_not_detected)
        var owner: Any? = null
        scenario.onActivity {
            owner = controller(it)
            MockEnvironmentDataSource.setDeadlineForTests(now + 1L)
            now += 2L
        }
        assertHome(scenario, R.string.home_status_danger, R.color.warning_red,
            R.string.environment_status_on, R.string.environment_status_detected, R.string.environment_status_not_detected)
        await(scenario) { dialog(it)?.isShowing == true }
        scenario.onActivity {
            assertSame(owner, controller(it))
            assertEquals(EnvironmentAlertType.UNATTENDED_COOKING, MockEnvironmentDataSource.getActiveWarning()!!.type)
            dialog(it)!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            MockEnvironmentDataSource.configureMinutes(15)
            assertEquals(EnvironmentCountdownPhase.RUNNING, MockEnvironmentDataSource.getCountdown().phase)
        }
        assertHome(scenario, R.string.home_status_danger, R.color.warning_red,
            R.string.environment_status_on, R.string.environment_status_detected, R.string.environment_status_not_detected)
        environment(scenario)
        sensor(scenario, R.id.cardView_environment_person_status)
        home(scenario)
        assertHome(scenario, R.string.home_status_safe, R.color.safe_green,
            R.string.environment_status_on, R.string.environment_status_detected, R.string.environment_status_detected)
        await(scenario) { dialog(it) == null }
        scenario.onActivity {
            assertEquals(5, MockEnvironmentAlertDataSource.getAlerts().size)
            assertTrue(MockEnvironmentAlertDataSource.getAlerts().all { row -> row.status == EnvironmentAlertStatus.RESOLVED })
        }
    }
    @Test fun englishSharedTasksAndAlertNavigation() = sharedTasksAndNavigation("en")
    @Test fun traditionalChineseSharedTasksAndAlertNavigation() = sharedTasksAndNavigation("zh-TW")

    private fun sharedTasksAndNavigation(tag: String) = withHome(tag) { scenario ->
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12); set(java.util.Calendar.MINUTE, 0)
        }.timeInMillis
        scenario.onActivity {
            MockTaskDataSource.replaceTasks(listOf(
                Task(90, "Today", "detail", "Alex", today, TaskStatus.PENDING),
                Task(91, "Done", "detail", "Alex", today, TaskStatus.COMPLETED),
                Task(92, "Tomorrow", "detail", "Alex", today + 86_400_000L, TaskStatus.PENDING)))
        }
        environment(scenario)
        home(scenario)
        fun checkHome(completed: Int, total: Int, status: Int?) {
            scenario.onActivity {
                val list = it.findViewById<RecyclerView>(R.id.recyclerView_home_tasks)
                assertEquals(total, list.adapter!!.itemCount)
                assertEquals(it.getString(R.string.task_progress_fraction, completed, total), text(it, R.id.textView_home_progress_center_text))
                val progress = it.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.circularProgressIndicator_home_task)
                assertEquals(total, progress.max)
                assertEquals(completed, progress.progress)
                assertEquals(it.getString(R.string.task_completion_rate, if (total == 0) 0 else completed * 100 / total), text(it, R.id.textView_home_task_rate))
                if (total == 0) assertEquals(View.VISIBLE, it.findViewById<View>(R.id.textView_home_tasks_empty).visibility)
            }
            if (status != null) {
                await(scenario) { it.findViewById<RecyclerView>(R.id.recyclerView_home_tasks).findViewHolderForAdapterPosition(0) != null }
                scenario.onActivity {
                    val card = it.findViewById<RecyclerView>(R.id.recyclerView_home_tasks).findViewHolderForAdapterPosition(0)!!.itemView
                    assertEquals(it.getString(status), card.findViewById<TextView>(R.id.textView_home_task_item_status_badge).text.toString())
                    val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT,
                        it.resources.configuration.locales[0]).format(java.util.Date(today))
                    assertEquals(date, card.findViewById<TextView>(R.id.textView_home_task_item_time).text.toString())
                    val title = card.findViewById<TextView>(R.id.textView_home_task_item_name)
                    assertTrue(title.isSelected)
                    assertEquals(android.text.TextUtils.TruncateAt.MARQUEE, title.ellipsize)
                    assertEquals(1, title.maxLines)
                }
            }
        }
        fun alertTasks(expected: Set<Long>, firstStatus: TaskStatus? = null) {
            navigate(scenario, R.id.navAlert, R.id.button_alert_task)
            onView(withId(R.id.button_alert_task)).perform(tap())
            await(scenario) {
                val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list).adapter as? TaskAlertAdapter
                adapter?.currentList?.map { row -> row.alert.taskId }?.toSet() == expected
            }
            scenario.onActivity {
                val rows = (it.findViewById<RecyclerView>(R.id.recyclerView_alert_list).adapter as TaskAlertAdapter).currentList
                if (firstStatus != null) assertEquals(firstStatus, rows.single { row -> row.alert.taskId == 90L }.alert.status)
            }
        }
        fun taskAction(label: Int) {
            navigate(scenario, R.id.navTask, R.id.recyclerView_task_list)
            await(scenario) { it.findViewById<RecyclerView>(R.id.recyclerView_task_list).findViewHolderForAdapterPosition(0) != null }
            scenario.onActivity { it.findViewById<RecyclerView>(R.id.recyclerView_task_list).findViewHolderForAdapterPosition(0)!!.itemView.performClick() }
            onView(withText(label)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(tap())
        }
        checkHome(1, 2, R.string.task_status_pending)
        taskAction(R.string.task_action_start)
        home(scenario)
        checkHome(1, 2, R.string.task_status_in_progress)
        alertTasks(setOf(90L, 92L), TaskStatus.IN_PROGRESS)
        taskAction(R.string.task_action_complete)
        home(scenario)
        checkHome(2, 2, R.string.task_status_completed)
        alertTasks(setOf(92L))
        // Exercise existing Management CRUD APIs; the remaining form interaction is manual acceptance.
        var createdId = 0L
        try {
            scenario.onActivity {
                MockAuthDataSource.resetForTests()
                val admin = MockMemberDataSource.getMembers().first { member -> member.role == MemberRole.ADMIN }
                assertNotNull(MockAuthDataSource.login(admin.account, MockAuthDataSource.INITIAL_PASSWORD))
                val due = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 23); set(java.util.Calendar.MINUTE, 59); set(java.util.Calendar.SECOND, 59)
                }.timeInMillis
                createdId = MockTaskDataSource.addTask(1, admin.id, due, { key -> it.getString(key) })!!.id
            }
            home(scenario)
            checkHome(2, 3, R.string.task_status_completed)
            alertTasks(setOf(92L, createdId))
            scenario.onActivity {
                val task = MockTaskDataSource.getTasks { key -> it.getString(key) }.single { row -> row.id == createdId }
                assertNotNull(MockTaskDataSource.updateTask(createdId, 2, task.assigneeId!!, task.dueDate, { key -> it.getString(key) }))
            }
            home(scenario)
            scenario.onActivity {
                val task = MockTaskDataSource.getTasks { key -> it.getString(key) }.single { row -> row.id == createdId }
                assertEquals(task.title, MockTaskAlertDataSource.create { key -> it.getString(key) }.single { row -> row.taskId == createdId }.title)
                assertTrue(MockTaskDataSource.deleteTask(createdId, { key -> it.getString(key) }))
            }
            alertTasks(setOf(92L))
            home(scenario)
            checkHome(2, 2, R.string.task_status_completed)
            onView(withId(R.id.cardView_home_alert)).perform(scrollTo(), tap())
            await(scenario) { it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter is EnvironmentAlertAdapter }
            scenario.onActivity {
                assertEquals(R.id.navAlert, it.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation).selectedItemId)
                MockTaskDataSource.replaceTasks(emptyList())
            }
            home(scenario)
            checkHome(0, 0, null)
        } finally { scenario.onActivity { MockAuthDataSource.resetForTests() } }
    }

}
