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
class FilenameHelperWindowsIncrementNameTest : AbstractFilenameHelperIncrementNameTests() {

    override val formatFlag: FilenameFormatFlag
        get() = FilenameFormatFlag.WINDOWS

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.WINDOWS] formatting scheme.
     */
    @Test
    fun testWindowsIncrementSimple() {
        val pairs = arrayOf(
            Pair("/test/file.txt", "/test/file (1).txt"),
            Pair("sub/foo.txt", "sub/foo (1).txt"),
            Pair("sub/nested/foo.txt", "sub/nested/foo (1).txt"),
            Pair("/test/afile", "/test/afile (1)")
        )
        performTest(pairs, true)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.WINDOWS] formatting scheme, strip
     * before increment and removeRawNumbers set to true.
     */
    @Test
    fun testWindowsStripRawNumbersAndIncrementsBeforeUpdatingIncrement() {
        val pairs = arrayOf(
            Pair("/test/foo.txt", "/test/foo (1).txt"),
            Pair("/test/foo 2.txt", "/test/foo (1).txt"),
            Pair("/test/foo copy.txt", "/test/foo (1).txt"),
            Pair("/test/qux 2.txt", "/test/qux (1).txt"),
            Pair("/test/abc (2) - Copy.txt", "/test/abc (1).txt"),
            Pair("/test/abc (2) - Copy Copy.txt", "/test/abc (1).txt"),
            Pair("/test/sub/nested/foo copy.txt", "/test/sub/nested/foo (1).txt"),
            Pair("/test/sub/nested/foo copy 2.txt", "/test/sub/nested/foo (1).txt")
        )
        performTest(pairs, strip = true, removeRawNumbers = true)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.WINDOWS] formatting scheme, strip
     * before increment and removeRawNumbers set to false.
     */
    @Test
    fun testWindowsStripBeforeUpdatingIncrement() {
        val pairs = arrayOf(
            Pair("/test/foo.txt", "/test/foo (1).txt"),
            Pair("/test/foo 2.txt", "/test/foo 2 (1).txt"),
            Pair("/test/foo copy.txt", "/test/foo (1).txt"),
            Pair("/test/qux 2.txt", "/test/qux 2 (1).txt"),
            Pair("/test/abc (2) - Copy.txt", "/test/abc (1).txt"),
            Pair("/test/abc (2) - Copy Copy.txt", "/test/abc (1).txt"),
            Pair("/test/sub/nested/foo copy.txt", "/test/sub/nested/foo (1).txt"),
            Pair("/test/sub/nested/foo copy 2.txt", "/test/sub/nested/foo (1).txt")
        )
        performTest(pairs, strip = true, removeRawNumbers = false)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.WINDOWS] formatting scheme and
     * start at specified number.
     */
    @Test
    fun testWindowsIncrementStartWithSpecifiedNumber() {
        val pairs = arrayOf(
            Pair("/test/foo (1).txt", 1),
            Pair("/test/foo (3).txt", 2),
            Pair("/test/foo (3).txt", 3),
            Pair("/test/foo (4).txt", 4),
            Pair("/test/foo (5).txt", 5),
            Pair("/test/foo (6).txt", 6),
            Pair("/test/foo (7).txt", 7),
            Pair("/test/foo (101).txt", 101),
            Pair("/test/foo (102).txt", 102)
        )
        for (pair in pairs) {
            performTest(
                Pair("/test/foo.txt", pair.first),
                true,
                start = pair.second
            )
        }
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.WINDOWS] formatting scheme and
     * without stripping.
     */
    @Test
    fun testWindowsIncrementStripOff() {
        val pairs = arrayOf(
            Pair("/test/foo.txt", "/test/foo (1).txt"),
            Pair("/test/foo 2.txt", "/test/foo 2 (1).txt"),
            Pair("/test/foo copy.txt", "/test/foo copy (1).txt")
        )
        performTest(pairs, false)
    }
}
