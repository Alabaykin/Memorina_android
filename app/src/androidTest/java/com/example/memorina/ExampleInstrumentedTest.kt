package com.example.memorina

import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.memorina", appContext.packageName)
    }

    @Test
    fun findsAllPairsAndStartsNewGame() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val cards = mutableListOf<ImageView>()
            scenario.onActivity { activity ->
                val board = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                    .getChildAt(0) as LinearLayout
                cards.addAll(board.children.flatMap { row ->
                    (row as LinearLayout).children.filterIsInstance<ImageView>()
                }.toList())
                assertEquals(16, cards.size)
                assertTrue(cards.none { it.isSelected })
            }
            capture("01-board")

            val first = cards.first()
            val different = cards.first { it.tag != first.tag }
            scenario.onActivity {
                first.performClick()
            }
            SystemClock.sleep(1100L)
            scenario.onActivity {
                assertTrue(first.isSelected)
                different.performClick()
                assertTrue(different.isSelected)
            }
            capture("02-mismatch")
            await(scenario) { !first.isSelected && !different.isSelected }

            val pairs = cards.groupBy { it.tag }.values
            assertEquals(8, pairs.size)
            for (pair in pairs) {
                assertEquals(2, pair.size)
                scenario.onActivity { pair.forEach { it.performClick() } }
                await(scenario) { pair.all { it.visibility == View.INVISIBLE } }
                if (pair == pairs.first()) capture("03-match")
            }

            onView(withText(R.string.game_won_title)).check(matches(isDisplayed()))
            capture("04-victory")
            onView(withText(R.string.play_again)).perform(click())
            scenario.onActivity { activity ->
                val board = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                    .getChildAt(0) as LinearLayout
                assertTrue(board.children.all { row ->
                    (row as LinearLayout).children.all { it.visibility == View.VISIBLE }
                })
            }
        }
    }

    private fun await(scenario: ActivityScenario<MainActivity>, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 3000L
        do {
            var ready = false
            scenario.onActivity { ready = condition() }
            if (ready) return
            SystemClock.sleep(25L)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("Cards did not update before the timeout")
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: return
        instrumentation.targetContext.openFileOutput("$name.png", 0).use { output ->
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
        }
        screenshot.recycle()
    }
}
