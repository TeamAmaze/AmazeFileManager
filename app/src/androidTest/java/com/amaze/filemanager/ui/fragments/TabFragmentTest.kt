package com.amaze.filemanager.ui.fragments

import android.content.pm.ActivityInfo
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.amaze.filemanager.R
import com.amaze.filemanager.test.StoragePermissionHelper
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
     * This test causes a rotation to happen while the MainFragment detaches, to check if it
     * fails. This could happen in reality, but should be very rare
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
    @Test
    fun testFragmentStateSavingDuringConfigChange() {
        // First perform the swipe action
        onView(withId(R.id.pager)).perform(swipeLeft())

        // Force a configuration change by rotating the screen
        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        Thread.sleep(1000) // Give time for the rotation to complete
        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /**
     * Check if the fragment state is saved correctly during rapid tab swiping.
     */
    @Test
    fun testRapidTabSwitchingAndStateSaving() {
        // Perform rapid tab switches
        repeat(10) {
            onView(withId(R.id.pager)).perform(swipeLeft())
            Thread.sleep(100) // Small delay to ensure swipe completes
            onView(withId(R.id.pager)).perform(swipeRight())
            Thread.sleep(100) // Small delay to ensure swipe completes
        }

        // Force a save state by rotating
        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    /**
     * Check if the fragment state is saved correctly when the fragment is detached.
     */
    @Test
    fun testFragmentDetachmentAndStateSaving() {
        // First switch to a different tab
        onView(withId(R.id.pager)).perform(swipeLeft())

        // Get the TabFragment
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = activityRule.activity
            val tabFragment =
                activity.supportFragmentManager
                    .findFragmentById(R.id.content_frame) as TabFragment

            // Detach fragment through FragmentManager
            activity.supportFragmentManager.beginTransaction().apply {
                tabFragment.fragments.firstOrNull()?.let { detach(it) }
                commit()
            }
        }

        // Force state save through configuration change
        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
} 
