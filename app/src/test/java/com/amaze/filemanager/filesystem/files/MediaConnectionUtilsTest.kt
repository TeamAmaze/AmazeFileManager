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

package com.amaze.filemanager.filesystem.files

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.P
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaScannerConnection
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Robolectric unit tests for [MediaConnectionUtils].
 *
 * Changes covered:
 * 1. `scanFile` was renamed to `scanFiles` and now uses `map { it.path }.toTypedArray()`.
 * 2. A new `scanFileByFileSystemPathAndMimeType` overload was added that accepts an optional
 *    mime-type and an optional [MediaScannerConnection.OnScanCompletedListener].
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [M, P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class],
)
class MediaConnectionUtilsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * Setup before tests.
     */
    @Before
    fun setUp() {
        ShadowMediaScannerConnection.reset()
    }

    /**
     * After test clean up.
     */
    @After
    fun tearDown() {
        ShadowMediaScannerConnection.reset()
    }

    /**
     * [MediaConnectionUtils.scanFiles] with a single [HybridFile] must forward exactly that
     * file's path to [MediaScannerConnection.scanFile].
     */
    @Test
    fun testScanFilesWithSingleFileForwardsPath() {
        val path = "/storage/emulated/0/Download/photo.jpg"
        val hybridFiles = arrayOf(HybridFile(OpenMode.FILE, path))

        MediaConnectionUtils.scanFiles(context, hybridFiles)

        val savedPaths = ShadowMediaScannerConnection.getSavedPaths()
        assertNotNull("ShadowMediaScannerConnection#getSavedPaths should not be null", savedPaths)
        assertEquals("Exactly one path should have been submitted for scanning", 1, savedPaths.size)
        assertTrue(
            "The submitted path should match the HybridFile path",
            savedPaths.contains(path),
        )
    }

    /**
     * [MediaConnectionUtils.scanFiles] with multiple [HybridFile] entries must forward ALL
     * their paths — demonstrating the `map { it.path }.toTypedArray()` refactoring still
     * preserves every path.
     */
    @Test
    fun testScanFilesWithMultipleFilesForwardsAllPaths() {
        val paths =
            listOf(
                "/storage/emulated/0/Music/song1.mp3",
                "/storage/emulated/0/Music/song2.flac",
                "/storage/emulated/0/Music/song3.ogg",
            )
        val hybridFiles = paths.map { HybridFile(OpenMode.FILE, it) }.toTypedArray()

        MediaConnectionUtils.scanFiles(context, hybridFiles)

        val savedPaths = ShadowMediaScannerConnection.getSavedPaths()
        assertEquals(
            "All ${paths.size} paths should have been submitted for scanning",
            paths.size,
            savedPaths.size,
        )
        paths.forEach { path ->
            assertTrue("Path $path should be in savedPaths", savedPaths.contains(path))
        }
    }

    /**
     * [MediaConnectionUtils.scanFiles] with an empty array must not submit any path.
     */
    @Test
    fun testScanFilesWithEmptyArraySubmitsNoPaths() {
        MediaConnectionUtils.scanFiles(context, emptyArray())

        assertTrue(
            "No paths should be saved when scanning an empty array",
            ShadowMediaScannerConnection.getSavedPaths().isEmpty(),
        )
    }

    // -------------------------------------------------------------------------
    // scanFileByFileSystemPathAndMimeType (new method)
    // -------------------------------------------------------------------------

    /**
     * Calling [MediaConnectionUtils.scanFileByFileSystemPathAndMimeType] with an explicit
     * mime-type must submit the path AND the mime-type to [MediaScannerConnection.scanFile].
     */
    @Test
    fun testScanFileByFileSystemPathAndMimeTypeWithMimeTypeSubmitsPathAndMimeType() {
        val path = "/storage/emulated/0/Music/ringtone.mp3"
        val mimeType = "audio/mpeg"

        MediaConnectionUtils.scanFileByFileSystemPathAndMimeType(
            context,
            path,
            mimeType,
        )

        val savedPaths = ShadowMediaScannerConnection.getSavedPaths()
        val savedMimeTypes = ShadowMediaScannerConnection.getSavedMimeTypes()

        assertTrue("Path should be submitted to scanner", savedPaths.contains(path))
        assertTrue(
            "Mime type should be submitted to scanner",
            savedMimeTypes.contains(mimeType),
        )
    }

    /**
     * Calling [MediaConnectionUtils.scanFileByFileSystemPathAndMimeType] with a `null`
     * mime-type must still submit the path; no mime-type entry should be recorded.
     */
    @Test
    fun testScanFileByFileSystemPathAndMimeTypeWithNullMimeTypeSubmitsOnlyPath() {
        val path = "/storage/emulated/0/DCIM/photo.png"

        MediaConnectionUtils.scanFileByFileSystemPathAndMimeType(
            context = context,
            path = path,
            mimeType = null,
        )

        val savedPaths = ShadowMediaScannerConnection.getSavedPaths()
        val savedMimeTypes = ShadowMediaScannerConnection.getSavedMimeTypes()

        assertTrue("Path should be submitted to scanner", savedPaths.contains(path))
        assertTrue(
            "No mime-type should be recorded when null is passed",
            savedMimeTypes.isEmpty(),
        )
    }

    /**
     * When a [MediaScannerConnection.OnScanCompletedListener] callback is provided, it must be
     * passed through to [MediaScannerConnection.scanFile].  Robolectric's
     * [ShadowMediaScannerConnection] does not auto-invoke the callback, but the method under
     * test should not swallow it — verified here by checking that providing a callback does not
     * prevent the path from being registered for scanning.
     */
    @Test
    fun testScanFileByFileSystemPathAndMimeTypeWithCallbackRegistersPath() {
        val path = "/storage/emulated/0/Music/notification.ogg"
        val callbackInvoked = AtomicBoolean(false)
        val callback =
            MediaScannerConnection.OnScanCompletedListener { _, _ ->
                callbackInvoked.set(true)
            }

        MediaConnectionUtils.scanFileByFileSystemPathAndMimeType(
            context = context,
            path = path,
            mimeType = "audio/ogg",
            callback = callback,
        )

        val savedPaths = ShadowMediaScannerConnection.getSavedPaths()
        assertTrue(
            "Path should be submitted to scanner even when callback is provided",
            savedPaths.contains(path),
        )
        // Robolectric's shadow does not invoke the completion callback automatically.
        // A separate integration / instrumented test would cover the actual callback execution.
    }

    /**
     * Default parameter: omitting the optional parameters should compile and submit the path.
     * This is the Kotlin-idiomatic call site used by callers that do not care about mime-type
     * or completion notification.
     */
    @Test
    fun testScanFileByFileSystemPathAndMimeTypeDefaultParamsSubmitsPath() {
        val path = "/storage/emulated/0/Music/alarm.mp3"

        // Call using only mandatory parameters — mime-type and callback default to null.
        MediaConnectionUtils.scanFileByFileSystemPathAndMimeType(context, path)

        assertTrue(
            "Path should be submitted even when optional params are omitted",
            ShadowMediaScannerConnection.getSavedPaths().contains(path),
        )
    }
}
