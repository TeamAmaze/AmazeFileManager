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

package com.amaze.filemanager.asynchronous.asynctasks.compress

import android.os.Environment
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("TooManyFunctions", "StringLiteralDuplication")
abstract class AbstractCompressedHelperCallableArchiveTest : AbstractCompressedHelperCallableTest() {

    private val EXPECTED_TIMESTAMP = ZonedDateTime.of(
        2018,
        5,
        29,
        10,
        38,
        0,
        0,
        ZoneId.of("UTC")
    ).toInstant().toEpochMilli()

    protected abstract val archiveFileName: String

    /**
     * Test browse archive top level.
     */
    @Test
    open fun testRoot() {
        val task = createCallable("")
        val result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("test-archive", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
    }

    /**
     * Test browse archive sub levels.
     */
    @Test
    open fun testSublevels() {
        var task = createCallable("test-archive")
        var result = task.call()
        assertEquals("Thrown from $javaClass.name", 5, result.size.toLong())
        assertEquals("1", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        assertEquals("2", result[1].name)
        assertEquals(EXPECTED_TIMESTAMP, result[1].date)
        assertEquals("3", result[2].name)
        assertEquals(EXPECTED_TIMESTAMP, result[2].date)
        assertEquals("4", result[3].name)
        assertEquals(EXPECTED_TIMESTAMP, result[3].date)
        assertEquals("a", result[4].name)
        assertEquals(EXPECTED_TIMESTAMP, result[4].date)
        task = createCallable("test-archive/1")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("8", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        task = createCallable("test-archive/2")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("7", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        task = createCallable("test-archive/3")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("6", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        task = createCallable("test-archive/4")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("5", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        task = createCallable("test-archive/a")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("b", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        task = createCallable("test-archive/a/b")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("c", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        task = createCallable("test-archive/a/b/c")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("d", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        task = createCallable("test-archive/a/b/c/d")
        result = task.call()
        assertEquals(1, result.size.toLong())
        assertEquals("lipsum.bin", result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result[0].date)
        // assertEquals(512, result.get(0).size);
    }

    protected fun createCallable(relativePath: String): CompressedHelperCallable =
        doCreateCallable(File(Environment.getExternalStorageDirectory(), archiveFileName), relativePath)

    protected abstract fun doCreateCallable(archive: File, relativePath: String): CompressedHelperCallable
}
