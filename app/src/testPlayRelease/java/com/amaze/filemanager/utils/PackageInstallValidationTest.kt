/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.P
import android.os.storage.StorageManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.test.TestUtils.initializeInternalStorage
import com.amaze.filemanager.ui.activities.MainActivity
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.awaitility.Awaitility.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowSQLiteConnection
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Unit test for [PackageInstallValidation].
 */
@SuppressLint("SdCardPath")
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [KITKAT, P],
    shadows = [ShadowPackageManager::class, ShadowMultiDex::class, ShadowTabHandler::class]
)
class PackageInstallValidationTest {

    companion object {
        private const val GOOD_PACKAGE = "/sdcard/good-package.apk"
        private const val MY_PACKAGE = "/sdcard/my-package.apk"
        private const val INVALID_PACKAGE = "/sdcard/bad-package.apk"
    }

    /**
     * Prepare [ShadowPackageManager].
     */
    @Before
    fun setUp() {
        val packageManager = shadowOf(AppConfig.getInstance().packageManager)
        packageManager.setPackageArchiveInfo(
            GOOD_PACKAGE,
            PackageInfo().also {
                it.packageName = "foo.bar.abc"
            }
        )
        packageManager.setPackageArchiveInfo(
            MY_PACKAGE,
            PackageInfo().also {
                it.packageName = AppConfig.getInstance().packageName
            }
        )
        packageManager.setPackageArchiveInfo(
            INVALID_PACKAGE,
            null
        )
        if (SDK_INT >= N) initializeInternalStorage()
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler {
            Schedulers.trampoline()
        }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler {
            Schedulers.trampoline()
        }
        ShadowSQLiteConnection.reset()
    }

    /**
     * Post test cleanup.
     */
    @After
    fun tearDown() {
        if (SDK_INT >= N) shadowOf(
            ApplicationProvider.getApplicationContext<Context>().getSystemService(
                StorageManager::class.java
            )
        ).resetStorageVolumeList()
    }

    /**
     * If package is good, nothing happens.
     */
    @Test
    fun testGoodPackage() {
        PackageInstallValidation.validatePackageInstallability(
            File(GOOD_PACKAGE)
        )
        assertEquals(2, 1 + 1)
    }

    /**
     * If package name matches us, should throw PackageCannotBeInstalledException.
     */
    @Test(expected = PackageInstallValidation.PackageCannotBeInstalledException::class)
    fun testMyPackage() {
        PackageInstallValidation.validatePackageInstallability(
            File(MY_PACKAGE)
        )
        fail("PackageCannotBeInstalledException not thrown")
    }

    /**
     * If package is bad, IllegalStateException is thrown.
     */
    @Test(expected = IllegalStateException::class)
    fun testInvalidPackage() {
        PackageInstallValidation.validatePackageInstallability(
            File(INVALID_PACKAGE)
        )
        fail("PackageCannotBeInstalledException not thrown")
    }

    /**
     * Test [FileUtils.installApk] success scenario.
     */
    @Test
    fun testInstallApkSuccess() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            FileUtils.installApk(File(GOOD_PACKAGE), activity)
            shadowOf(activity).nextStartedActivityForResult?.run {
                assertNotNull(this)
                assertNotNull(this.intent)
            } ?: fail("Cannot get next started activity for result as Intent")
        }.also {
            scenario.moveToState(Lifecycle.State.DESTROYED)
            scenario.close()
        }
    }

    /**
     * Test [FileUtils.installApk] failure scenario.
     */
    @Test
    fun testInstallApkFailure() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            FileUtils.installApk(File(MY_PACKAGE), activity)
            await().atMost(10, TimeUnit.SECONDS).until {
                ShadowToast.getLatestToast() != null
            }
            assertEquals(
                activity.getString(R.string.error_google_play_cannot_update_myself),
                ShadowToast.getTextOfLatestToast()
            )
        }.also {
            scenario.moveToState(Lifecycle.State.DESTROYED)
            scenario.close()
        }
    }

    /**
     * Test invalid package specified for installation.
     */
    @Test
    fun testInstallApkInvalidPackage() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            FileUtils.installApk(File(INVALID_PACKAGE), activity)
            await().atMost(10, TimeUnit.SECONDS).until {
                ShadowToast.getLatestToast() != null
            }
            assertEquals(
                activity.getString(R.string.error_cannot_get_package_info, INVALID_PACKAGE),
                ShadowToast.getTextOfLatestToast()
            )
        }.also {
            scenario.moveToState(Lifecycle.State.DESTROYED)
            scenario.close()
        }
    }
}
