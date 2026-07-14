/*
 * Copyright (C) 2026 Arjun Thirumani<arjunthirumani@gmail.com> and Contributors.
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

import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.shadows.jcifs.smb.ShadowSmbFile
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.ui.activities.MainActivity
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSQLiteConnection
import org.robolectric.shadows.ShadowStorageManager

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    sdk = [LOLLIPOP],
    shadows = [
        ShadowMultiDex::class,
        ShadowStorageManager::class,
        ShadowPasswordUtil::class,
        ShadowSmbFile::class,
        ShadowTabHandler::class,
    ],
)
class MainFragmentStateTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        ShadowSQLiteConnection.reset()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        ShadowSQLiteConnection.reset()
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }

    private fun MainActivity.firstMainFragment(): MainFragment? = getTabFragment()?.getFragmentAtIndex(0) as? MainFragment

    @Test
    fun testMainFragmentSavesAndRestoresState() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.RESUMED)
        
        scenario.onActivity { activity ->
            val mainFragment = activity.firstMainFragment()
            assertNotNull("MainFragment must be attached", mainFragment)
            val viewModel = mainFragment!!.mainFragmentViewModel
            
            // Set custom state that is different from default
            viewModel.currentPath = "0"
            viewModel.openMode = OpenMode.CUSTOM
        }

        // Trigger recreation (simulates configuration change/process death)
        scenario.recreate()
        ShadowLooper.idleMainLooper()

        scenario.onActivity { activity ->
            val mainFragment = activity.firstMainFragment()
            assertNotNull("MainFragment must be restored", mainFragment)
            val viewModel = mainFragment!!.mainFragmentViewModel
            
            assertEquals("Path should be restored after recreation", "0", viewModel.currentPath)
            assertEquals("OpenMode should be restored after recreation", OpenMode.CUSTOM, viewModel.openMode)
        }
    }
}
