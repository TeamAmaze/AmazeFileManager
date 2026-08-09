/*
 * Copyright (C) 2014-2025 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.fragments

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.viewpager2.widget.ViewPager2
import com.amaze.filemanager.R
import com.amaze.filemanager.test.StoragePermissionHelper
import com.amaze.filemanager.ui.activities.MainActivity
import org.awaitility.Awaitility.await
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Tests for [TabFragment] functionality, mainly for
 * https://github.com/TeamAmaze/AmazeFileManager/issues/1555.
 *
 * Note: deprecated methods and classes are used here for best reproducing the issues.
 */
@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class TabFragmentTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Rule
    @JvmField
    val storagePermissionRule: GrantPermissionRule =
        GrantPermissionRule
            .grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Rule
    @JvmField
    val notificationPermissionRule: GrantPermissionRule =
        if (SDK_INT >= TIRAMISU) {
            GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    @Before
    fun grantManageStoragePermission() {
        StoragePermissionHelper.grantManageStoragePermission()
    }

    /**
     * This test saves state while a MainFragment is detached.
     */
    @Test
    fun testFragmentStateSavingDuringDetachment() {
        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Get the TabFragment
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = activityRule.activity
            val tabFragment =
                activity.supportFragmentManager
                    .findFragmentById(R.id.content_frame) as TabFragment

            // Detach fragment through FragmentManager
            activity.supportFragmentManager.beginTransaction().apply {
                tabFragment.fragments.forEach { detach(it) }
                commit()
            }
        }
    }

    /**
     * Check if the fragment state is saved correctly during a configuration change
     * by rotate the screen while swiping between the tabs.
     */
    @SdkSuppress(excludedSdks = [21, 28]) // TODO check why this doesn't work on emulator
    @Test
    fun testFragmentStateSavingDuringConfigChange() {
        withScenario { scenario ->
            // First perform the swipe action
            swipeToItem(scenario, 1)
            // Then force a configuration change by rotating the screen
            rotateScreen(scenario)
            rotateScreen(scenario)
            awaitCurrentItem(scenario, 1)
        }
    }

    /**
     * Check if the fragment state is saved correctly during rapid tab swiping.
     */
    @SdkSuppress(excludedSdks = [21, 28]) // TODO check why this doesn't work on emulator
    @Test
    fun testRapidTabSwitchingAndStateSaving() {
        withScenario { scenario ->
            // Perform rapid tab switches
            repeat(10) {
                swipeToItem(scenario, 1)
                swipeToItem(scenario, 0)
            }

            // Then force a save state by rotating
            rotateScreen(scenario)
            awaitCurrentItem(scenario, 0)
        }
    }

    /**
     * Check if the fragment state is saved correctly when the fragment is detached.
     */
    @SdkSuppress(excludedSdks = [21, 28]) // TODO check why this doesn't work on emulator
    @Test
    fun testFragmentDetachmentAndStateSaving() {
        withScenario { scenario ->
            // First switch to a different tab
            swipeToItem(scenario, 1)
            awaitTabFragment(scenario)

            scenario.onActivity { activity ->
                val tabFragment =
                    activity.supportFragmentManager
                        .findFragmentById(R.id.content_frame) as TabFragment

                // Detach TabFragment through FragmentManager
                activity.supportFragmentManager.beginTransaction().apply {
                    tabFragment.fragments.forEach { detach(it) }
                    commitNow()
                }
            }

            // Force state save through configuration change
            rotateScreen(scenario)
        }
    }

    private fun withScenario(testBody: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitPager(scenario)
            testBody(scenario)
        }
    }

    private fun awaitPager(scenario: ActivityScenario<MainActivity>): ViewPager2 {
        var pager: ViewPager2? = null

        await().atMost(10, TimeUnit.SECONDS).until {
            scenario.onActivity { activity ->
                pager = activity.findViewById(R.id.pager)
            }

            pager != null
        }

        return requireNotNull(pager)
    }

    /**
     * Hack that works like swipeLeft or swipeRight on smaller screens
     */
    private fun swipeHack(
        interpolatorX: Float,
        interpolatorY: Float,
        viewMatcher: Matcher<View>,
    ) {
        /* HACK
         If the View items are contained inside a ScrollView, and the screen's height is not
          enough to show 90% of the ScrollView, an error is thrown. This is a problem for smaller
          screens, to fix this we simply run the swipe "manually".
          See https://stackoverflow.com/a/74361805/3124150
         */

        onView(viewMatcher).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isDisplayed()

                override fun getDescription(): String = "Swipe without checking rect availability"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                    val visibleRect = Rect()
                    view.getGlobalVisibleRect(visibleRect)

                    val endX = visibleRect.left + (visibleRect.right * interpolatorX).toInt()
                    val endY = visibleRect.top + (visibleRect.bottom * interpolatorY).toInt()

                    // Swipe up from the center, at 5ms per step
                    device.swipe(
                        visibleRect.centerX(),
                        visibleRect.centerY(),
                        endX,
                        endY,
                        10,
                    )
                }
            },
        )
    }

    private fun swipeLeftCompat(viewMatcher: Matcher<View>) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (device.displayHeight <= 1280) {
            swipeHack(0.95f, 0.5f, viewMatcher)
        } else {
            onView(viewMatcher).perform(swipeLeft())
        }
    }

    private fun swipeRightCompat(viewMatcher: Matcher<View>) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if (device.displayHeight <= 1280) {
            swipeHack(0.05f, 0.5f, viewMatcher)
        } else {
            onView(viewMatcher).perform(swipeRight())
        }
    }

    private fun awaitTabFragment(scenario: ActivityScenario<MainActivity>): TabFragment {
        var tabFragment: TabFragment? = null

        await().atMost(10, TimeUnit.SECONDS).until {
            runCatching {
                scenario.onActivity { activity ->
                    tabFragment =
                        activity.supportFragmentManager
                            .findFragmentById(R.id.content_frame) as? TabFragment
                }
            }

            tabFragment?.view != null && tabFragment?.fragments?.isNotEmpty() == true
        }

        return requireNotNull(tabFragment)
    }

    // Swipe to the other tab in the ViewPager2.
    // Index 0 is the first tab, index 1 is the second tab.
    private fun swipeToItem(
        scenario: ActivityScenario<MainActivity>,
        index: Int,
    ) {
        awaitPager(scenario)

        when (index) {
            0 -> swipeRightCompat(withId(R.id.pager))
            1 -> swipeLeftCompat(withId(R.id.pager))
            else -> error("Unsupported pager index: $index")
        }

        awaitCurrentItem(scenario, index)
    }

    private fun rotateScreen(scenario: ActivityScenario<MainActivity>) {
        val initialOrientation =
            currentOrientation(scenario).takeIf {
                it == Configuration.ORIENTATION_LANDSCAPE || it == Configuration.ORIENTATION_PORTRAIT
            } ?: Configuration.ORIENTATION_PORTRAIT
        val rotatedRequestedOrientation =
            if (initialOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

        setRequestedOrientation(scenario, rotatedRequestedOrientation)
        awaitOrientation(scenario, orientationForRequest(rotatedRequestedOrientation))

        setRequestedOrientation(scenario, orientationRequestFor(initialOrientation))
        awaitOrientation(scenario, initialOrientation)

        awaitPager(scenario)
        awaitTabFragment(scenario)
    }

    private fun setRequestedOrientation(
        scenario: ActivityScenario<MainActivity>,
        requestedOrientation: Int,
    ) {
        scenario.onActivity { activity ->
            activity.requestedOrientation = requestedOrientation
        }
    }

    private fun currentOrientation(scenario: ActivityScenario<MainActivity>): Int {
        var orientation = Configuration.ORIENTATION_UNDEFINED

        scenario.onActivity { activity ->
            orientation = activity.resources.configuration.orientation
        }

        return orientation
    }

    private fun orientationForRequest(requestedOrientation: Int): Int =
        when (requestedOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> Configuration.ORIENTATION_LANDSCAPE
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> Configuration.ORIENTATION_PORTRAIT
            else -> Configuration.ORIENTATION_UNDEFINED
        }

    private fun orientationRequestFor(orientation: Int): Int =
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

    private fun awaitOrientation(
        scenario: ActivityScenario<MainActivity>,
        expectedOrientation: Int,
    ) {
        await().atMost(10, TimeUnit.SECONDS).until {
            currentOrientation(scenario) == expectedOrientation
        }
    }

    private fun awaitCurrentItem(
        scenario: ActivityScenario<MainActivity>,
        index: Int,
    ) {
        await().pollDelay(50, TimeUnit.MILLISECONDS).atMost(100, TimeUnit.MILLISECONDS).until {
            var currentItem = -1

            runCatching {
                scenario.onActivity { activity ->
                    currentItem = activity.findViewById<ViewPager2>(R.id.pager).currentItem
                }
            }

            currentItem == index
        }
    }
}
