/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.activities

import android.app.AppOpsManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.TestUtils.initializeInternalStorage
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowSQLiteConnection
import org.robolectric.shadows.ShadowStorageManager

/**
 * Tests MainActivity's superclass, PermissionsActivity.
 *
 * Cannot instantiate itself, hence still uses MainActivity to trigger its actions.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [KITKAT, P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class, ShadowStorageManager::class]
)
class PermissionsActivityTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    /**
     * Pre-test setup
     */
    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) initializeInternalStorage()
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        ShadowSQLiteConnection.reset()
    }

    /**
     * Post-test cleanup
     */
    @After
    fun tearDown() {
        scenario.moveToState(Lifecycle.State.DESTROYED)
        scenario.close()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Shadows.shadowOf(
                ApplicationProvider.getApplicationContext<Context>().getSystemService(
                    StorageManager::class.java
                )
            ).resetStorageVolumeList()
        }
    }

    /**
     * Test grant all files access dialog.
     */
    @Test
    @Config(sdk = [R])
    fun testDisplayAllFilesPermissionDialog() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            // Make Environment.isExternalStorageManager() returns false.
            val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
            shadowOf(
                activity.getSystemService(
                    AppOpsManager::class.java
                )
            ).setMode(
                92,
                activity.applicationInfo.uid,
                activity.packageName,
                AppOpsManager.MODE_IGNORED
            )
            activity.requestAllFilesAccess { }
            assertNotNull(ShadowDialog.getLatestDialog())
            ShadowDialog.getLatestDialog().run {
                assertTrue(this is MaterialDialog)
                (this as MaterialDialog).run {
                    assertEquals(
                        activity.getString(com.amaze.filemanager.R.string.grantper),
                        this.titleView.text
                    )
                    assertEquals(
                        activity.getString(
                            com.amaze.filemanager.R.string.grant_all_files_permission
                        ),
                        this.contentView?.text.toString()
                    )
                    this.getActionButton(DialogAction.POSITIVE).run {
                        assertEquals(
                            activity.getString(com.amaze.filemanager.R.string.grant),
                            this.text
                        )
                        performClick()
                    }
                    val intent = shadowApplication.nextStartedActivity
                    assertNotNull(intent)
                    assertEquals(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        intent.action
                    )
                    assertEquals(Uri.parse("package:${activity.packageName}"), intent.data)
                }
            }
        }
    }
}
