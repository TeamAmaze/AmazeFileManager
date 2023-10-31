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

package com.amaze.filemanager.filesystem

import org.junit.Test

/**
 * Tests against [FilenameHelper] for its increment capability.
 *
 * @see FilenameHelper.increment
 */
@Suppress("StringLiteralDuplication")
class FilenameHelperLinuxIncrementNameTest : AbstractFilenameHelperIncrementNameTests() {

    override val formatFlag: FilenameFormatFlag
        get() = FilenameFormatFlag.LINUX

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.LINUX] formatting scheme.
     */
    @Test
    fun testLinuxIncrementSimple() {
        val pairs = arrayOf(
            Pair("/test/file.txt", "/test/file (copy).txt"),
            Pair("sub/foo.txt", "sub/foo (copy).txt"),
            Pair("sub/nested/foo.txt", "sub/nested/foo (copy).txt"),
            Pair("/test/afile", "/test/afile (copy)")
        )
        performTest(pairs, true)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.LINUX] formatting scheme, strip
     * before increment and removeRawNumbers set to true.
     */
    @Test
    fun testLinuxIncrementStripExistingNumbersBeforeIncrement() {
        val pairs = arrayOf(
            Pair("/test/file.txt", "/test/file (copy).txt"),
            Pair("/test/file 2.txt", "/test/file (copy).txt"),
            Pair("/test/foo copy.txt", "/test/foo (copy).txt"),
            Pair("/test/one (copy).txt", "/test/one (another copy).txt"),
            Pair("/test/qux 2.txt", "/test/qux (copy).txt"),
            Pair("/test/abc (2) - Copy.txt", "/test/abc (copy).txt"),
            Pair("/test/abc (2) - Copy Copy.txt", "/test/abc (copy).txt"),
            Pair("/test/sub/nested/foo copy.txt", "/test/sub/nested/foo (copy).txt"),
            Pair("/test/sub/nested/foo copy 2.txt", "/test/sub/nested/foo (copy).txt")
        )
        performTest(pairs, strip = true, removeRawNumbers = true)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.LINUX] formatting scheme, strip
     * before increment and removeRawNumbers set to false.
     */
    @Test
    fun testLinuxIncrementNotStripExistingNumbersBeforeIncrement() {
        val pairs = arrayOf(
            Pair("/test/file.txt", "/test/file (copy).txt"),
            Pair("/test/file 2.txt", "/test/file 2 (copy).txt"),
            Pair("/test/foo copy.txt", "/test/foo (copy).txt"),
            Pair("/test/one (copy).txt", "/test/one (another copy).txt"),
            Pair("/test/qux 2.txt", "/test/qux 2 (copy).txt"),
            Pair("/test/abc (2) - Copy.txt", "/test/abc (copy).txt"),
            Pair("/test/abc (2) - Copy Copy.txt", "/test/abc (copy).txt"),
            Pair("/test/sub/nested/foo copy.txt", "/test/sub/nested/foo (copy).txt"),
            Pair("/test/sub/nested/foo copy 2.txt", "/test/sub/nested/foo (copy).txt")
        )
        performTest(pairs, strip = true, removeRawNumbers = false)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.LINUX] formatting scheme and
     * specifying starting numbers.
     */
    @Test
    fun testLinuxIncrementWithSpecifiedNumbers() {
        performTest(
            Pair("/test/file.txt", "/test/file (copy).txt"),
            start = 0
        )
        val pairs = arrayOf(
            Pair(3, "3rd"),
            Pair(4, "4th"),
            Pair(5, "5th"),
            Pair(6, "6th"),
            Pair(7, "7th"),
            Pair(8, "8th"),
            Pair(9, "9th"),
            Pair(10, "10th"),
            Pair(11, "11th"),
            Pair(12, "12th"),
            Pair(13, "13th"),
            Pair(14, "14th"),
            Pair(112, "112th"),
            Pair(1112, "1112th"),
            Pair(22, "22nd"),
            Pair(122, "122nd"),
            Pair(1122, "1122nd"),
            Pair(102, "102nd"),
            Pair(103, "103rd")
        )
        for (pair in pairs) {
            performTest(
                Pair("/test/file.txt", "/test/file (${pair.second} copy).txt"),
                start = pair.first
            )
        }
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.LINUX] formatting scheme and
     * without stripping.
     */
    @Test
    fun testLinuxIncrementStripOff() {
        val pairs = arrayOf(
            Pair("/test/file.txt", "/test/file (copy).txt"),
            Pair("/test/foo 2.txt", "/test/foo 2 (copy).txt"),
            Pair("/test/foo copy.txt", "/test/foo copy (copy).txt")
        )
        performTest(pairs, false)
    }
}
