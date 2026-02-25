/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.activities.texteditor

import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.P
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.FileWindowReader
import com.amaze.filemanager.shadows.ShadowMultiDex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests for [TextEditorActivityViewModel] windowed mode state and loadWindow logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [LOLLIPOP, P, Build.VERSION_CODES.R],
)
class TextEditorActivityViewModelTest {
    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Default state ────────────────────────────────────────────────

    @Test
    fun testDefaultStateNotWindowed() {
        val vm = TextEditorActivityViewModel()
        assertFalse(vm.isWindowed)
        assertNull(vm.fileWindowReader)
        assertEquals(0L, vm.windowStartByte)
        assertEquals(0L, vm.windowEndByte)
        assertEquals(0L, vm.totalFileSize)
        assertNull(vm.windowContent.value)
    }

    @Test
    fun testDefaultNonWindowedState() {
        val vm = TextEditorActivityViewModel()
        assertNull(vm.original)
        assertNull(vm.cacheFile)
        assertFalse(vm.modified)
        assertEquals(-1, vm.current)
        assertEquals(0, vm.line)
        assertTrue(vm.searchResultIndices.isEmpty())
    }

    // ── Windowed state initialization ────────────────────────────────

    @Test
    fun testInitializeWindowedMode() {
        val vm = TextEditorActivityViewModel()
        val file = createLargeTestFile()
        val reader = FileWindowReader.fromFile(file)

        vm.isWindowed = true
        vm.fileWindowReader = reader
        vm.totalFileSize = file.length()
        vm.windowStartByte = 0L
        vm.windowEndByte = 1000L

        assertTrue(vm.isWindowed)
        assertNotNull(vm.fileWindowReader)
        assertEquals(file.length(), vm.totalFileSize)
        assertEquals(0L, vm.windowStartByte)
        assertEquals(1000L, vm.windowEndByte)

        reader.close()
    }

    // ── loadWindow: forward ──────────────────────────────────────────

    @Test
    fun testLoadWindowForward() =
        runTest(testDispatcher) {
            val vm = createWindowedViewModel()
            val file = createLargeTestFile()
            val reader = FileWindowReader.fromFile(file)

            vm.fileWindowReader = reader
            vm.totalFileSize = file.length()
            vm.windowStartByte = 0L
            vm.windowEndByte = 200L

            // Observe LiveData
            var result: FileWindowReader.WindowResult? = null
            vm.windowContent.observeForever { result = it }

            vm.loadWindow(TextEditorActivityViewModel.Direction.FORWARD)
            advanceUntilIdle()

            assertNotNull(result)
            assertTrue("Window should have shifted forward", vm.windowStartByte > 0L)
            assertTrue(result!!.text.isNotEmpty())

            reader.close()
        }

    // ── loadWindow: backward ─────────────────────────────────────────

    @Test
    fun testLoadWindowBackward() =
        runTest(testDispatcher) {
            val vm = createWindowedViewModel()
            val file = createLargeTestFile()
            val reader = FileWindowReader.fromFile(file)

            vm.fileWindowReader = reader
            vm.totalFileSize = file.length()
            // Start at a mid-file position
            vm.windowStartByte = 500L
            vm.windowEndByte = 700L

            var result: FileWindowReader.WindowResult? = null
            vm.windowContent.observeForever { result = it }

            vm.loadWindow(TextEditorActivityViewModel.Direction.BACKWARD)
            advanceUntilIdle()

            assertNotNull(result)
            assertTrue("Window should have shifted backward", vm.windowStartByte < 500L)
            assertTrue(result!!.text.isNotEmpty())

            reader.close()
        }

    // ── loadWindow: no-op at boundaries ──────────────────────────────

    @Test
    fun testLoadWindowForwardNoOpAtEndOfFile() =
        runTest(testDispatcher) {
            val vm = createWindowedViewModel()
            val file = createLargeTestFile()
            val reader = FileWindowReader.fromFile(file)

            vm.fileWindowReader = reader
            vm.totalFileSize = file.length()
            // Position at the end
            vm.windowStartByte = file.length() - 100
            vm.windowEndByte = file.length()

            var result: FileWindowReader.WindowResult? = null
            vm.windowContent.observeForever { result = it }

            vm.loadWindow(TextEditorActivityViewModel.Direction.FORWARD)
            advanceUntilIdle()

            // Should not have emitted anything (no-op)
            assertNull(result)

            reader.close()
        }

    @Test
    fun testLoadWindowBackwardNoOpAtStartOfFile() =
        runTest(testDispatcher) {
            val vm = createWindowedViewModel()
            val file = createLargeTestFile()
            val reader = FileWindowReader.fromFile(file)

            vm.fileWindowReader = reader
            vm.totalFileSize = file.length()
            vm.windowStartByte = 0L
            vm.windowEndByte = 200L

            var result: FileWindowReader.WindowResult? = null
            vm.windowContent.observeForever { result = it }

            vm.loadWindow(TextEditorActivityViewModel.Direction.BACKWARD)
            advanceUntilIdle()

            // Should not have emitted anything (no-op)
            assertNull(result)

            reader.close()
        }

    // ── loadWindow: no-op when no reader ─────────────────────────────

    @Test
    fun testLoadWindowNoOpWithoutReader() =
        runTest(testDispatcher) {
            val vm = createWindowedViewModel()
            // No fileWindowReader set
            vm.fileWindowReader = null

            var result: FileWindowReader.WindowResult? = null
            vm.windowContent.observeForever { result = it }

            vm.loadWindow(TextEditorActivityViewModel.Direction.FORWARD)
            advanceUntilIdle()

            assertNull(result)
        }

    // ── Window byte offsets updated after load ───────────────────────

    @Test
    fun testWindowByteOffsetsUpdatedAfterLoad() =
        runTest(testDispatcher) {
            val vm = createWindowedViewModel()
            val file = createLargeTestFile()
            val reader = FileWindowReader.fromFile(file)

            vm.fileWindowReader = reader
            vm.totalFileSize = file.length()
            vm.windowStartByte = 0L
            vm.windowEndByte = 400L

            val originalStart = vm.windowStartByte
            val originalEnd = vm.windowEndByte

            var result: FileWindowReader.WindowResult? = null
            vm.windowContent.observeForever { result = it }

            vm.loadWindow(TextEditorActivityViewModel.Direction.FORWARD)
            advanceUntilIdle()

            assertNotNull(result)
            // Offsets should reflect the new window position from the result
            assertEquals(result!!.startByte, vm.windowStartByte)
            assertEquals(result!!.endByte, vm.windowEndByte)
            // Should have shifted
            assertTrue(vm.windowStartByte > originalStart || vm.windowEndByte > originalEnd)

            reader.close()
        }

    // ── onCleared closes reader ──────────────────────────────────────

    @Test
    fun testOnClearedClosesReader() {
        val vm = TextEditorActivityViewModel()
        val file = createSmallTestFile()
        val reader = FileWindowReader.fromFile(file)
        vm.fileWindowReader = reader

        // Trigger onCleared via reflection (it's protected)
        val method =
            TextEditorActivityViewModel::class.java
                .getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(vm)

        // Verify the reader was closed (reading should throw or return empty)
        var threwException = false
        try {
            reader.readWindow(0, 100)
        } catch (e: Exception) {
            threwException = true
        }
        assertTrue("Reader should be closed after onCleared", threwException)
    }

    // ── Direction enum values ────────────────────────────────────────

    @Test
    fun testDirectionEnum() {
        val values = TextEditorActivityViewModel.Direction.values()
        assertEquals(2, values.size)
        assertEquals(TextEditorActivityViewModel.Direction.FORWARD, values[0])
        assertEquals(TextEditorActivityViewModel.Direction.BACKWARD, values[1])
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /** Creates a ViewModel configured for windowed mode with testDispatcher for IO. */
    private fun createWindowedViewModel(): TextEditorActivityViewModel {
        val vm = TextEditorActivityViewModel()
        vm.isWindowed = true
        vm.ioDispatcher = testDispatcher
        return vm
    }

    private fun createLargeTestFile(): File {
        val file = tempFolder.newFile("large.txt")
        val lines = (1..500).map { "Line number $it with some padding content\n" }
        file.writeText(lines.joinToString(""))
        return file
    }

    private fun createSmallTestFile(): File {
        val file = tempFolder.newFile("small.txt")
        file.writeText("Hello\nWorld\n")
        return file
    }
}
