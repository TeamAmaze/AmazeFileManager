/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.read

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.P
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.ReadTextFileCallable.MAX_FILE_SIZE_CHARS
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException
import com.amaze.filemanager.fileoperations.exceptions.StreamNotFoundException
import com.amaze.filemanager.filesystem.EditableFileAbstraction
import com.amaze.filemanager.filesystem.RandomPathGenerator
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.ui.activities.texteditor.ReturnedValueOnReadFile
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [LOLLIPOP, P, Build.VERSION_CODES.R],
)
class ReadTextFileCallableTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * Test read an empty file with [ReadTextFileCallable]
     */
    @Test
    @Throws(
        ShellNotRunningException::class,
        IOException::class,
        StreamNotFoundException::class,
    )
    fun testReadEmptyFile() {
        val uri = generatePath()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cr = ctx.contentResolver
        val input = ByteArrayInputStream("".toByteArray())
        Shadows.shadowOf(cr).registerInputStream(uri, input)
        val task =
            ReadTextFileCallable(
                cr,
                EditableFileAbstraction(ctx, uri),
                null,
                false,
            )
        val result = task.call()
        Assert.assertEquals(
            result,
            ReturnedValueOnReadFile("", null, false),
        )
    }

    /**
     * Test read an [MAX_FILE_SIZE_CHARS] / 2 char file with [ReadTextFileCallable]
     */
    @Test
    @Throws(
        ShellNotRunningException::class,
        IOException::class,
        StreamNotFoundException::class,
    )
    fun testReadNormalFile() {
        val random = Random(123)
        val letters = ('A'..'Z').toSet() + ('a'..'z').toSet()

        val fileContents = List(MAX_FILE_SIZE_CHARS / 2) { letters.random(random) }.joinToString("")

        val uri = generatePath()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cr = ctx.contentResolver
        val input = ByteArrayInputStream(fileContents.toByteArray())
        Shadows.shadowOf(cr).registerInputStream(uri, input)
        val task =
            ReadTextFileCallable(
                cr,
                EditableFileAbstraction(ctx, uri),
                null,
                false,
            )
        val result = task.call()
        Assert.assertEquals(
            result,
            ReturnedValueOnReadFile(fileContents, null, false),
        )
    }

    /**
     * Test read a [MAX_FILE_SIZE_CHARS] * 2 char file  with [ReadTextFileCallable]
     */
    @Test
    @Throws(
        ShellNotRunningException::class,
        IOException::class,
        StreamNotFoundException::class,
    )
    fun testReadBigFile() {
        val random = Random(123)
        val letters = ('A'..'Z').toSet() + ('a'..'z').toSet()

        val fileContents = List(MAX_FILE_SIZE_CHARS * 2) { letters.random(random) }.joinToString("")

        val uri = generatePath()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cr = ctx.contentResolver
        val input = ByteArrayInputStream(fileContents.toByteArray())
        Shadows.shadowOf(cr).registerInputStream(uri, input)
        val task =
            ReadTextFileCallable(
                cr,
                EditableFileAbstraction(ctx, uri),
                null,
                false,
            )
        val result = task.call()
        Assert.assertEquals(
            result,
            ReturnedValueOnReadFile(fileContents.substring(0, MAX_FILE_SIZE_CHARS), null, true),
        )
    }

    private fun generatePath(): Uri {
        val path = RandomPathGenerator.generateRandomPath(Random(123), 50)
        return Uri.parse("content://com.amaze.filemanager.test/$path/foobar.txt")
    }

    // ── New tests for windowed mode (file:// URI with FileWindowReader) ──

    /**
     * Test that reading a big file via file:// URI produces a FileWindowReader
     * and correct total file size.
     */
    @Test
    fun testReadBigFileViaFileUriCreatesWindowReader() {
        val random = Random(456)
        val letters = ('A'..'Z').toSet() + ('a'..'z').toSet()
        val bigContent = List(MAX_FILE_SIZE_CHARS * 2) { letters.random(random) }.joinToString("")

        val file = tempFolder.newFile("bigfile.txt")
        file.writeText(bigContent)

        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.fromFile(file)
        val task =
            ReadTextFileCallable(
                ctx.contentResolver,
                EditableFileAbstraction(ctx, uri),
                tempFolder.root,
                false,
            )
        val result = task.call()

        Assert.assertTrue("File should be too long", result.fileIsTooLong)
        Assert.assertEquals(
            bigContent.substring(0, MAX_FILE_SIZE_CHARS),
            result.fileContents,
        )
        Assert.assertNotNull("FileWindowReader should be created for file:// URI", result.fileWindowReader)
        Assert.assertEquals(file.length(), result.totalFileSize)

        // Verify the reader works
        val windowResult = result.fileWindowReader!!.readWindow(0, 100)
        Assert.assertTrue(windowResult.text.isNotEmpty())
        Assert.assertTrue(windowResult.isStartOfFile)

        result.fileWindowReader!!.close()
    }

    /**
     * Test that reading a small file via file:// URI does NOT create a FileWindowReader.
     */
    @Test
    fun testReadSmallFileViaFileUriNoWindowReader() {
        val content = "Small file content\n"

        val file = tempFolder.newFile("smallfile.txt")
        file.writeText(content)

        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.fromFile(file)
        val task =
            ReadTextFileCallable(
                ctx.contentResolver,
                EditableFileAbstraction(ctx, uri),
                tempFolder.root,
                false,
            )
        val result = task.call()

        Assert.assertFalse("File should not be too long", result.fileIsTooLong)
        Assert.assertEquals(content, result.fileContents)
        Assert.assertNull("FileWindowReader should be null for small files", result.fileWindowReader)
        Assert.assertEquals(0L, result.totalFileSize)
    }

    /**
     * Test that big file via content:// URI gracefully handles missing seekable descriptor
     * (fileWindowReader remains null).
     */
    @Test
    fun testReadBigFileViaContentUriFallsBackGracefully() {
        val random = Random(789)
        val letters = ('A'..'Z').toSet() + ('a'..'z').toSet()
        val bigContent = List(MAX_FILE_SIZE_CHARS * 2) { letters.random(random) }.joinToString("")

        val uri = generatePath()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cr = ctx.contentResolver
        Shadows.shadowOf(cr).registerInputStream(uri, ByteArrayInputStream(bigContent.toByteArray()))

        val task = ReadTextFileCallable(cr, EditableFileAbstraction(ctx, uri), null, false)
        val result = task.call()

        Assert.assertTrue("File should be too long", result.fileIsTooLong)
        // Content provider shadow doesn't support openFileDescriptor,
        // so FileWindowReader creation should have failed gracefully
        Assert.assertNull(
            "FileWindowReader should be null when content provider doesn't support seek",
            result.fileWindowReader,
        )
        Assert.assertEquals(0L, result.totalFileSize)
    }
}
