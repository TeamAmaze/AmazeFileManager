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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ReturnedValueOnReadFile] data class.
 */
@Suppress("StringLiteralDuplication")
class ReturnedValueOnReadFileTest {
    /**
     * Test default values
     */
    @Test
    fun testDefaultValues() {
        val result = ReturnedValueOnReadFile("content", null, false)
        assertEquals("content", result.fileContents)
        assertNull(result.cachedFile)
        assertFalse(result.fileIsTooLong)
        assertNull(result.fileWindowReader)
        assertEquals(0L, result.totalFileSize)
    }

    /**
     * Test explicit null values for optional parameters
     */
    @Test
    fun testExplicitNullReader() {
        val result = ReturnedValueOnReadFile("content", null, true, null, 0L)
        assertTrue(result.fileIsTooLong)
        assertNull(result.fileWindowReader)
        assertEquals(0L, result.totalFileSize)
    }

    /**
     *
     */
    @Test
    fun testEqualityWithDefaultParams() {
        val a = ReturnedValueOnReadFile("hello", null, false)
        val b = ReturnedValueOnReadFile("hello", null, false, null, 0L)
        assertEquals(a, b)
    }

    /**
     * Test equality with different file contents
     */
    @Test
    fun testEqualityDifferentContent() {
        val a = ReturnedValueOnReadFile("hello", null, false)
        val b = ReturnedValueOnReadFile("world", null, false)
        assertFalse(a == b)
    }

    /**
     * Test the copy method with modified fileIsTooLong and totalFileSize
     */
    @Test
    fun testCopyWithTotalFileSize() {
        val original = ReturnedValueOnReadFile("content", null, false)
        val modified = original.copy(fileIsTooLong = true, totalFileSize = 999L)
        assertTrue(modified.fileIsTooLong)
        assertEquals(999L, modified.totalFileSize)
        assertEquals("content", modified.fileContents)
    }

    /**
     * Test toString().
     *
     * WARNING: return value can be expensive if content is big!
     */
    @Test
    fun testToString() {
        val result = ReturnedValueOnReadFile("hi", null, false)
        val str = result.toString()
        assertTrue(str.contains("fileContents=hi"))
        assertTrue(str.contains("fileIsTooLong=false"))
    }
}
