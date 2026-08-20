/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE_DEFAULT
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_THUMB
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

/**
 * Tests that [MainFragment] detects thumbnail-preference changes that occurred while the fragment
 * was paused, and forces a full list reload on resume.
 *
 * The relevant logic lives in [MainFragment.onPause] / [MainFragment.onResume]:
 * - [MainFragment.onPause] snapshots [PreferencesConstants.PREFERENCE_SHOW_THUMB] and
 *   [PreferencesConstants.PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE].
 * - [MainFragment.onResume] compares the current values with the snapshots and, when they differ,
 *   calls `updateList(true)` which creates a new [MainFragment.loadFilesListTask].
 *
 * The tests live in the *same package* as [MainFragment] so they can access the
 * package-private `loadFilesListTask` field directly (without reflection).
 */
class MainFragmentThumbnailPrefChangeTest : AbstractMainActivityTestBase() {
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    override fun tearDown() {
        super.tearDown()
        scenario?.close()
        scenario = null
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Launches [MainActivity], idles the looper so the fragment is fully set up, and moves to
     * [Lifecycle.State.STARTED] (which triggers [MainFragment.onPause]).
     *
     * Returns the [ActivityScenario] so the caller can continue exercising it.
     */
    private fun launchAndPause(): ActivityScenario<MainActivity> {
        val s = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        s.moveToState(Lifecycle.State.STARTED) // triggers fragment onPause → snapshots prefs
        ShadowLooper.idleMainLooper()
        return s
    }

    // ------------------------------------------------------------------ tests

    /**
     * When [PREFERENCE_SHOW_THUMB] is toggled while the fragment is paused, [MainFragment.onResume]
     * should detect the change and create a new [MainFragment.loadFilesListTask].
     */
    @Test
    fun testShowThumbChangeTriggersReload() {
        scenario = launchAndPause()

        // Record the load task reference before we trigger a reload
        var taskBefore: Any? = null
        scenario!!.onActivity { activity ->
            val fragment = activity.getCurrentMainFragment()
            assertNotNull("getCurrentMainFragment() returned null", fragment)
            taskBefore = fragment!!.loadFilesListTask

            // Flip the showThumbs preference while paused
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val current = prefs.getBoolean(PREFERENCE_SHOW_THUMB, true)
            prefs.edit().putBoolean(PREFERENCE_SHOW_THUMB, !current).commit()
        }

        // Resume: onResume detects the change and calls updateList(true)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        ShadowLooper.idleMainLooper()

        scenario!!.onActivity { activity ->
            val fragment = activity.getCurrentMainFragment()
            assertNotNull(fragment)
            assertNotSame(
                "A new loadFilesListTask should have been created after PREFERENCE_SHOW_THUMB changed",
                taskBefore,
                fragment!!.loadFilesListTask,
            )
        }
    }

    /**
     * When [PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE] changes while the fragment is paused,
     * [MainFragment.onResume] should detect the change and create a new load task.
     */
    @Test
    fun testRemoteThumbMaxSizeChangeTriggersReload() {
        scenario = launchAndPause()

        var taskBefore: Any? = null
        scenario!!.onActivity { activity ->
            val fragment = activity.getCurrentMainFragment()
            assertNotNull(fragment)
            taskBefore = fragment!!.loadFilesListTask

            // Change the remote thumbnail size-cap preference while paused
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val current = prefs.getInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE_DEFAULT)
            // Toggle between 0 (no cap) and 1 (1 MB cap)
            prefs.edit().putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, if (current == 0) 1 else 0).commit()
        }

        scenario!!.moveToState(Lifecycle.State.RESUMED)
        ShadowLooper.idleMainLooper()

        scenario!!.onActivity { activity ->
            val fragment = activity.getCurrentMainFragment()
            assertNotNull(fragment)
            assertNotSame(
                "A new loadFilesListTask should have been created after " +
                    "PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE changed",
                taskBefore,
                fragment!!.loadFilesListTask,
            )
        }
    }

    /**
     * When neither thumbnail preference changes between pause and resume, no reload should occur:
     * [MainFragment.loadFilesListTask] should remain the same instance.
     */
    @Test
    fun testNoPreferenceChangeDoesNotTriggerReload() {
        scenario = launchAndPause()

        var taskBefore: Any? = null
        scenario!!.onActivity { activity ->
            val fragment = activity.getCurrentMainFragment()
            assertNotNull(fragment)
            // Record task, but do NOT change any prefs
            taskBefore = fragment!!.loadFilesListTask
        }

        scenario!!.moveToState(Lifecycle.State.RESUMED)
        ShadowLooper.idleMainLooper()

        scenario!!.onActivity { activity ->
            val fragment = activity.getCurrentMainFragment()
            assertNotNull(fragment)
            assertSame(
                "loadFilesListTask should not change when no thumbnail preferences changed",
                taskBefore,
                fragment!!.loadFilesListTask,
            )
        }
    }
}
