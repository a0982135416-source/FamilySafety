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
            // The linkage must not replace Home tasks or its still-static alert summary.
            assertEquals(4, it.findViewById<RecyclerView>(R.id.recyclerView_home_tasks).adapter!!.itemCount)
            assertEquals(it.getString(R.string.task_progress_fraction, 3, 5), text(it, R.id.textView_home_progress_center_text))
            assertEquals(it.getString(R.string.home_alert_pending_preview), text(it, R.id.textView_home_alert_count))
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
}
