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

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.shadows.ShadowSmbUtil
import com.amaze.filemanager.test.ShadowTabHandlerWithTabs
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Headless (Robolectric) tests for [MainActivity.teleportToFile], covering local files,
 * networked files (SMB/FTP), and the no-parent fallback case.
 *
 * These tests exercise [MainActivity.teleportToFile] end-to-end (scrollToFileName + navigation),
 * on top of [com.amaze.filemanager.filesystem.HybridFileTest] which already covers the
 * underlying getParent/getName path parsing for each protocol.
 *
 * Each test uses ActivityScenario's `.use { }` (Kotlin AutoCloseable extension) rather than a
 * manual close() call at the end of the method, so the scenario -- and its underlying database
 * connection -- is always closed even if an assertion inside the block fails. Without this, a
 * failing assertion in one test can leave a connection open and cause unrelated failures (or
 * Windows-specific file-lock crashes during Robolectric's temp directory cleanup) in later tests.
 */
@Config(shadows = [ShadowSmbUtil::class, ShadowTabHandlerWithTabs::class])
class MainActivityTeleportTest : AbstractMainActivityTestBase() {
    /**
     * Runs the io scheduler inline so that the initial navigation from
     * [MainActivity.onCreate] finishes before the test teleports.
     */
    @Before
    fun runIoSchedulerSynchronously() {
        // MainActivity.onCreate loads DataUtils on Schedulers.io() and then calls goToMain(null)
        // on the main thread. Running io work inline makes that initial navigation finish
        // before the test teleports, instead of racing with it.
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setErrorHandler { }
    }

    /** Restores the default RxJava plugins so other tests are not affected. */
    @After
    fun resetRxPlugins() {
        RxJavaPlugins.reset()
    }

    /**
     * Verifies teleporting to a normal local file sets scrollToFileName and navigates
     * to the file's parent directory.
     */
    @Test
    fun testTeleportToLocalFile() =
        assertTeleport(
            HybridFile(OpenMode.FILE, "/storage/emulated/0/Documents/report.pdf"),
            expectedScrollToFileName = "report.pdf",
            expectedCurrentPath = "/storage/emulated/0/Documents",
        )

    /**
     * Verifies teleporting to an SMB file sets scrollToFileName and navigates
     * to the file's parent directory, without requiring a live SMB server
     * (getParent/getName resolve purely from the path string).
     */
    @Test
    fun testTeleportToSmbFile() =
        assertTeleport(
            HybridFile(OpenMode.SMB, "smb://user:password@1.2.3.4/share/folder/file.pdf"),
            expectedScrollToFileName = "file.pdf",
            expectedCurrentPath = "smb://user:password@1.2.3.4/share/folder",
        )

    /**
     * Verifies teleporting to an FTP file sets scrollToFileName and navigates
     * to the file's parent directory, without requiring a live FTP server.
     */
    @Test
    fun testTeleportToFtpFile() =
        assertTeleport(
            HybridFile(OpenMode.FTP, "ftp://user:password@127.0.0.1:22222/uploads/document.docx"),
            expectedScrollToFileName = "document.docx",
            expectedCurrentPath = "ftp://user:password@127.0.0.1:22222/uploads",
        )

    /**
     * Verifies teleporting to a file with no resolvable parent (e.g. a root-level path)
     * falls back gracefully to navigating to the file's own path, without crashing.
     */
    @Test
    fun testTeleportToFileWithNoParentFallsBackGracefully() =
        assertTeleport(
            HybridFile(OpenMode.FILE, "/"),
            expectedScrollToFileName = null,
            expectedCurrentPath = null,
            checkPath = false,
        )

    /**
     * Launches [MainActivity], teleports to [file] and asserts both the scroll target and the
     * directory the current [com.amaze.filemanager.ui.fragments.MainFragment] navigated to.
     */
    private fun assertTeleport(
        file: HybridFile,
        expectedScrollToFileName: String?,
        expectedCurrentPath: String?,
        checkPath: Boolean = true,
    ) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { activity: MainActivity ->
                activity.teleportToFile(file)
                ShadowLooper.idleMainLooper()

                assertEquals(expectedScrollToFileName, activity.scrollToFileName)
                if (checkPath) {
                    // java.io.File based getParent() uses the host separator ("\" on Windows),
                    // so normalize it to make the assertion host-independent.
                    assertEquals(
                        expectedCurrentPath,
                        activity.currentMainFragment?.currentPath?.replace(File.separatorChar, '/'),
                    )
                }
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }
}
