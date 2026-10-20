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

package com.amaze.filemanager.asynchronous.services

import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.shadows.ShadowPausedAsyncTask
import java.io.File

/** Verify rejected directory copies and moves leave no destination artifacts. */
@Suppress("StringLiteralDuplication")
class CopyServiceDirectoryLoopTest : AbstractMainActivityTestBase() {
    /** Keep every source and destination isolated from the device's real files. */
    @get:Rule
    val tempFolder = TemporaryFolder()

    /** Copying a directory into itself must not create a nested namesake. */
    @Test
    fun testCopyIntoSelfLeavesSourceUnchanged() {
        verifyRejectedLoop(move = false, descendant = false)
    }

    /** Moving a directory into itself must neither create a child nor remove the source. */
    @Test
    fun testMoveIntoSelfLeavesSourceUnchanged() {
        verifyRejectedLoop(move = true, descendant = false)
    }

    /** Copying a directory into a descendant must not create a destination before rejection. */
    @Test
    fun testCopyIntoDescendantLeavesSourceUnchanged() {
        verifyRejectedLoop(move = false, descendant = true)
    }

    /** Moving a directory into a descendant must retain the complete original tree. */
    @Test
    fun testMoveIntoDescendantLeavesSourceUnchanged() {
        verifyRejectedLoop(move = true, descendant = true)
    }

    /** A normal directory copy still creates every destination and retains its source. */
    @Test
    fun testDirectoryCopyPreservesContents() {
        verifySuccessfulTransfer(move = false)
    }

    /** A normal directory move still copies every byte before deleting its source. */
    @Test
    fun testDirectoryMovePreservesContents() {
        verifySuccessfulTransfer(move = true)
    }

    /** Exercise the real service entry point, failure broadcast and unchanged filesystem tree. */
    private fun verifyRejectedLoop(
        move: Boolean,
        descendant: Boolean,
    ) {
        val source = createSource()
        val targetDirectory = if (descendant) File(source, "nested") else source
        val unexpectedDestination = File(targetDirectory, source.name)
        val originalPaths = relativePaths(source)
        val originalContents = fileContents(source)
        val broadcasts = runCopyService(source, targetDirectory, move)

        assertFalse("A rejected copy loop must not create a directory", unexpectedDestination.exists())
        assertEquals(originalPaths, relativePaths(source))
        assertContents(source, originalContents)
        val failures = broadcasts.filter { it.hasExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS) }
        assertEquals("The rejected source must be reported once", 1, failures.size)
        val failedSources =
            requireNotNull(
                failures.single().getParcelableArrayListExtra<HybridFileParcelable>(
                    MainActivity.TAG_INTENT_FILTER_FAILED_OPS,
                ),
            )
        assertEquals(listOf(source.path), failedSources.map { it.path })
    }

    /** Verify ordinary directory transfers still complete with the expected source disposition. */
    private fun verifySuccessfulTransfer(move: Boolean) {
        val source = createSource()
        val originalPaths = relativePaths(source)
        val originalContents = fileContents(source)
        val targetDirectory = tempFolder.newFolder("destination")
        val broadcasts = runCopyService(source, targetDirectory, move)
        val destination = File(targetDirectory, source.name)

        assertEquals(originalPaths, relativePaths(destination))
        assertContents(destination, originalContents)
        assertFalse(
            "An ordinary transfer must not report failed operations",
            broadcasts.any { it.hasExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS) },
        )
        if (move) {
            assertFalse("A completed move must remove the source", source.exists())
        } else {
            assertEquals(originalPaths, relativePaths(source))
            assertContents(source, originalContents)
        }
    }

    /** Build a real nested directory containing deterministic synthetic file bytes. */
    private fun createSource(): File =
        tempFolder.newFolder("source").apply {
            File(this, "notes.txt").writeBytes(byteArrayOf(1, 4, 9, 16))
            File(this, "nested").mkdir()
            File(this, "nested/deep.txt").writeBytes(byteArrayOf(2, 3, 5, 7, 11))
            File(this, "empty").mkdir()
        }

    /** Record both files and directories, including empty directories. */
    private fun relativePaths(directory: File): Set<String> = directory.walkTopDown().map { it.relativeTo(directory).path }.toSet()

    /** Snapshot exact file contents before the requested operation. */
    private fun fileContents(directory: File): Map<String, ByteArray> =
        directory.walkTopDown().filter { it.isFile }
            .associate { it.relativeTo(directory).path to it.readBytes() }

    /** Verify source or destination bytes without relying on reported operation success. */
    private fun assertContents(
        directory: File,
        expected: Map<String, ByteArray>,
    ) {
        expected.forEach { (path, bytes) ->
            val file = File(directory, path)
            assertTrue("Expected file $path", file.isFile)
            assertArrayEquals(path, bytes, file.readBytes())
        }
    }

    /** Run CopyService's normal Intent flow with synchronous background scheduling. */
    private fun runCopyService(
        source: File,
        targetDirectory: File,
        move: Boolean,
    ): List<Intent> {
        val application = ApplicationProvider.getApplicationContext<AppConfig>()
        val sourceFile =
            HybridFileParcelable(source.path, "", source.lastModified(), 0, true)
        val intent =
            Intent(application, CopyService::class.java)
                .putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, arrayListOf(sourceFile))
                .putExtra(CopyService.TAG_COPY_TARGET, targetDirectory.path)
                .putExtra(CopyService.TAG_COPY_OPEN_MODE, OpenMode.FILE.ordinal)
                .putExtra(CopyService.TAG_COPY_MOVE, move)
        val previousBroadcasts = shadowOf(application).broadcastIntents.size
        ShadowPausedAsyncTask.overrideExecutor(InlineExecutorService())
        val service = Robolectric.buildService(CopyService::class.java, intent).create()

        try {
            service.startCommand(0, 1)
            shadowOf(Looper.getMainLooper()).idle()
            val broadcasts = shadowOf(application).broadcastIntents.drop(previousBroadcasts)
            assertTrue(
                "CopyService must finish and request a directory refresh",
                broadcasts.any {
                    it.action == MainActivity.KEY_INTENT_LOAD_LIST &&
                        it.getStringExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE) == targetDirectory.path
                },
            )
            return broadcasts
        } finally {
            service.destroy()
        }
    }
}
