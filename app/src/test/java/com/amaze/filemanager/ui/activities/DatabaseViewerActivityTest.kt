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

package com.amaze.filemanager.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.shadows.ShadowMultiDex
import io.mockk.every
import io.mockk.spyk
import org.awaitility.Awaitility.await
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowStorageManager
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Tests for [DatabaseViewerActivity].
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [M, P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class, ShadowStorageManager::class],
)
class DatabaseViewerActivityTest {
    /**
     * Tests that the activity sanitizes the path in the intent to prevent malicious payloads
     * from being executed.
     */
    @Test
    fun testSanitizePathInIntent() {
        val maliciousPayload = "/sdcard/fake.db; uname -a > /sdcard/system_info.txt; echo"

        val intent = Intent().putExtra("path", maliciousPayload)

        val activity =
            Robolectric.buildActivity(
                DatabaseViewerActivity::class.java,
                intent,
            )
                .create().start().visible().get()

        await().atMost(10, TimeUnit.SECONDS).until {
            activity.isFinishing &&
                ReflectionHelpers.getField<File>(activity, "pathFile") == null
        }
    }

    /**
     * Tests that the activity does not crash when the path in the intent is valid.
     */
    @Test
    fun testValidPathInIntent() {
        // Create a temporary valid database file
        val validDbFile = File.createTempFile("test", ".db")
        validDbFile.deleteOnExit()

        val intent = Intent().putExtra("path", validDbFile.absolutePath)

        val activityController =
            Robolectric.buildActivity(
                DatabaseViewerActivity::class.java,
                intent,
            )
                .create()

        val activity = activityController.get()
        val spyActivity = spyk(activity, recordPrivateCalls = true)

        // Mock the load method to do nothing
        every { spyActivity invoke ("load") withArguments listOf(any<File>()) } returns Unit

        activityController.start().visible()

        await().atMost(5, TimeUnit.SECONDS).until {
            !spyActivity.isFinishing &&
                ReflectionHelpers
                    .getField<File>(spyActivity, "pathFile")?.absolutePath == validDbFile.absolutePath
        }

        // Clean up
        validDbFile.delete()
    }
}
