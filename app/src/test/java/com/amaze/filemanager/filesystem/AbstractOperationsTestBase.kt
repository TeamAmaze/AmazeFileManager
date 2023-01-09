/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Looper
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.shadows.ShadowSmbUtil
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.test.TestUtils
import com.amaze.filemanager.ui.activities.MainActivity
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowPausedAsyncTask
import org.robolectric.shadows.ShadowSQLiteConnection

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    shadows = [
        ShadowSmbUtil::class,
        ShadowMultiDex::class,
        ShadowTabHandler::class,
        ShadowPasswordUtil::class
    ],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
abstract class AbstractOperationsTestBase {

    private var ctx: Context? = null

    private val blankCallback = object : Operations.ErrorCallBack {
        override fun exists(file: HybridFile?) = Unit
        override fun launchSAF(file: HybridFile?) = Unit
        override fun launchSAF(file: HybridFile?, file1: HybridFile?) = Unit
        override fun done(hFile: HybridFile?, b: Boolean) = Unit
        override fun invalidName(file: HybridFile?) = Unit
    }

    @Rule
    @JvmField
    @RequiresApi(Build.VERSION_CODES.R)
    val allFilesPermissionRule = GrantPermissionRule
        .grant(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

    /**
     * Test case setup.
     *
     * TODO: some even more generic test case base to prevent copy-and-paste?
     */
    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        ShadowPausedAsyncTask.overrideExecutor(InlineExecutorService())
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    /**
     * Close database on test finished.
     */
    @After
    fun tearDown() {
        ShadowSQLiteConnection.reset()
    }

    protected fun testRenameFileAccessDenied(
        fileMode: OpenMode,
        oldFilePath: String,
        newFilePath: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) TestUtils.initializeInternalStorage()

        ActivityScenario.launch(MainActivity::class.java).also {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }.moveToState(Lifecycle.State.STARTED).onActivity { activity ->

            val oldFile = HybridFile(fileMode, oldFilePath)
            val newFile = HybridFile(fileMode, newFilePath)
            Operations.rename(oldFile, newFile, false, activity, blankCallback)
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            Shadows.shadowOf(activity).broadcastIntents.run {
                assertNotNull(this)
                assertTrue(this.size > 0)
                this[0].apply {
                    assertEquals(MainActivity.TAG_INTENT_FILTER_GENERAL, this.action)
                    this
                        .getParcelableArrayListExtra<HybridFileParcelable>(
                            MainActivity.TAG_INTENT_FILTER_FAILED_OPS
                        )
                        .run {
                            assertNotNull(this)
                            this?.let {
                                assertTrue(it.size > 0)
                                assertEquals(oldFilePath, it[0].path)
                            }
                        }
                }
            }
        }.moveToState(Lifecycle.State.DESTROYED).close().run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Shadows.shadowOf(ctx?.getSystemService(StorageManager::class.java))
                    .resetStorageVolumeList()
            }
        }
    }
}
