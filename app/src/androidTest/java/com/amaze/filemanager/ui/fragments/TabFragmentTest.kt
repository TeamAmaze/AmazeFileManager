/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.viewpager2.widget.ViewPager2
import com.amaze.filemanager.R
import com.amaze.filemanager.test.StoragePermissionHelper
import com.amaze.filemanager.ui.activities.MainActivity
import org.awaitility.Awaitility.await
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
        withScenario { scenario ->
            rotateScreen(scenario)

            swipeToItem(scenario, 1)
            awaitTabFragment(scenario)

            scenario.onActivity { activity ->
                val tabFragment =
                    activity.supportFragmentManager
                        .findFragmentById(R.id.content_frame) as TabFragment

                activity.supportFragmentManager.beginTransaction().apply {
                    tabFragment.fragments.firstOrNull { it.isAdded }?.let { detach(it) }
                    commitNow()
                }
            }
        }
    }

    /**
     * Check if the fragment state is saved correctly during a configuration change
     * by rotate the screen while swiping between the tabs.
     */
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
    @Test
    fun testFragmentDetachmentAndStateSaving() {
        withScenario { scenario ->
            swipeToItem(scenario, 1)
            awaitTabFragment(scenario)

            scenario.onActivity { activity ->
                val tabFragment =
                    activity.supportFragmentManager
                        .findFragmentById(R.id.content_frame) as TabFragment

                // Detach TabFragment through FragmentManager
                activity.supportFragmentManager.beginTransaction().apply {
                    tabFragment.fragments.firstOrNull { it.isAdded }?.let { detach(it) }
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
            0 -> onView(withId(R.id.pager)).perform(swipeRight())
            1 -> onView(withId(R.id.pager)).perform(swipeLeft())
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
