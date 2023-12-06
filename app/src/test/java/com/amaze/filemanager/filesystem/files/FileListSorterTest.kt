/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.files

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.files.sort.DirSortBy
import com.amaze.filemanager.filesystem.files.sort.SortBy
import com.amaze.filemanager.filesystem.files.sort.SortOrder
import com.amaze.filemanager.filesystem.files.sort.SortType
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * because of test based on mock-up, extension testing isn't tested so, assume all extension is
 * "*{slash}*"
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [Build.VERSION_CODES.KITKAT, Build.VERSION_CODES.P, Build.VERSION_CODES.R]
)
@Suppress("StringLiteralDuplication", "ComplexMethod", "LongMethod", "LargeClass")
class FileListSorterTest {
    /**
     * Purpose: when dirsOnTop is [DirSortBy.DIR_ON_TOP], if file1 is directory && file2 is not directory, result is -1
     *
     * Input: FileListSorter with [DirSortBy.DIR_ON_TOP], [SortBy.NAME] and [SortOrder.ASC]
     * compare(file1,file2) file1 is dir, file2 is not dir
     *
     * Expected: return -1
     */
    @Test
    fun testDir0File1DirAndFile2NoDir() {
        val fileListSorter = FileListSorter(
            DirSortBy.DIR_ON_TOP,
            SortType(SortBy.NAME, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), -1)
    }

    /*
     public LayoutElementParcelable(String title, String path, String permissions,
                                String symlink, String size, long longSize, boolean header,
                                String date, boolean isDirectory, boolean useThumbs, OpenMode openMode)
  */
    /**
     * Purpose: when dirsOnTop is [DirSortBy.DIR_ON_TOP], if file1 is not directory && file2 is directory, result is 1
     *
     * Input: FileListSorter with [DirSortBy.DIR_ON_TOP], [SortBy.NAME] and [SortOrder.ASC]
     * compare(file1,file2) file1 is not dir, file2 is dir
     *
     * Expected: return 1
     */
    @Test
    fun testDir0File1NoDirAndFile2Dir() {
        val fileListSorter = FileListSorter(
            DirSortBy.DIR_ON_TOP,
            SortType(SortBy.NAME, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1.txt",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            true,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), 1)
    }

    /**
     * Purpose: when dirsOnTop is [DirSortBy.FILE_ON_TOP], if file1 is directory && file2 is not directory, result is 1
     *
     * Input: FileListSorter with [DirSortBy.FILE_ON_TOP], [SortBy.NAME] and [SortOrder.ASC]
     * compare(file1,file2) file1 is dir, file2 is not dir
     *
     * Expected: return 1
     */
    @Test
    fun testDir1File1DirAndFile2NoDir() {
        val fileListSorter = FileListSorter(
            DirSortBy.FILE_ON_TOP,
            SortType(SortBy.NAME, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), 1)
    }

    /**
     * Purpose: when dirsOnTop is [DirSortBy.FILE_ON_TOP], if file1 is not directory && file2 is directory, result is -1
     *
     * Input: FileListSorter with [DirSortBy.FILE_ON_TOP], [SortBy.NAME] and [SortOrder.ASC]
     * compare(file1,file2) file1 is not dir, file2 is dir
     *
     * Expected: return -1
     */
    @Test
    fun testDir1File1NoDirAndFile2Dir() {
        val fileListSorter = FileListSorter(
            DirSortBy.FILE_ON_TOP,
            SortType(SortBy.NAME, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1.txt",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            true,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), -1)
    }

    // From here, use dir is not DIR_ON_TOP or FILE_ON_TOP. -> Select dir is NONE_ON_TOP
    /**
     * Purpose: when sort is [SortBy.NAME], if file1 title's length bigger than file2 title's length, result is
     * positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.NAME] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length > file2 title's length
     *
     * Expected: return positive integer
     */
    @Test
    fun testSort0File1TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.NAME, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1.txt",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.greaterThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.NAME], if file1 title's length smaller than file2 title's length, result is
     * negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.NAME] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length < file2 title's length
     *
     * Expected: return negative integer
     */
    @Test
    fun testSort0File2TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.NAME, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.lessThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.NAME], if file1 title's length and file2 title's length are same, result is
     * zero
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.NAME] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length = file2 title's length
     *
     * Expected: return zero
     */
    @Test
    fun testSort0TitleSame() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.NAME, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC.txt",
            "C:\\AmazeFileManager\\ABC",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), 0)
    }

    /**
     * Purpose: when sort is [SortBy.LAST_MODIFIED], if file1 date more recent than file2 date, result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.LAST_MODIFIED] and [SortOrder.ASC]
     * compare(file1,file2) file1 date > file2 date
     *
     * Expected: return positive integer
     */
    @Test
    fun testSort1File1DateLatest() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.LAST_MODIFIED, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.greaterThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.LAST_MODIFIED], if file2 date more recent than file1 date, result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.LAST_MODIFIED] and [SortOrder.ASC]
     * compare(file1,file2) file1 date < file2 date
     *
     * Expected: return negative integer
     */
    @Test
    fun testSort1File2DateLatest() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.LAST_MODIFIED, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.lessThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.LAST_MODIFIED], if file1 date and file2 date are same, result is zero
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.LAST_MODIFIED] and [SortOrder.ASC]
     * compare(file1,file2) file1 date = file2 date
     *
     * Expected: return zero
     */
    @Test
    fun testSort1FileDateSame() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.LAST_MODIFIED, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), 0)
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if two file are not directory && file1 size bigger than file2 size,
     * result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 size > file2 size
     *
     * Expected: return positive integer
     */
    @Test
    fun testSort2NoDirAndFile1SizeBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.greaterThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if two file are not directory && file1 size smaller than file2 size,
     * result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 size < file2 size
     *
     * Expected: return negative integer
     */
    @Test
    fun testSort2NoDirAndFile2SizeBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.lessThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if two file are not directory && file1 size and file2 size are same,
     * result is zero
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 size = file2 size
     *
     * Expected: return zero
     */
    @Test
    fun testSort2NoDirAndFileSizeSame() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), 0)
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if file1 is directory && file1 title's length bigger than file2
     * title's length, result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length > file2 title's length
     *
     * Expected: return positive integer
     */
    @Test
    fun testSort2File1DirAndFile1TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.greaterThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if file1 is directory && file1 title's length smaller than file2
     * title's length, result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length < file2 title's length
     *
     * Expected: return negative integer
     */
    @Test
    fun testSort2File1DirAndFile2TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.lessThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if file2 is directory && file1 title's length bigger than file2
     * title's length, result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length > file2 title's length
     *
     * Expected: return positive integer
     */
    @Test
    fun testSort2File2DirAndFile1TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1.txt",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.greaterThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if file2 is directory && file1 title's length smaller than file2
     * title's length, result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length < file2 title's length
     *
     * Expected: return negative integer
     */
    @Test
    fun testSort2File2DirAndFile2TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.lessThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.SIZE], if file2 is directory && file1 title's length and file2 title's length
     * are same, result is zero
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.SIZE] and [SortOrder.ASC]
     * compare(file1,file2) file1 title's length = file2 title's length
     *
     * Expected: return zero
     */
    @Test
    fun testSort2File2DirAndFileTitleSame() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.SIZE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            true,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), 0)
    }

    /**
     * Purpose: when sort is [SortBy.TYPE], if file1 extension's length and file2 extension's length are same &&
     * file1 title's length bigger than file2 title's length, result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.TYPE] and [SortOrder.ASC]
     * compare(file1,file2) file1 extension's length = file2 extension's length && file1 title's length > file2 title's length
     *
     * Expected: return positive integer
     */
    @Test
    fun testSort3FileExtensionSameAndFile1TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.TYPE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc1.txt",
            "C:\\AmazeFileManager\\abc1",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.greaterThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.TYPE], if file1 extension's length and file2 extension's length are same &&
     * file1 title's length smaller than file2 title's length, result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.TYPE] and [SortOrder.ASC]
     * compare(file1,file2) file1 extension's length = file2 extension's length && file1 title's length < file2 title's length
     *
     * Expected: return negative integer
     */
    @Test
    fun testSort3FileExtensionSameAndFile2TitleBigger() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.TYPE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc2.txt",
            "C:\\AmazeFileManager\\abc2",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertThat(fileListSorter.compare(file1, file2), Matchers.lessThan(0))
    }

    /**
     * Purpose: when sort is [SortBy.TYPE], if file1 extension's length and file2 extension's length are same &&
     * file1 title's length and file2 title's length are same, result is zero
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.TYPE] and [SortOrder.ASC]
     * compare(file1,file2) file1 extension's length = file2 extension's length && file1 title's length = file2 title's length
     *
     * Expected: return zero
     */
    @Test
    fun testSort3FileExtensionSameAndFileTitleSame() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.TYPE, SortOrder.ASC)
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC.txt",
            "C:\\AmazeFileManager\\ABC",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(fileListSorter.compare(file1, file2).toLong(), 0)
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term more than file2, result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" more than file2 title
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile1MoreMatchThanFile2() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABCDE.txt",
            "C:\\AmazeFileManager\\ABCDE",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(-1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term less than file2, result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" less than file2 title
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile1LessMatchThanFile2() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abcdefg.txt",
            "C:\\AmazeFileManager\\abcdefg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC.txt",
            "C:\\AmazeFileManager\\ABC",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2
     * and file1 starts with search term, result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title and file1 starts with "abc"
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile1StartsWithSearchTerm() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.txt",
            "C:\\AmazeFileManager\\abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "XYZ_ABC",
            "C:\\AmazeFileManager\\XYZ_ABC",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(-1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2
     * and file2 starts with search term, result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title and file2 starts with "abc"
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile2StartWithSearchTerm() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "txt-abc",
            "C:\\AmazeFileManager\\txt-abc",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC.txt",
            "C:\\AmazeFileManager\\ABC",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file1 contains the search term as a word (surrounded by
     * separators), result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file1 contains "abc" as word (separated by "-")
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile1HasSearchTermAsWord() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc-efg.txt",
            "C:\\AmazeFileManager\\abc-efg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABCD-FG.txt",
            "C:\\AmazeFileManager\\ABCD-FG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(-1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file2 contains the search term as a word (surrounded by
     * separators), result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file2 contains "abc" as word (separated by "_")
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile2HasSearchTermAsWord() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abcdefg",
            "C:\\AmazeFileManager\\abcdefg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC_EFG",
            "C:\\AmazeFileManager\\ABC_EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file2 contains the search term as a word (surrounded by
     * separators), result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file2 contains "abc" as word (separated by " ")
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithSpaceWordSeparator() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abcdefg",
            "C:\\AmazeFileManager\\abcdefg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC EFG",
            "C:\\AmazeFileManager\\ABC EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file2 contains the search term as a word (surrounded by
     * separators), result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file2 contains "abc" as word (separated by ".")
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithDotWordSeparator() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abcdefg",
            "C:\\AmazeFileManager\\abcdefg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC.EFG",
            "C:\\AmazeFileManager\\ABC.EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term, both contain the search term as a word and file1 date is more recent,
     * result is positive
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc",
     * both contain "abc" as word and file1 date is more recent
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile1MoreRecent() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.efg",
            "C:\\AmazeFileManager\\abc.efg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC_EFG",
            "C:\\AmazeFileManager\\ABC_EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term, both contain the search term as a word and file2 date is more recent,
     * result is negative
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc",
     * both contain "abc" as word and file2 date is more recent
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile2MoreRecent() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.efg",
            "C:\\AmazeFileManager\\abc.efg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1235",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC_EFG",
            "C:\\AmazeFileManager\\ABC_EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(-1, fileListSorter.compare(file1, file2).toLong())
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term, both contain the search term as a word and file2 date is more recent,
     * result is zero
     *
     * Input: FileListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc",
     * both contain "abc" as word and the date of both is the same
     *
     * Expected: return zero
     */
    @Test
    fun testSortByRelevanceWithSameRelevance() {
        val fileListSorter = FileListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "abc.efg",
            "C:\\AmazeFileManager\\abc.efg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            "ABC_EFG",
            "C:\\AmazeFileManager\\ABC_EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            "1234",
            false,
            false,
            OpenMode.UNKNOWN
        )
        Assert.assertEquals(0, fileListSorter.compare(file1, file2).toLong())
    }
}
