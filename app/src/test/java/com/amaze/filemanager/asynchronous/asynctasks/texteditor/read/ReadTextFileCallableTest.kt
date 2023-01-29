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
import android.os.Build.VERSION_CODES.KITKAT
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
class ReadTextFileCallableTest {

    /**
     * Test read an empty file with [ReadTextFileCallable]
     */
    @Test
    @Throws(
        ShellNotRunningException::class,
        IOException::class,
        StreamNotFoundException::class
    )
    fun testReadEmptyFile() {
        val uri = generatePath()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cr = ctx.contentResolver
        val input = ByteArrayInputStream("".toByteArray())
        Shadows.shadowOf(cr).registerInputStream(uri, input)
        val task = ReadTextFileCallable(
            cr,
            EditableFileAbstraction(ctx, uri),
            null,
            false
        )
        val result = task.call()
        Assert.assertEquals(
            result,
            ReturnedValueOnReadFile("", null, false)
        )
    }

    /**
     * Test read an [MAX_FILE_SIZE_CHARS] / 2 char file with [ReadTextFileCallable]
     */
    @Test
    @Throws(
        ShellNotRunningException::class,
        IOException::class,
        StreamNotFoundException::class
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
        val task = ReadTextFileCallable(
            cr,
            EditableFileAbstraction(ctx, uri),
            null,
            false
        )
        val result = task.call()
        Assert.assertEquals(
            result,
            ReturnedValueOnReadFile(fileContents, null, false)
        )
    }

    /**
     * Test read a [MAX_FILE_SIZE_CHARS] * 2 char file  with [ReadTextFileCallable]
     */
    @Test
    @Throws(
        ShellNotRunningException::class,
        IOException::class,
        StreamNotFoundException::class
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
        val task = ReadTextFileCallable(
            cr,
            EditableFileAbstraction(ctx, uri),
            null,
            false
        )
        val result = task.call()
        Assert.assertEquals(
            result,
            ReturnedValueOnReadFile(fileContents.substring(0, MAX_FILE_SIZE_CHARS), null, true)
        )
    }

    private fun generatePath(): Uri {
        val path = RandomPathGenerator.generateRandomPath(Random(123), 50)
        return Uri.parse("content://com.amaze.filemanager.test/$path/foobar.txt")
    }
}
