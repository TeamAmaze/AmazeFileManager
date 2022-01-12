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

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

@Suppress("TooManyFunctions", "StringLiteralDuplication")
abstract class AbstractCompressedHelperTaskArchiveTest : AbstractCompressedHelperTaskTest() {

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

    /**
     * Test browse archive top level.
     */
    @Test
    open fun testRoot() {
        val task = createTask("")
        val result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("test-archive", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
    }

    /**
     * Test browse archive sub levels.
     */
    @Test
    open fun testSublevels() {
        var task = createTask("test-archive")
        var result = task.doInBackground()
        assertEquals(5, result.result.size.toLong())
        assertEquals("1", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        assertEquals("2", result.result[1].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[1].date)
        assertEquals("3", result.result[2].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[2].date)
        assertEquals("4", result.result[3].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[3].date)
        assertEquals("a", result.result[4].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[4].date)
        task = createTask("test-archive/1")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("8", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        task = createTask("test-archive/2")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("7", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        task = createTask("test-archive/3")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("6", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        task = createTask("test-archive/4")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("5", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        task = createTask("test-archive/a")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("b", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        task = createTask("test-archive/a/b")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("c", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        task = createTask("test-archive/a/b/c")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("d", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        task = createTask("test-archive/a/b/c/d")
        result = task.doInBackground()
        assertEquals(1, result.result.size.toLong())
        assertEquals("lipsum.bin", result.result[0].name)
        assertEquals(EXPECTED_TIMESTAMP, result.result[0].date)
        // assertEquals(512, result.get(0).size);
    }

    protected abstract fun createTask(relativePath: String): CompressedHelperTask
}
