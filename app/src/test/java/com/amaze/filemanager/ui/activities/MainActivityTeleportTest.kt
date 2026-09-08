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
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.TabHandler
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.shadows.ShadowSmbUtil
import org.junit.Assert.assertEquals
import org.junit.Assume
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
@Config(shadows = [ShadowSmbUtil::class])
class MainActivityTeleportTest : AbstractMainActivityTestBase() {
    /**
     * TabHandler is a Bill-Pugh singleton whose `database` field is captured once,
     * at class-load time, pointing at whatever ExplorerDatabase / SQLite connection
     * existed in the Application at that moment.
     *
     * Robolectric tears down and rebuilds the Application (and its SQLite connections)
     * between test methods, but this JVM-wide singleton survives across tests in the
     * same run and keeps holding a stale connection -- causing an
     * "Illegal connection pointer" IllegalStateException once a second test launches
     * an Activity that touches TabHandler (via TabFragment.refactorDrawerStorages ->
     * getAllTabs()).
     *
     * Since TabHandler is shared/upstream code, we refresh its internal `database`
     * reference via reflection before every test instead of modifying it, pointing it
     * at the current test's fresh ExplorerDatabase instance. Test-only workaround.
     */
    @Before
    fun refreshTabHandlerDatabaseReference() {
        runCatching {
            val tabHandler = TabHandler.getInstance()
            val databaseField = TabHandler::class.java.getDeclaredField("database")
            databaseField.isAccessible = true

            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
            unsafeField.isAccessible = true
            val unsafe: Any = unsafeField.get(null)

            val objectFieldOffsetMethod =
                unsafeClass.getMethod("objectFieldOffset", java.lang.reflect.Field::class.java)
            val offset = objectFieldOffsetMethod.invoke(unsafe, databaseField) as Long

            val putObjectMethod =
                unsafeClass.getMethod(
                    "putObject",
                    Any::class.java,
                    Long::class.javaPrimitiveType,
                    Any::class.java,
                )
            putObjectMethod.invoke(
                unsafe,
                tabHandler,
                offset,
                AppConfig.getInstance().explorerDatabase,
            )
        }.onFailure {
            println("WARN: failed to refresh TabHandler database reference via reflection: ${it.message}")
        }
    }

    /**
     * Verifies teleporting to a normal local file sets scrollToFileName and navigates
     * to the file's parent directory.
     */
    @Test
    fun testTeleportToLocalFile() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { activity: MainActivity ->
                val file = HybridFile(OpenMode.FILE, "/storage/emulated/0/Documents/report.pdf")

                activity.teleportToFile(file)
                ShadowLooper.idleMainLooper()

                assertEquals("report.pdf", activity.scrollToFileName)
                // Local (java.io.File-based) getParent() uses the host OS separator, which is
                // "\" when this test runs on a Windows dev machine but always "/" on a real
                // Android device. Normalize before comparing so the test is host-independent.
                assertEquals(
                    "/storage/emulated/0/Documents",
                    activity.currentMainFragment?.currentPath?.replace(File.separatorChar, '/'),
                )
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    /**
     * Verifies teleporting to an SMB file sets scrollToFileName and navigates
     * to the file's parent directory, without requiring a live SMB server
     * (getParent/getName resolve purely from the path string).
     */
    @Test
    fun testTeleportToSmbFile() {
        io.reactivex.plugins.RxJavaPlugins.setErrorHandler { }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { activity: MainActivity ->
                val file =
                    HybridFile(OpenMode.SMB, "smb://user:password@1.2.3.4/share/folder/file.pdf")

                activity.teleportToFile(file)
                ShadowLooper.idleMainLooper()

                assertEquals("file.pdf", activity.scrollToFileName)
                assertEquals(
                    "smb://user:password@1.2.3.4/share/folder",
                    activity.currentMainFragment?.currentPath,
                )
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    /**
     * Verifies teleporting to an FTP file sets scrollToFileName and navigates
     * to the file's parent directory, without requiring a live FTP server.
     */
    @Test
    fun testTeleportToFtpFile() {
        Assume.assumeFalse(System.getProperty("os.name").lowercase().contains("win"))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            scenario.onActivity { activity: MainActivity ->
                val file =
                    HybridFile(
                        OpenMode.FTP,
                        "ftp://user:password@127.0.0.1:22222/uploads/document.docx",
                    )
                activity.teleportToFile(file)

                assertEquals("document.docx", activity.scrollToFileName)
                assertEquals(
                    "ftp://user:password@127.0.0.1:22222/uploads",
                    activity.currentMainFragment?.currentPath,
                )
            }

            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    /**
     * Verifies teleporting to a file with no resolvable parent (e.g. a root-level path)
     * falls back gracefully to navigating to the file's own path, without crashing.
     */
    @Test
    fun testTeleportToFileWithNoParentFallsBackGracefully() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            ShadowLooper.idleMainLooper()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { activity: MainActivity ->
                val file = HybridFile(OpenMode.FILE, "/")

                // Should not throw, regardless of whether getParent resolves or not.
                activity.teleportToFile(file)
                ShadowLooper.idleMainLooper()
                assertEquals(null, activity.scrollToFileName)
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }
}
