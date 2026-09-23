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

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.P
import android.os.storage.StorageManager
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.shadows.jcifs.smb.ShadowSmbFile
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.test.TestUtils
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSQLiteConnection
import org.robolectric.shadows.ShadowStorageManager

/**
 * Base class for all [MainActivity] related tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [LOLLIPOP, P, VERSION_CODES.R],
    shadows = [
        ShadowMultiDex::class,
        ShadowStorageManager::class,
        ShadowPasswordUtil::class,
        ShadowSmbFile::class,
    ],
)
/*
 * Need to make LooperMode PAUSED and flush the main looper before activity can show up.
 * @see {@link LooperMode.Mode.PAUSED}
 * @see {@link <a href="https://stackoverflow.com/questions/55679636/robolectric-throws-fragmentmanager-is-already-executing-transactions">StackOverflow discussion</a>}
 */
@LooperMode(LooperMode.Mode.PAUSED)
abstract class AbstractMainActivityTestBase {
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @NonNull
    @JvmField
    @RequiresApi(Build.VERSION_CODES.R)
    val allFilesPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

    // ActivityScenarioRule launches MainActivity before @Before runs, so initialize storage here.
    private val initializeStorageRule: ExternalResource =
        object : ExternalResource() {
            override fun before() {
                if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                    TestUtils.initializeInternalStorage()
                }
            }
        }

    @get:Rule
    val ruleChain: TestRule =
        RuleChain.outerRule(initializeStorageRule)
            .around(allFilesPermissionRule)
            .around(activityRule)

    /**
     * Setups before test.
     */
    @Before
    open fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        ShadowSQLiteConnection.reset()
    }

    /**
     * Post test cleanups.
     */
    @After
    open fun tearDown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Shadows.shadowOf(
                ApplicationProvider.getApplicationContext<Context>().getSystemService(
                    StorageManager::class.java,
                ),
            ).resetStorageVolumeList()
        }
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }
}
