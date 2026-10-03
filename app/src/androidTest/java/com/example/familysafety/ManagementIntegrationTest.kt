package com.example.familysafety

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** STEP 6-1：雙語 Dashboard、入口提示及既有五頁導覽。 */
@RunWith(AndroidJUnit4::class)
class ManagementIntegrationTest {
    @Test fun english_dashboardAndNavigation() = verifyDashboard("en", "Management")
    @Test fun traditionalChinese_dashboardAndNavigation() = verifyDashboard("zh-TW", "管理")

    private fun verifyDashboard(locale: String, title: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val previous = AppCompatDelegate.getApplicationLocales()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
                }
                fun awaitScreen(type: Class<*>? = null) {
                    val deadline = SystemClock.uptimeMillis() + 5_000
                    do {
                        instrumentation.waitForIdleSync()
                        var ready = false
                        scenario.onActivity { activity ->
                            ready = activity.resources.configuration.locales[0].language == locale.substringBefore('-') &&
                                (type == null || type.isInstance(activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content)))
                        }
                        if (ready) return
                        SystemClock.sleep(50)
                    } while (SystemClock.uptimeMillis() < deadline)
                    fail("Locale or Fragment did not become ready: $locale / $type")
                }
                awaitScreen()
                onView(withId(R.id.navManagement)).perform(click())
                awaitScreen(ManagementFragment::class.java)
                onView(withId(R.id.textView_management_title)).check(matches(withText(title)))
                val labels = listOf(
                    R.id.textView_management_member_title to R.string.management_member_title,
                    R.id.textView_management_member_description to R.string.management_member_description,
                    R.id.textView_management_task_title to R.string.management_task_title,
                    R.id.textView_management_task_description to R.string.management_task_description,
                    R.id.button_management_member to R.string.management_action_manage,
                    R.id.button_management_task to R.string.management_action_manage,
                )
                labels.forEach { (id, string) -> onView(withId(id)).check(matches(withText(string))) }
                onView(withId(R.id.cardView_management_member)).check(matches(isDisplayed()))
                onView(withId(R.id.cardView_management_task)).check(matches(isDisplayed()))
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content)
                    assertTrue(fragment is ManagementFragment)
                    fun countTitle(view: View): Int =
                        (if (view is TextView && view !is android.widget.Button && view.text.toString() == title) 1 else 0) +
                            (if (view is ViewGroup) (0 until view.childCount).sumOf { countTitle(view.getChildAt(it)) } else 0)
                    assertEquals(1, countTitle(fragment!!.requireView()))
                    val header = Rect()
                    val nav = Rect()
                    val card = Rect()
                    activity.findViewById<View>(R.id.textView_management_title).getGlobalVisibleRect(header)
                    activity.findViewById<View>(R.id.cardView_management_task).getGlobalVisibleRect(card)
                    val navigation = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation)
                    navigation.getGlobalVisibleRect(nav)
                    assertTrue(header.top >= activity.window.decorView.rootWindowInsets.getInsets(android.view.WindowInsets.Type.statusBars()).top)
                    assertTrue(card.bottom <= nav.top)
                    assertEquals(5, navigation.menu.size())
                    assertEquals(R.id.navManagement, navigation.menu.getItem(4).itemId)
                }
                listOf(R.id.button_management_member, R.id.button_management_task).forEach { id ->
                    onView(withId(id)).perform(scrollTo(), click())
                    onView(withId(com.google.android.material.R.id.snackbar_text))
                        .check(matches(withText(R.string.management_in_development)))
                    scenario.onActivity { activity ->
                        assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content) is ManagementFragment)
                    }
                }
                val pages = listOf(
                    R.id.navHome to HomeFragment::class.java,
                    R.id.navEnvironment to EnvironmentFragment::class.java,
                    R.id.navAlert to AlertFragment::class.java,
                    R.id.navTask to TaskFragment::class.java,
                    R.id.navManagement to ManagementFragment::class.java,
                )
                pages.forEach { (id, type) ->
                    onView(withId(id)).perform(click())
                    awaitScreen(type)
                    scenario.onActivity { activity ->
                        assertTrue(type.isInstance(activity.supportFragmentManager.findFragmentById(R.id.fragmentContainerView_main_content)))
                        assertEquals(id, activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView_main_navigation).selectedItemId)
                    }
                }
                onView(withId(R.id.textView_management_title)).check(matches(withText(title)))
            }
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(previous) }
            instrumentation.waitForIdleSync()
        }
    }
}
