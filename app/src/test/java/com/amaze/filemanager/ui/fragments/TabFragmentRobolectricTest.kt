package com.amaze.filemanager.ui.fragments

import androidx.test.core.app.ActivityScenario
import androidx.viewpager2.widget.ViewPager2
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for [TabFragment].
 *
 * JVM generally runs faster than Dalvik on devices, hence tests here are not very meaningful.
 * Created here just for mimicking the Espresso tests.
 */
class TabFragmentRobolectricTest : AbstractMainActivityTestBase() {
    private lateinit var scenario: ActivityScenario<MainActivity>

    /**
     * Launches the [MainActivity] before each test.
     */
    @Before
    override fun setUp() {
        super.setUp()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /**
     * Post test cleanup.
     */
    @After
    override fun tearDown() {
        super.tearDown()
        scenario.close()
    }

    /**
     * Check if the fragment state is saved correctly during a configuration change
     * by rotate the screen while swiping between the tabs.
     */
    @Test
    fun testFragmentStateSavingDuringConfigChange() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.pager)
            // Switch tab
            viewPager.currentItem = 1

            // Trigger configuration change as if the screen was rotated
            activity.recreate()
        }
    }

    /**
     * Check if the fragment state is saved correctly during rapid tab swiping.
     */
    @Test
    fun testRapidTabSwitchingAndStateSaving() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.pager)

            // switch between tabs back and forth
            repeat(10) {
                viewPager.currentItem = 1
                viewPager.currentItem = 0
            }

            // Trigger configuration save
            activity.recreate()
        }
    }

    /**
     * Check if the fragment state is saved correctly when the fragment is detached.
     */
    @Test
    fun testFragmentDetachmentAndStateSaving() {
        scenario.onActivity { activity ->
            val tabFragment =
                activity.supportFragmentManager
                    .findFragmentById(R.id.content_frame) as TabFragment

            // detach fragment from FragmentManager
            activity.supportFragmentManager.beginTransaction().apply {
                tabFragment.fragments.firstOrNull()?.let { detach(it) }
                commit()
            }

            // Trigger configuration save
            activity.recreate()
        }
    }
}
