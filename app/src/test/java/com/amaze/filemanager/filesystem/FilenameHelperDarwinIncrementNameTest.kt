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
class FilenameHelperDarwinIncrementNameTest : AbstractFilenameHelperIncrementNameTests() {

    override val formatFlag: FilenameFormatFlag
        get() = FilenameFormatFlag.DARWIN

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.DARWIN] formatting scheme.
     */
    @Test
    fun testDarwinIncrementSimple() {
        val pairs = arrayOf(
            Pair("/test/file.txt", "/test/file copy.txt"),
            Pair("/test/sub/foo.txt", "/test/sub/foo copy.txt"),
            Pair("/test/sub/nested/foo.txt", "/test/sub/nested/foo copy 2.txt"),
            Pair("/test/afile", "/test/afile copy")
        )
        performTest(pairs, true)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.LINUX] formatting scheme, strip
     * before increment and removeRawNumbers set to true.
     */
    @Test
    fun testDarwinIncrementStripExistingRawNumbersBeforeIncrement() {
        val pairs = arrayOf(
            Pair("/test/foo.txt", "/test/foo copy 7.txt"),
            Pair("/test/foo 2.txt", "/test/foo copy 7.txt"),
            Pair("/test/foo copy.txt", "/test/foo copy 7.txt"),
            Pair("/test/qux 2.txt", "/test/qux copy.txt"),
            Pair("/test/abc (2) - Copy.txt", "/test/abc copy.txt"),
            Pair("/test/abc (2) - Copy Copy.txt", "/test/abc copy.txt"),
            Pair("/test/sub/nested/foo copy.txt", "/test/sub/nested/foo copy 2.txt"),
            Pair("/test/sub/nested/foo copy 3.txt", "/test/sub/nested/foo copy 2.txt")
        )
        performTest(pairs, strip = true, removeRawNumbers = true)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.DARWIN] formatting scheme, strip
     * before increment and removeRawNumbers set to false.
     */
    @Test
    fun testDarwinIncrementStripExistingNumbersBeforeIncrement() {
        val pairs = arrayOf(
            Pair("/test/file.txt", "/test/file copy.txt"),
            Pair("/test/foo 2.txt", "/test/foo 2 copy.txt"),
            Pair("/test/foo copy.txt", "/test/foo copy 7.txt"),
            Pair("/test/qux 2.txt", "/test/qux 2 copy.txt"),
            Pair("/test/abc (2) - Copy.txt", "/test/abc copy.txt"),
            Pair("/test/abc (2) - Copy Copy.txt", "/test/abc copy.txt"),
            Pair("/test/sub/nested/foo copy.txt", "/test/sub/nested/foo copy 2.txt"),
            Pair("/test/sub/nested/foo copy 3.txt", "/test/sub/nested/foo copy 2.txt")
        )
        performTest(pairs, strip = true, removeRawNumbers = false)
    }

    /**
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.DARWIN] formatting scheme and
     * start at specified number.
     */
    @Test
    fun testDarwinIncrementStartWithSpecifiedNumber() {
        val pairs = arrayOf(
            Pair("/test/foo copy 7.txt", 1),
            Pair("/test/foo copy 7.txt", 2),
            Pair("/test/foo copy 7.txt", 3),
            Pair("/test/foo copy 7.txt", 4),
            Pair("/test/foo copy 7.txt", 5),
            Pair("/test/foo copy 7.txt", 6),
            Pair("/test/foo copy 7.txt", 7),
            Pair("/test/foo copy 8.txt", 8),
            Pair("/test/foo copy 101.txt", 101)
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
     * Test [FilenameHelper.increment] with [FilenameFormatFlag.DARWIN] formatting scheme and
     * without stripping.
     */
    @Test
    fun testDarwinIncrementStripOff() {
        val pairs = arrayOf(
            Pair("/test/foo.txt", "/test/foo copy 7.txt"),
            Pair("/test/foo 2.txt", "/test/foo 2 copy.txt"),
            Pair("/test/foo copy.txt", "/test/foo copy copy.txt")
        )
        performTest(pairs, false)
    }
}
