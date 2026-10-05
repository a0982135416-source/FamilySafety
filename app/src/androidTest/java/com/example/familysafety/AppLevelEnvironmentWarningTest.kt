package com.example.familysafety

import android.os.SystemClock
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the Activity-owned production warning engine while Environment is detached. */
@RunWith(AndroidJUnit4::class)
class AppLevelEnvironmentWarningTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var now = 1_000L
    private fun controller(activity: MainActivity): Any = MainActivity::class.java
        .getDeclaredField("warningController").apply { isAccessible = true }.get(activity)!!
    private fun field(owner: Any, name: String): Any? = owner.javaClass.getDeclaredField(name)
        .apply { isAccessible = true }.get(owner)
    private fun dialog(activity: MainActivity) = field(controller(activity), "dialog") as? AlertDialog
    private fun nav(activity: MainActivity, id: Int) {
        activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation).selectedItemId = id
        activity.supportFragmentManager.executePendingTransactions()
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
        fail("Activity warning condition did not settle")
    }
    private fun runScenario(block: (ActivityScenario<MainActivity>) -> Unit) {
        instrumentation.runOnMainSync {
            now = 1_000L
            MockEnvironmentDataSource.resetForTests { now }
            MockEnvironmentAlertDataSource.resetForTests()
            EnvironmentWarningController.reminderIntervalForTests = 1_500L
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use(block)
        } finally {
            instrumentation.runOnMainSync {
                MockEnvironmentDataSource.resetForTests()
                MockEnvironmentAlertDataSource.resetForTests()
                EnvironmentWarningController.reminderIntervalForTests = null
            }
        }
    }

    @Test fun fivePagesShareOneDialogAndReminderWithoutDuplicateRecords() = runScenario { scenario ->
        scenario.onActivity {
            MockEnvironmentDataSource.updateSensors(EnvironmentSensorState(gasOn = true))
        }
        await(scenario) { dialog(it)?.isShowing == true }
        var original: Any? = null
        var acknowledgedWarning: AlertDialog? = null
        scenario.onActivity { original = controller(it) }
        listOf(R.id.navHome, R.id.navEnvironment, R.id.navAlert, R.id.navTask, R.id.navManagement).forEach { id ->
            scenario.onActivity {
                val before = dialog(it)
                nav(it, id)
                assertSame(original, controller(it))
                assertSame(before, dialog(it))
                assertTrue(dialog(it)!!.isShowing)
                acknowledgedWarning = dialog(it)
                acknowledgedWarning!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            // A new reminder may already be showing after idle sync; observe the acknowledged instance.
            await(scenario) { acknowledgedWarning?.isShowing == false }
            await(scenario) { dialog(it)?.isShowing == true && dialog(it) !== acknowledgedWarning }
            scenario.onActivity {
                assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
                assertEquals(EnvironmentAlertStatus.PENDING, MockEnvironmentAlertDataSource.getAlerts().last().status)
            }
        }
        scenario.onActivity {
            acknowledgedWarning = dialog(it)
            acknowledgedWarning!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        }
        // A new reminder may already be showing after idle sync; observe the acknowledged instance.
        await(scenario) { acknowledgedWarning?.isShowing == false }
        await(scenario) { it.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) is AlertFragment }
        await(scenario) { dialog(it)?.isShowing == true && dialog(it) !== acknowledgedWarning }
        scenario.onActivity {
            assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
            assertEquals(EnvironmentAlertStatus.PENDING, MockEnvironmentAlertDataSource.getAlerts().last().status)
            acknowledgedWarning = dialog(it)
            acknowledgedWarning!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            it.findViewById<android.view.View>(R.id.button_alert_task).performClick()
        }
        // A new reminder may already be showing after idle sync; observe the acknowledged instance.
        await(scenario) { acknowledgedWarning?.isShowing == false }
        await(scenario) { dialog(it)?.isShowing == true && dialog(it) !== acknowledgedWarning }
        scenario.onActivity {
            acknowledgedWarning = dialog(it)
            acknowledgedWarning!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        }
        await(scenario) { it.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView_alert_list)
            ?.adapter is EnvironmentAlertAdapter }
    }

    @Test fun backgroundStopsForegroundResourcesAndResumeRecreationPreservePending() = runScenario { scenario ->
        scenario.onActivity {
            nav(it, R.id.navTask)
            MockEnvironmentDataSource.updateSensors(EnvironmentSensorState(gasOn = true))
        }
        await(scenario) { dialog(it)?.isShowing == true }
        var previous: Any? = null
        scenario.onActivity { previous = controller(it) }
        scenario.moveToState(Lifecycle.State.CREATED)
        instrumentation.runOnMainSync {
            assertNull(field(previous!!, "dialog"))
            assertNull(field(previous!!, "tone"))
            assertEquals(false, field(previous!!, "visible"))
            assertEquals(EnvironmentAlertStatus.PENDING, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
        SystemClock.sleep(1_600L)
        instrumentation.runOnMainSync { assertNull(field(previous!!, "dialog")) }
        scenario.moveToState(Lifecycle.State.RESUMED)
        await(scenario) { dialog(it)?.isShowing == true }
        scenario.recreate()
        await(scenario) { dialog(it)?.isShowing == true }
        scenario.onActivity {
            assertNotSame(previous, controller(it))
            assertNull(field(previous!!, "dialog"))
            assertNull(field(previous!!, "tone"))
            MockEnvironmentDataSource.updateSensors(EnvironmentSensorState())
        }
        await(scenario) { dialog(it) == null }
        scenario.onActivity {
            assertEquals(4, MockEnvironmentAlertDataSource.getAlerts().size)
            assertEquals(EnvironmentAlertStatus.RESOLVED, MockEnvironmentAlertDataSource.getAlerts().last().status)
        }
    }

    @Test fun countdownExpiresOnHomeEscalatesOnceAndBothOffResolvesHistory() = runScenario { scenario ->
        scenario.onActivity {
            MockEnvironmentDataSource.updateSensors(EnvironmentSensorState(gasOn = true, flameDetected = true))
            MockEnvironmentDataSource.setDeadlineForTests(now + 1L)
            now += 2L
        }
        await(scenario) { dialog(it)?.isShowing == true }
        scenario.onActivity {
            assertEquals(EnvironmentAlertType.UNATTENDED_COOKING, MockEnvironmentDataSource.getActiveWarning()!!.type)
            MockEnvironmentDataSource.updateSensors(EnvironmentSensorState(gasOn = true))
            it.refreshEnvironmentWarning()
            assertEquals(EnvironmentAlertType.GAS_LEAK_RISK, MockEnvironmentDataSource.getActiveWarning()!!.type)
            assertEquals(5, MockEnvironmentAlertDataSource.getAlerts().size)
            assertEquals(2, MockEnvironmentAlertDataSource.getAlerts().drop(3).count { row -> row.status == EnvironmentAlertStatus.PENDING })
            nav(it, R.id.navManagement)
            MockEnvironmentDataSource.updateSensors(EnvironmentSensorState())
        }
        await(scenario) { dialog(it) == null }
        scenario.onActivity {
            assertEquals(5, MockEnvironmentAlertDataSource.getAlerts().size)
            assertTrue(MockEnvironmentAlertDataSource.getAlerts().drop(3).all { row -> row.status == EnvironmentAlertStatus.RESOLVED })
        }
    }
}
