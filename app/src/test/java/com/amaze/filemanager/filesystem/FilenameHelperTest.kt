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

import com.amaze.filemanager.filesystem.FilenameHelper.strip
import com.amaze.filemanager.filesystem.FilenameHelper.toOrdinal
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("StringLiteralDuplication")
class FilenameHelperTest {

    /**
     * [FilenameHelper.strip] should remove linux-style increments from a filename.
     */
    @Test
    fun testStripLinuxFilename() {
        assertEquals("foo.txt", strip("foo (copy).txt"))
        assertEquals("foo.txt", strip("foo (another copy).txt"))
        assertEquals("foo.txt", strip("foo (3rd copy).txt"))
        assertEquals("foo.txt", strip("foo (4th copy).txt"))
        assertEquals("foo.txt", strip("foo (5th copy).txt"))
        assertEquals("foo.txt", strip("foo (111th copy).txt"))
    }

    /**
     * [FilenameHelper.strip] should remove linux-style increments from a folder name.
     */
    @Test
    fun testStripLinuxFolderName() {
        assertEquals("foo", strip("foo (copy)"))
        assertEquals("foo", strip("foo (another copy)"))
        assertEquals("foo", strip("foo (3rd copy)"))
        assertEquals("foo", strip("foo (4th copy)"))
        assertEquals("foo", strip("foo (5th copy)"))
        assertEquals("foo", strip("foo (111th copy)"))
    }

    /**
     * [FilenameHelper.strip] should not remove non-incremental numbers from a folder name.
     */
    @Test
    fun testStripLinuxFolderNameWithNumbering() {
        assertEquals("foo 1", strip("foo 1 (copy)"))
        assertEquals("foo 1", strip("foo 1 (another copy)"))
        assertEquals("foo 1", strip("foo 1 (3rd copy)"))
        assertEquals("foo 1", strip("foo 1 (4th copy)"))
        assertEquals("foo 1", strip("foo 1 (5th copy)"))
        assertEquals("foo 1", strip("foo 1 (111th copy)"))
    }

    /**
     * [FilenameHelper.strip] should not remove raw numbers from file names by default.
     */
    @Test
    fun testStripNonStandardShouldNotRemoveNumbersByDefault() {
        assertEquals("foo 1", strip("foo 1"))
        assertEquals("foo 2", strip("foo 2"))
    }

    /**
     * [FilenameHelper.strip] should remove raw numbers from file names when specified on options.
     */
    @Test
    fun testStripNonStandardShouldRemoveNumbersWhenSpecified() {
        assertEquals("foo", strip("foo 1", true))
        assertEquals("foo", strip("foo 2", true))
    }

    /**
     * [FilenameHelper.strip] should remove (incomplete) from a file name.
     */
    @Test
    fun testStripNonStandardShouldStripIncomplete() {
        assertEquals("foo", strip("foo.(incomplete)"))
        assertEquals("foo", strip("foo copy 219.(incomplete)"))
        assertEquals("foo", strip("foo copy 219.(incomplete).(incomplete).(incomplete)"))
        assertEquals("foo", strip("foo.(incomplete).(incomplete)"))
    }

    /**
     * [FilenameHelper.strip] should remove (incomplete) from a file name with extension.
     */
    @Test
    fun testStripNonStandardShouldStripIncompleteWhenWithExtension() {
        assertEquals("foo.txt", strip("foo.(incomplete).txt"))
        assertEquals("foo.txt", strip("foo copy 219.(incomplete).txt"))
        assertEquals("foo.txt", strip("foo copy 219.(incomplete).(incomplete).(incomplete).txt"))
        assertEquals("foo.txt", strip("foo.(incomplete).(incomplete).txt"))
    }

    /**
     * [FilenameHelper.strip] should not remove a non-increment from a file or folder name.
     */
    @Test
    fun testStripNonStandardShouldNotStripNumbersInNonIncrementFilenames() {
        assertEquals("foo [1]", strip("foo [1]"))
        assertEquals("foo [1].txt", strip("foo [1].txt"))
        assertEquals("bar [1]/foo [1].txt", strip("bar [1]/foo [1].txt"))
        assertEquals("bar[1]/foo[1].txt", posix(strip("bar[1]/foo[1].txt")))
    }

    /**
     * [FilenameHelper.strip] should not remove a non-increment from a basename.
     */
    @Test
    fun testStripNonStandardShouldNotStripNonIncrementsFromBasename() {
        assertEquals("foo [1].txt", strip("foo [1].txt"))
    }

    /**
     * [FilenameHelper.strip] should remove mac-OS-style increments from a file name.
     */
    @Test
    fun testDarwinStripMacOsStyleIncrementsFilename() {
        assertEquals("foo.txt", strip("foo copy.txt"))
        assertEquals("foo.txt", strip("foo copy 1.txt"))
        assertEquals("foo.txt", strip("foo copy 2.txt"))
        assertEquals("foo.txt", strip("foo copy 21.txt"))
        assertEquals("foo.txt", strip("foo copy 219 copy 219.txt"))
        assertEquals("foo.txt", strip("foo copy 219 (2).txt"))
    }

    /**
     * [FilenameHelper.strip] should remove mac-OS-style increments from a folder name.
     */
    @Test
    fun testDarwinStripMacOsStyleIncrementsFolderName() {
        assertEquals("foo", strip("foo copy"))
        assertEquals("foo", strip("foo copy 1"))
        assertEquals("foo", strip("foo copy 2"))
        assertEquals("foo", strip("foo copy 21"))
        assertEquals("foo", strip("foo copy 219 copy 219"))
        assertEquals("foo", strip("foo copy 219 (2)"))
        assertEquals("foo", strip("foo Copy"))
        assertEquals("foo", strip("foo Copy 1"))
        assertEquals("foo", strip("foo Copy 2"))
        assertEquals("foo", strip("foo Copy 21"))
        assertEquals("foo", strip("foo Copy 219 copy 219"))
        assertEquals("foo", strip("foo Copy 219 (2)"))
    }

    /**
     * [FilenameHelper.strip] should remove mac-OS-style increments from folder and file name.
     */
    @Test
    fun testDarwinStripMacOsStyleIncrementsFileAndFolderNames() {
        assertEquals("bar/foo.txt", posix(strip("bar copy/foo copy 1.txt")))
        assertEquals("bar/foo.txt", posix(strip("bar copy/foo copy 2.txt")))
        assertEquals("bar/foo.txt", posix(strip("bar copy/foo copy 21.txt")))
        assertEquals("bar/foo.txt", posix(strip("bar copy/foo copy 219 (2).txt")))
        assertEquals("bar/foo.txt", posix(strip("bar copy/foo copy 219 copy 219.txt")))
        assertEquals("bar/foo.txt", posix(strip("bar copy/foo copy.txt")))
    }

    /**
     * [FilenameHelper.strip] should remove mac-OS-style increments from a basename.
     */
    @Test
    fun testDarwinStripMacOsStyleIncrementsBasename() {
        assertEquals("foo.txt", strip("foo copy.txt"))
        assertEquals("foo.txt", strip("foo copy 1.txt"))
        assertEquals("foo.txt", strip("foo copy 2.txt"))
        assertEquals("foo.txt", strip("foo copy 21.txt"))
        assertEquals("foo.txt", strip("foo copy 219 copy 219.txt"))
        assertEquals("foo.txt", strip("foo copy 219 (2).txt"))
    }

    /**
     * [FilenameHelper.strip] should remove mac-OS-style increments from a basename.
     */
    @Test
    fun testDarwinStripMacOsStyleIncrementsBasename2() {
        assertEquals("foo.txt", strip("foo.(incomplete).txt"))
        assertEquals("foo.txt", strip("foo copy 219.(incomplete).txt"))
        assertEquals("foo.txt", strip("foo copy 219.(incomplete).(incomplete).(incomplete).txt"))
        assertEquals("foo.txt", strip("foo.(incomplete).(incomplete).txt"))
    }

    /**
     * [FilenameHelper.strip] should remove windows-style increments from a file name.
     */
    @Test
    fun testWindowsStripIncrementsFromFilename() {
        assertEquals("foo", strip("foo (1)"))
        assertEquals("foo", strip("foo (2)"))
        assertEquals("foo", strip("foo (22)"))
    }

    /**
     * [FilenameHelper.strip] should not remove non-increments.
     */
    @Test
    fun testWindowsStripShouldNotRemoveNonIncrements() {
        assertEquals("foo 1", strip("foo 1"))
        assertEquals("foo (1) 1", strip("foo (1) 1"))
        assertEquals("foo [1]", strip("foo [1]"))
    }

    /**
     * [FilenameHelper.strip] should not remove non-increments.
     */
    @Test
    fun testWindowsStripShouldNotRemoveNonIncrementsEvenRemoveRawNumbersIsTrue() {
        assertEquals("foo", strip("foo 1", true))
        assertEquals("foo", strip("foo (1) 1", true))
        assertEquals("foo [1]", strip("foo [1]", true))
    }

    /**
     * [FilenameHelper.strip] should remove windows-style increments from absolute paths.
     */
    @Test
    fun testWindowsStripIncrementsInWindowsPaths() {
        assertEquals(strip("\\foo (1)"), "\\foo")
        assertEquals(strip("\\foo (2)"), "\\foo")
        assertEquals(strip("\\foo (22)"), "\\foo")
    }

    /**
     * [FilenameHelper.strip] should remove dash-separated windows-style increments.
     */
    @Test
    fun testWindowsStripDashSeparatedWindowsIncrements() {
        assertEquals("foo", strip("foo (3) - Copy"))
        assertEquals("foo", strip("foo (31) - Copy - Copy"))
    }

    /**
     * [FilenameHelper.strip] should remove windows-style increments from a basename.
     */
    @Test
    fun testWindowsStripWindowsIncrementsInBasename() {
        assertEquals("foo.txt", strip("foo (1).txt"))
        assertEquals("foo.txt", strip("foo (2).txt"))
        assertEquals("foo.txt", strip("foo (22).txt"))
        assertEquals("foo.txt", strip("foo copy (22).txt"))
        assertEquals("foo.txt", strip("foo Copy (22).txt"))
    }

    /**
     * Test [FilenameHelper.toOrdinal] for 0.
     */
    @Test
    fun testToOrdinalForZero() {
        assertEquals("0th", toOrdinal(0))
        assertEquals("0th", toOrdinal(-0))
    }

    /**
     * Test [FilenameHelper.toOrdinal] for 1s.
     */
    @Test
    fun testToOrdinalForOnes() {
        assertEquals("1st", toOrdinal(1))
        assertEquals("11th", toOrdinal(11))
        assertEquals("21st", toOrdinal(21))
        assertEquals("31st", toOrdinal(31))
        assertEquals("41st", toOrdinal(41))
        assertEquals("51st", toOrdinal(51))
        assertEquals("61st", toOrdinal(61))
        assertEquals("71st", toOrdinal(71))
        assertEquals("81st", toOrdinal(81))
        assertEquals("91st", toOrdinal(91))
        assertEquals("111th", toOrdinal(111))
        assertEquals("121st", toOrdinal(121))
        assertEquals("211th", toOrdinal(211))
        assertEquals("311th", toOrdinal(311))
        assertEquals("321st", toOrdinal(321))
        assertEquals("10011th", toOrdinal(10011))
        assertEquals("10111th", toOrdinal(10111))
    }

    /**
     * Test [FilenameHelper.toOrdinal] for 2s.
     */
    @Test
    fun testToOrdinalForTwos() {
        assertEquals("2nd", toOrdinal(2))
        assertEquals("12th", toOrdinal(12))
        assertEquals("22nd", toOrdinal(22))
        assertEquals("32nd", toOrdinal(32))
        assertEquals("42nd", toOrdinal(42))
        assertEquals("52nd", toOrdinal(52))
        assertEquals("62nd", toOrdinal(62))
        assertEquals("72nd", toOrdinal(72))
        assertEquals("82nd", toOrdinal(82))
        assertEquals("92nd", toOrdinal(92))
        assertEquals("112th", toOrdinal(112))
        assertEquals("212th", toOrdinal(212))
        assertEquals("1012th", toOrdinal(1012))
        assertEquals("10012th", toOrdinal(10012))
    }

    /**
     * Test [FilenameHelper.toOrdinal] for 3s.
     */
    @Test
    fun testToOrdinalForThrees() {
        assertEquals("3rd", toOrdinal(3))
        assertEquals("13th", toOrdinal(13))
        assertEquals("23rd", toOrdinal(23))
        assertEquals("33rd", toOrdinal(33))
        assertEquals("43rd", toOrdinal(43))
        assertEquals("53rd", toOrdinal(53))
        assertEquals("63rd", toOrdinal(63))
        assertEquals("73rd", toOrdinal(73))
        assertEquals("83rd", toOrdinal(83))
        assertEquals("93rd", toOrdinal(93))
        assertEquals("103rd", toOrdinal(103))
        assertEquals("113th", toOrdinal(113))
        assertEquals("123rd", toOrdinal(123))
        assertEquals("213th", toOrdinal(213))
        assertEquals("1013th", toOrdinal(1013))
        assertEquals("10013th", toOrdinal(10013))
    }

    /**
     * Test [FilenameHelper.toOrdinal] for negative numbers.
     */
    @Test
    fun testToOrdinalsForNegativeNumbers() {
        assertEquals("0th", toOrdinal(-0))
        assertEquals("-1st", toOrdinal(-1))
        assertEquals("-2nd", toOrdinal(-2))
        assertEquals("-3rd", toOrdinal(-3))
    }

    private fun posix(str: String): String = str.replace(Regex("\\\\"), "/")
}
