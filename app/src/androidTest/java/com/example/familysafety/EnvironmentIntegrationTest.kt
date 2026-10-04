package com.example.familysafety

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real controls/navigation with a deterministic monotonic clock; no long timer waits. */
@RunWith(AndroidJUnit4::class)
class EnvironmentIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var now = 1_000L
    private fun tap() = click(InputDevice.SOURCE_TOUCHSCREEN, 0)

    private fun withEnvironment(tag: String = "en", realClock: Boolean = false, block: (ActivityScenario<MainActivity>) -> Unit) {
        instrumentation.runOnMainSync {
            now = 1_000L
            if (realClock) MockEnvironmentDataSource.resetForTests()
            else MockEnvironmentDataSource.resetForTests { now }
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
                navigateEnvironment(scenario)
                onView(withId(R.id.cardView_environment_gas_status)).check(matches(isDisplayed()))
                listOf(R.id.textView_environment_gas_status_title to R.string.environment_gas_status,
                    R.id.textView_environment_flame_status_title to R.string.environment_flame_detection,
                    R.id.textView_environment_person_status_title to R.string.environment_person_detection).forEach { (id, key) ->
                    onView(withId(id)).check(matches(withText(key)))
                }
                block(scenario)
            } finally {
                scenario.onActivity {
                    AppCompatDelegate.setApplicationLocales(previous)
                    MockEnvironmentDataSource.resetForTests()
                    MockEnvironmentAlertDataSource.resetForTests()
                    EnvironmentWarningController.reminderIntervalForTests = null
                }
            }
        }
    }

    private fun await(scenario: ActivityScenario<MainActivity>, condition: (MainActivity) -> Boolean) {
        val limit = SystemClock.uptimeMillis() + 5_000L
        do {
            instrumentation.waitForIdleSync()
            var ready = false
            scenario.onActivity { ready = condition(it) }
            if (ready) return
            SystemClock.sleep(25)
        } while (SystemClock.uptimeMillis() < limit)
        fail("Expected screen/data did not become ready")
    }

    private fun warningController(activity: MainActivity): Any? {
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content)
        if (fragment !is EnvironmentFragment) return null
        return EnvironmentFragment::class.java.getDeclaredField("warningController")
            .apply { isAccessible = true }.get(fragment)
    }
    private fun controllerField(controller: Any?, name: String): Any? = controller?.javaClass?.getDeclaredField(name)
        ?.apply { isAccessible = true }?.get(controller)
    private fun warningShowing(activity: MainActivity) =
        (controllerField(warningController(activity), "dialog") as? androidx.appcompat.app.AlertDialog)?.isShowing == true

    private fun acknowledgeWarningIfShown() {
        var showing = false
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().firstOrNull()
            showing = activity != null && warningShowing(activity)
        }
        if (showing) onView(withText(R.string.environment_warning_got_it)).inRoot(isDialog()).perform(tap())
    }
    private fun sensor(id: Int, acknowledge: Boolean = true) {
        acknowledgeWarningIfShown()
        onView(withId(id)).perform(scrollTo(), tap())
        if (acknowledge) acknowledgeWarningIfShown()
    }
    // A locale recreation can restore Home after the first navigation tap. Wait for the actual destination.
    private fun navigateEnvironment(scenario: ActivityScenario<MainActivity>) {
        val limit = SystemClock.uptimeMillis() + 5_000L
        do {
            var alreadyReady = false
            scenario.onActivity { alreadyReady = it.findViewById<View>(R.id.cardView_environment_gas_status) != null }
            if (alreadyReady) return
            onView(withId(R.id.navEnvironment)).perform(tap())
            instrumentation.waitForIdleSync()
            var ready = false
            scenario.onActivity { ready = it.findViewById<View>(R.id.cardView_environment_gas_status) != null }
            if (ready) return
            SystemClock.sleep(25)
        } while (SystemClock.uptimeMillis() < limit)
        fail("Environment destination did not become ready after locale/navigation")
    }
    // Read-only runtime trace: do not reconcile through getCountdown while checking ticker settlement.
    private fun trace(scenario: ActivityScenario<MainActivity>, label: String) {
        scenario.onActivity {
            fun field(name: String): Any? = MockEnvironmentDataSource::class.java.getDeclaredField(name)
                .apply { isAccessible = true }.get(MockEnvironmentDataSource)
            val alerts = MockEnvironmentAlertDataSource.getAlerts()
            val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter as? EnvironmentAlertAdapter
            Log.d("EnvironmentRuntimeTrace", "$label sensors=${MockEnvironmentDataSource.sensorState} " +
                "gasLatch=${field("gasRiskLatched")} cycleConsumed=${field("unattendedCycleConsumed")} " +
                "phase=${field("phase")} sharedCount=${alerts.size} last=${alerts.lastOrNull()} " +
                "visibleIds=${adapter?.currentList?.map { row -> row.id }}")
        }
    }

    private fun configure(minutes: Int) {
        acknowledgeWarningIfShown()
        onView(withId(R.id.button_environment_countdown_setting)).perform(scrollTo(), tap())
        onView(isAssignableFrom(EditText::class.java)).perform(replaceText(minutes.toString()), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).perform(tap())
    }
    private fun showAlert(scenario: ActivityScenario<MainActivity>, type: EnvironmentAlertType, count: Int = 4) {
        acknowledgeWarningIfShown()
        onView(withId(R.id.navAlert)).perform(tap())
        onView(withId(R.id.button_alert_environment)).perform(tap())
        onView(withId(R.id.button_alert_pending)).perform(tap())
        await(scenario) { activity ->
            val list = activity.findViewById<RecyclerView>(R.id.recyclerView_alert_list)
            val adapter = list?.adapter as? EnvironmentAlertAdapter
            adapter?.currentList?.size == 1 && adapter.currentList.firstOrNull()?.type == type &&
                list?.findViewHolderForAdapterPosition(0) != null
        }
        onView(withId(R.id.recyclerView_alert_list)).check { view, failure ->
            if (failure != null) throw failure
            val list = view as RecyclerView
            val adapter = list.adapter as EnvironmentAlertAdapter
            assertTrue(adapter.currentList.first().id > 3L)
            assertEquals(count, MockEnvironmentAlertDataSource.getAlerts().size)
            val title = list.findViewHolderForAdapterPosition(0)!!.itemView
                .findViewById<TextView>(R.id.textView_alert_item_title)
            assertEquals(view.context.getString(if (type == EnvironmentAlertType.GAS_LEAK_RISK)
                R.string.alert_type_gas_leak else R.string.alert_type_unattended), title.text.toString())
            assertEquals(View.VISIBLE, title.visibility)
        }
    }

    @Test fun gasRiskCreatesVisiblePendingAlert() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        onView(withId(R.id.textView_environment_gas_value)).check(matches(withText(R.string.environment_status_on)))
        showAlert(scenario, EnvironmentAlertType.GAS_LEAK_RISK)
    }

    @Test fun runtimeFreshGasAndCancelledCycleTrace() = withEnvironment("zh-TW") { scenario ->
        trace(scenario, "fresh-before-gas")
        sensor(R.id.cardView_environment_gas_status)
        trace(scenario, "after-gas")
        scenario.onActivity {
            assertEquals(EnvironmentSensorState(true, false, false), MockEnvironmentDataSource.sensorState)
            assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
            assertEquals(EnvironmentAlertType.GAS_LEAK_RISK, MockEnvironmentAlertDataSource.getAlerts().last().type)
        }
        showAlert(scenario, EnvironmentAlertType.GAS_LEAK_RISK)
        trace(scenario, "gas-visible-pending")
        onView(withId(R.id.navEnvironment)).perform(tap())
        sensor(R.id.cardView_environment_flame_status)
        trace(scenario, "after-flame-no-person-click")
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_running)))
        onView(withId(R.id.button_environment_countdown_start)).perform(scrollTo(), tap())
        sensor(R.id.cardView_environment_flame_status)
        sensor(R.id.cardView_environment_flame_status)
        trace(scenario, "cancelled-after-flame-off-on")
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_cancelled)))
        configure(18)
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_cancelled)))
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("18:00")))
        sensor(R.id.cardView_environment_person_status)
        sensor(R.id.cardView_environment_person_status)
        trace(scenario, "person-return-then-leave")
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_running)))
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("18:00")))
    }

    @Test fun realClockTickerSettlesExpiry() = withEnvironment("zh-TW", realClock = true) { scenario ->
        configure(1)
        // Explicit opt-in runs the full production minute once; regular suite uses the same engine for 3 seconds.
        val fullMinute = InstrumentationRegistry.getArguments().getString("environmentFullMinute") == "true"
        if (!fullMinute) scenario.onActivity { MockEnvironmentDataSource.setDurationOverrideForTests(3_000L) }
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        trace(scenario, "real-clock-start")
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_running)))
        val limit = SystemClock.elapsedRealtime() + 70_000L
        var finished = false
        while (!finished && SystemClock.elapsedRealtime() < limit) {
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                finished = it.findViewById<TextView>(R.id.textView_environment_countdown_status).text.toString() ==
                    it.getString(R.string.countdown_status_finished)
            }
            if (!finished) SystemClock.sleep(100)
        }
        assertTrue("Production ticker must settle while Environment stays visible", finished)
        trace(scenario, "real-clock-finished-before-alert-entry")
        scenario.onActivity {
            val alerts = MockEnvironmentAlertDataSource.getAlerts()
            assertEquals(5, alerts.size)
            assertEquals(1, alerts.drop(3).count { row -> row.type == EnvironmentAlertType.UNATTENDED_COOKING })
        }
        acknowledgeWarningIfShown()
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("00:00")))
        showAlert(scenario, EnvironmentAlertType.UNATTENDED_COOKING, 5)
        trace(scenario, "real-clock-unattended-visible")
    }

    @Test fun flameStartsAfterGasWithoutPersonClickAndPersonReturnResets() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_running)))
        scenario.onActivity { assertTrue(MockEnvironmentDataSource.sensorState.gasOn) }
        sensor(R.id.cardView_environment_person_status)
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_standby)))
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("10:00")))
        scenario.onActivity { assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size) }
    }

    @Test fun gasOffExpiryRetainsResolvedHistoryWithoutWarning() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        sensor(R.id.cardView_environment_gas_status)
        scenario.onActivity {
            assertEquals(EnvironmentSensorState(), MockEnvironmentDataSource.sensorState)
            assertEquals(EnvironmentCountdownPhase.RUNNING, MockEnvironmentDataSource.getCountdown().phase)
            MockEnvironmentDataSource.setDeadlineForTests(now + 1)
            now += 2
        }
        onView(withId(R.id.navAlert)).perform(tap())
        onView(withId(R.id.button_alert_resolved)).perform(tap())
        await(scenario) {
            val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter as? EnvironmentAlertAdapter
            adapter?.currentList?.any { row -> row.id == 5L && row.type == EnvironmentAlertType.UNATTENDED_COOKING } == true
        }
        scenario.onActivity { assertNull(MockEnvironmentDataSource.getActiveWarning()) }
    }

    @Test fun navigationAndRecreationPreserveCountdownAndExpiryIsConsumedOnce() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        var deadline: Long? = null
        scenario.onActivity { deadline = MockEnvironmentDataSource.getCountdown().deadlineElapsed }
        onView(withId(R.id.navTask)).perform(tap())
        scenario.onActivity { now += 30_000L }
        onView(withId(R.id.navEnvironment)).perform(tap())
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("09:30")))
        scenario.recreate()
        onView(withId(R.id.navEnvironment)).perform(tap())
        scenario.onActivity { assertEquals(deadline, MockEnvironmentDataSource.getCountdown().deadlineElapsed) }
        onView(withId(R.id.navTask)).perform(tap())
        scenario.onActivity { now = deadline!! + 1L }
        onView(withId(R.id.navEnvironment)).perform(tap())
        acknowledgeWarningIfShown()
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("00:00")))
        repeat(2) {
            acknowledgeWarningIfShown()
            onView(withId(R.id.navTask)).perform(tap())
            onView(withId(R.id.navEnvironment)).perform(tap())
        }
        scenario.onActivity { assertEquals(5, MockEnvironmentAlertDataSource.getAlerts().size) }
        showAlert(scenario, EnvironmentAlertType.UNATTENDED_COOKING, 5)
    }

    @Test fun autoResolvePersistsAfterLeavingAlert() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        showAlert(scenario, EnvironmentAlertType.GAS_LEAK_RISK)
        onView(withId(R.id.navEnvironment)).perform(tap())
        sensor(R.id.cardView_environment_gas_status)
        onView(withId(R.id.navAlert)).perform(tap())
        onView(withId(R.id.button_alert_resolved)).perform(tap())
        await(scenario) {
            val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter as? EnvironmentAlertAdapter
            adapter?.currentList?.any { row -> row.id == 4L && row.status == EnvironmentAlertStatus.RESOLVED } == true
        }
        scenario.onActivity { assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size) }
    }

    @Test fun traditionalChineseSensorControlsAndSharedAlert() = withEnvironment("zh-TW") { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        onView(withId(R.id.textView_environment_gas_value)).check(matches(withText("開啟")))
        showAlert(scenario, EnvironmentAlertType.GAS_LEAK_RISK)
    }

    @Test fun settingAndLocationDialogsAreDismissedWhenViewIsDestroyed() = withEnvironment { scenario ->
        onView(withId(R.id.button_environment_countdown_setting)).perform(scrollTo(), tap())
        onView(withId(android.R.id.button1)).check(matches(withText(R.string.countdown_dialog_confirm)))
        scenario.onActivity { it.findViewById<View>(R.id.navTask).performClick() }
        onView(withId(android.R.id.button1)).check(doesNotExist())
        onView(withId(R.id.navEnvironment)).perform(tap())
        onView(withId(R.id.button_environment_location)).perform(scrollTo(), tap())
        scenario.onActivity { it.findViewById<View>(R.id.navTask).performClick() }
        onView(withId(android.R.id.button2)).check(doesNotExist())
    }

    @Test fun mockSensorTransitionsPreventFlameWithoutGas() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_flame_status)
        scenario.onActivity { assertEquals(EnvironmentSensorState(), MockEnvironmentDataSource.sensorState) }
        onView(withId(R.id.textView_environment_flame_value)).check(matches(withText(R.string.environment_status_not_detected)))
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_running)))
        sensor(R.id.cardView_environment_gas_status)
        scenario.onActivity {
            assertEquals(EnvironmentSensorState(), MockEnvironmentDataSource.sensorState)
            assertEquals(EnvironmentCountdownPhase.RUNNING, MockEnvironmentDataSource.getCountdown().phase)
        }
        onView(withId(R.id.textView_environment_flame_value)).check(matches(withText(R.string.environment_status_not_detected)))
    }

    @Test fun cancelConfigureAndManualRestartPreserveEighteenMinutes() = withEnvironment { scenario ->
        configure(18)
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("18:00")))
        onView(withId(R.id.button_environment_countdown_start)).perform(scrollTo(), tap())
        configure(18)
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_cancelled)))
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("18:00")))
        onView(withId(R.id.button_environment_countdown_start)).perform(scrollTo(), tap())
        scenario.onActivity { now += 1_080_001L }
        await(scenario) { it.findViewById<TextView>(R.id.textView_environment_countdown_status).text.toString() ==
            it.getString(R.string.countdown_status_finished) }
        scenario.onActivity {
            assertEquals(18, MockEnvironmentDataSource.getCountdown().configuredMinutes)
            assertEquals(5, MockEnvironmentAlertDataSource.getAlerts().size)
        }
        acknowledgeWarningIfShown()
        sensor(R.id.cardView_environment_person_status)
        sensor(R.id.cardView_environment_person_status)
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("18:00")))
        onView(withId(R.id.navTask)).perform(tap())
        onView(withId(R.id.navEnvironment)).perform(tap())
        scenario.recreate()
        onView(withId(R.id.navEnvironment)).perform(tap())
        onView(withId(R.id.textView_environment_countdown_remaining)).check(matches(withText("18:00")))
        onView(withId(R.id.navAlert)).perform(tap())
        onView(withId(R.id.button_alert_resolved)).perform(tap())
        await(scenario) {
            val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter as? EnvironmentAlertAdapter
            adapter?.currentList?.any { row -> row.id == 5L && row.status == EnvironmentAlertStatus.RESOLVED } == true
        }
    }

    @Test fun gasWarningAcknowledgementRemindsWithoutDuplicateAndViewAlertNavigates() = withEnvironment { scenario ->
        scenario.onActivity { EnvironmentWarningController.reminderIntervalForTests = 1_500L }
        sensor(R.id.cardView_environment_gas_status, acknowledge = false)
        onView(withText(R.string.alert_type_gas_leak)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(android.R.id.message)).inRoot(isDialog())
            .check(matches(withText(R.string.environment_warning_gas_message)))
        scenario.onActivity {
            assertNotNull(controllerField(warningController(it), "tone"))
            assertEquals(EnvironmentAlertStatus.PENDING, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
        onView(withText(R.string.environment_warning_got_it)).inRoot(isDialog()).perform(tap())
        scenario.onActivity {
            assertFalse(warningShowing(it))
            assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
            assertEquals(EnvironmentAlertStatus.PENDING, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
        await(scenario) { warningShowing(it) }
        scenario.onActivity { assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size) }
        onView(withText(R.string.environment_warning_view_alert)).inRoot(isDialog()).perform(tap())
        await(scenario) {
            val adapter = it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter as? EnvironmentAlertAdapter
            adapter?.currentList?.any { row -> row.id == 4L && row.status == EnvironmentAlertStatus.PENDING } == true
        }
    }

    @Test fun gasAutoResolveDismissesWarningAndHistoryMovesFilters() = withEnvironment("zh-TW") { scenario ->
        sensor(R.id.cardView_environment_gas_status, acknowledge = false)
        onView(withText("瓦斯外洩風險")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("知道了")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("查看警報")).inRoot(isDialog()).check(matches(isDisplayed()))
        // Simulate a sensor change while the modal is open through the same production listener.
        scenario.onActivity { it.findViewById<View>(R.id.cardView_environment_gas_status).performClick() }
        await(scenario) { !warningShowing(it) }
        scenario.onActivity {
            assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
            assertEquals(EnvironmentAlertStatus.RESOLVED, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
        onView(withId(R.id.navAlert)).perform(tap())
        onView(withId(R.id.button_alert_pending)).perform(tap())
        await(scenario) { (it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter as? EnvironmentAlertAdapter)
            ?.currentList?.isEmpty() == true }
        onView(withId(R.id.button_alert_resolved)).perform(tap())
        await(scenario) { (it.findViewById<RecyclerView>(R.id.recyclerView_alert_list)?.adapter as? EnvironmentAlertAdapter)
            ?.currentList?.any { row -> row.id == 4L } == true }
        navigateEnvironment(scenario)
        sensor(R.id.cardView_environment_gas_status, acknowledge = false)
        scenario.onActivity {
            assertEquals(5L, MockEnvironmentAlertDataSource.getAlerts().last().id)
            assertTrue(warningShowing(it))
        }
    }

    @Test fun flameLossEscalatesCountdownIntoImmediateGasWarning() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        onView(withId(R.id.textView_environment_countdown_status)).check(matches(withText(R.string.countdown_status_running)))
        sensor(R.id.cardView_environment_flame_status, acknowledge = false)
        onView(withText(R.string.alert_type_gas_leak)).inRoot(isDialog()).check(matches(isDisplayed()))
        scenario.onActivity {
            assertEquals(EnvironmentCountdownPhase.IDLE, MockEnvironmentDataSource.getCountdown().phase)
            assertEquals(0, MockEnvironmentAlertDataSource.getAlerts().drop(3)
                .count { row -> row.type == EnvironmentAlertType.UNATTENDED_COOKING })
            assertEquals(5L, MockEnvironmentDataSource.getActiveWarning()!!.id)
        }
    }

    @Test fun unattendedWarningLifecycleResumesAndPersonReturnAutoResolves() = withEnvironment { scenario ->
        sensor(R.id.cardView_environment_gas_status)
        sensor(R.id.cardView_environment_flame_status)
        scenario.onActivity {
            MockEnvironmentDataSource.setDeadlineForTests(now + 1)
            now += 2
        }
        await(scenario) { warningShowing(it) }
        onView(withText(R.string.alert_type_unattended)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(android.R.id.message)).inRoot(isDialog())
            .check(matches(withText(R.string.environment_warning_unattended_message)))
        var oldController: Any? = null
        scenario.onActivity {
            oldController = warningController(it)
            assertNotNull(controllerField(oldController, "tone"))
            it.findViewById<View>(R.id.navTask).performClick()
        }
        await(scenario) { it.findViewById<View>(R.id.textView_task_title) != null }
        scenario.onActivity {
            assertNull(controllerField(oldController, "dialog"))
            assertNull(controllerField(oldController, "tone"))
            assertEquals(EnvironmentAlertStatus.PENDING, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
        navigateEnvironment(scenario)
        await(scenario) { warningShowing(it) }
        scenario.recreate()
        await(scenario) { warningShowing(it) }
        scenario.onActivity {
            assertEquals(5, MockEnvironmentAlertDataSource.getAlerts().size)
            it.findViewById<View>(R.id.cardView_environment_person_status).performClick()
        }
        await(scenario) { !warningShowing(it) }
        scenario.onActivity {
            assertEquals(EnvironmentCountdownPhase.IDLE, MockEnvironmentDataSource.getCountdown().phase)
            assertEquals(EnvironmentAlertStatus.RESOLVED, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
    }
}
