/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test [SevenZipHelperCallable] internals
 */
class SevenZipHelperCallableInternalTest {

    /**
     * Test simple scenario of consolidate()
     */
    @Test
    fun testConsolidateSimple() {
        val callable = SevenZipHelperCallable("dummy", "", false)
        val list = listOf("1/2/3/4/test.jpg", "test.jpg", "testdir/test.jpg")
        var result = callable.consolidate(list)
        assertEquals(3, result.size)
        assertTrue(result.contains("1"))
        assertTrue(result.contains("testdir"))
        assertTrue(result.contains("test.jpg"))
        result = callable.consolidate(list, 1)
        assertEquals(2, result.size)
        assertTrue(result.contains("1/2"))
        assertTrue(result.contains("testdir/test.jpg"))
        result = callable.consolidate(list, 2)
        assertEquals(1, result.size)
        assertTrue(result.contains("1/2/3"))
    }

    /**
     * Test classic scenario of consolidate(), i.e. our classic directory structure
     */
    @Test
    fun testConsolidateClassic() {
        val callable = SevenZipHelperCallable("dummy", "", false)
        val list = listOf(
            "test-archive",
            "test-archive/1",
            "test-archive/2",
            "test-archive/3",
            "test-archive/4",
            "test-archive/a",
            "test-archive/a/b",
            "test-archive/a/b/c",
            "test-archive/a/b/c/d",
            "test-archive/1/8",
            "test-archive/2/7",
            "test-archive/3/6",
            "test-archive/4/5",
            "test-archive/a/b/c/d/lipsum.bin"
        )
        var result = callable.consolidate(list)
        assertEquals(1, result.size)
        assertEquals("test-archive", result.first())
        result = callable.consolidate(list, 1)
        assertEquals(5, result.size)
        assertTrue(result.contains("test-archive/1"))
        assertTrue(result.contains("test-archive/2"))
        assertTrue(result.contains("test-archive/3"))
        assertTrue(result.contains("test-archive/4"))
        assertTrue(result.contains("test-archive/a"))
        result = callable.consolidate(list, 2)
        assertEquals(5, result.size)
        assertTrue(result.contains("test-archive/a/b"))
        assertTrue(result.contains("test-archive/1/8"))
        assertTrue(result.contains("test-archive/2/7"))
        assertTrue(result.contains("test-archive/3/6"))
        assertTrue(result.contains("test-archive/4/5"))
        result = callable.consolidate(list, 3)
        assertEquals(1, result.size)
        assertTrue(result.contains("test-archive/a/b/c"))
        result = callable.consolidate(list, 4)
        assertEquals(1, result.size)
        assertTrue(result.contains("test-archive/a/b/c/d"))
        result = callable.consolidate(list, 5)
        assertEquals(1, result.size)
        assertTrue(result.contains("test-archive/a/b/c/d/lipsum.bin"))
    }
}
