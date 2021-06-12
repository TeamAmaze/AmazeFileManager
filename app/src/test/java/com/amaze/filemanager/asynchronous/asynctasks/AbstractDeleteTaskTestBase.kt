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

package com.amaze.filemanager.asynchronous.asynctasks

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.*
import android.os.Looper
import android.os.storage.StorageManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.shadows.ShadowSmbUtil
import com.amaze.filemanager.test.ShadowCryptUtil
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.test.TestUtils
import com.amaze.filemanager.ui.activities.MainActivity
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSQLiteConnection
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    shadows = [
        ShadowMultiDex::class,
        ShadowSmbUtil::class,
        ShadowTabHandler::class,
        ShadowCryptUtil::class
    ],
    sdk = [JELLY_BEAN, KITKAT, P]
)
abstract class AbstractDeleteTaskTestBase {

    private var ctx: Context? = null

    /**
     * Test case setup.
     *
     * TODO: some even more generic test case base to prevent copy-and-paste?
     */
    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
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

    protected fun doTestDeleteFileOk(file: HybridFileParcelable) {
        val task = DeleteTask(ctx!!)
        val result = task.doInBackground(ArrayList(listOf(file)))
        assertTrue(result.result)
        assertNull(result.exception)

        task.onPostExecute(result)
        shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(ShadowToast.getLatestToast())
        assertEquals(ctx?.getString(R.string.done), ShadowToast.getTextOfLatestToast())
    }

    protected fun doTestDeleteFileAccessDenied(file: HybridFileParcelable) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) TestUtils.initializeInternalStorage()

        ActivityScenario.launch(MainActivity::class.java).also {
            shadowOf(Looper.getMainLooper()).idle()
        }.moveToState(Lifecycle.State.STARTED).onActivity { activity ->

            val task = DeleteTask(ctx!!)
            val result = task.doInBackground(ArrayList(listOf(file)))
            if (result.result != null) {
                assertFalse(result.result)
            } else {
                assertNotNull(result.exception)
            }
            task.onPostExecute(result)
            shadowOf(Looper.getMainLooper()).idle()

            shadowOf(activity).broadcastIntents.run {
                assertTrue(size > 0)
                find {
                    MainActivity.TAG_INTENT_FILTER_GENERAL.equals(it.action)
                }!!.apply {
                    assertEquals(MainActivity.TAG_INTENT_FILTER_GENERAL, action)
                    getParcelableArrayListExtra<HybridFileParcelable>(
                        MainActivity.TAG_INTENT_FILTER_FAILED_OPS
                    )
                        .run {
                            assertTrue(size > 0)
                            assertEquals(file.path, this!![0].path)
                        }
                }
            }
        }.moveToState(Lifecycle.State.DESTROYED).close().run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                shadowOf(ctx?.getSystemService(StorageManager::class.java))
                    .resetStorageVolumeList()
        }
    }
}
