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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Instrumented (emulator/device) test verifying that [MainActivity.teleportToFile]
 * actually scrolls the target file into view within the real file-list RecyclerView,
 * on top of the headless assertions in [MainActivityTeleportTest].
 *
 * Uses plain Espresso view matching (checking the target file's name is displayed on
 * screen) rather than accessing MainFragment's `listView`/`adapter` fields directly,
 * since those are private -- this keeps the test decoupled from internal implementation
 * details, following the same style as the existing TextEditorActivityEspressoTest.
 */
@LargeTest
class MainActivityTeleportScrollInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var testDir: File
    private lateinit var targetFile: File

    /**
     * Creates a temp directory (inside the app's own external files dir, which needs no
     * runtime permission) with enough files that the target file starts off-screen and a
     * real scroll is required to bring it into view.
     */
    @Before
    fun setUp() {
        // Use the shared storage root (NOT getExternalFilesDir / Android/data), since Amaze
        // treats any path under Android/data specially (Scoped Storage) and prompts for SAF
        // access before browsing there, even for its own app-specific folder. The shared
        // root instead relies on MANAGE_EXTERNAL_STORAGE, which must be pre-granted via:
        //   adb shell appops set <applicationId> MANAGE_EXTERNAL_STORAGE allow
        testDir = File(android.os.Environment.getExternalStorageDirectory(), "AmazeTeleportScrollTest")
        val created = testDir.mkdirs()
        check(created || testDir.isDirectory) {
            "Failed to create test directory at ${testDir.absolutePath} " +
                "(mkdirs() returned $created, exists=${testDir.exists()}, isDirectory=${testDir.isDirectory}). " +
                "Did you run: adb shell appops set <applicationId> MANAGE_EXTERNAL_STORAGE allow ?"
        }

        for (i in 1..40) {
            val file = File(testDir, "file_%02d.txt".format(i))
            check(file.createNewFile() || file.exists()) {
                "Failed to create ${file.absolutePath} - parent exists: ${testDir.exists()}"
            }
        }
        targetFile = File(testDir, "file_40.txt")
    }

    /**
     * Removes the temp test directory after the test finishes.
     */
    @After
    fun tearDown() {
        if (::testDir.isInitialized) {
            testDir.deleteRecursively()
        }
    }

    /**
     * Verifies the target file's row becomes visible on screen after teleportToFile is called.
     */
    @Test
    fun testTeleportScrollsTargetFileIntoView() {
        activityRule.scenario.onActivity { activity ->
            val file = HybridFile(OpenMode.FILE, targetFile.absolutePath)
            activity.teleportToFile(file)
        }

        // Give the async directory load + scroll a moment to complete before asserting.
        Thread.sleep(2000)

        onView(withText(targetFile.name))
            .check(matches(isDisplayed()))
    }
}
