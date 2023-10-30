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
     * Purpose: when dirsOnTop is 0, if file1 is directory && file2 is not directory, result is -1
     * Input: FileListSorter(0,0,1) dir(=dirsOnTop) is 0 / compare(file1,file2) file1 is dir, file2 is
     * not dir Expected: return -1
     */
    @Test
    fun testDir0File1DirAndFile2NoDir() {
        val fileListSorter = FileListSorter(0, 0, 1)
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
     * Purpose: when dirsOnTop is 0, if file1 is not directory && file2 is directory, result is 1
     * Input: FileListSorter(0,0,1) dir(=dirsOnTop) is 0 / compare(file1,file2) file1 is not dir,
     * file2 is dir Expected: return 1
     */
    @Test
    fun testDir0File1NoDirAndFile2Dir() {
        val fileListSorter = FileListSorter(0, 0, 1)
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
     * Purpose: when dirsOnTop is 1, if file1 is directory && file2 is not directory, result is 1
     * Input: FileListSorter(1,0,1) dir(=dirsOnTop) is 1 / compare(file1,file2) file1 is dir, file2 is
     * not dir Expected: return 1
     */
    @Test
    fun testDir1File1DirAndFile2NoDir() {
        val fileListSorter = FileListSorter(1, 0, 1)
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
     * Purpose: when dirsOnTop is 1, if file1 is not directory && file2 is directory, result is -1
     * Input: FileListSorter(1,0,1) dir(=dirsOnTop) is 1 / compare(file1,file2) file1 is not dir,
     * file2 is dir Expected: return -1
     */
    @Test
    fun testDir1File1NoDirAndFile2Dir() {
        val fileListSorter = FileListSorter(1, 0, 1)
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

    // From here, use dir is not 0 or 1. -> Select dir is -1
    /**
     * Purpose: when sort is 0, if file1 title's length bigger than file2 title's length, result is
     * positive Input: FileListSorter(-1,0,1) sort is 0 / compare(file1,file2) file1 title's length >
     * file2 title's length Expected: return positive integer
     */
    @Test
    fun testSort0File1TitleBigger() {
        val fileListSorter = FileListSorter(-1, 0, 1)
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
     * Purpose: when sort is 0, if file1 title's length smaller than file2 title's length, result is
     * negative Input: FileListSorter(-1,0,1) sort is 0 / compare(file1,file2) file1 title's length <
     * file2 title's length Expected: return negative integer
     */
    @Test
    fun testSort0File2TitleBigger() {
        val fileListSorter = FileListSorter(-1, 0, 1)
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
     * Purpose: when sort is 0, if file1 title's length and file2 title's length are same, result is
     * zero Input: FileListSorter(-1,0,1) sort is 0 / compare(file1,file2) file1 title's length =
     * file2 title's length Expected: return zero
     */
    @Test
    fun testSort0TitleSame() {
        val fileListSorter = FileListSorter(-1, 0, 1)
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
     * Purpose: when sort is 1, if file1 date more recent than file2 date, result is positive Input:
     * FileListSorter(-1,1,1) sort is 1 / compare(file1,file2) file1 date > file2 date Expected:
     * return positive integer
     */
    @Test
    fun testSort1File1DateLastest() {
        val fileListSorter = FileListSorter(-1, 1, 1)
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
     * Purpose: when sort is 1, if file2 date more recent than file1 date, result is negative Input:
     * FileListSorter(-1,1,1) sort is 1 / compare(file1,file2) file1 date < file2 date Expected:
     * return negative integer
     */
    @Test
    fun testSort1File2DateLastest() {
        val fileListSorter = FileListSorter(-1, 1, 1)
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
     * Purpose: when sort is 1, if file1 date and file2 date are same, result is zero Input:
     * FileListSorter(-1,1,1) sort is 1 / compare(file1,file2) file1 date = file2 date Expected:
     * return zero
     */
    @Test
    fun testSort1FileDateSame() {
        val fileListSorter = FileListSorter(-1, 1, 1)
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
     * Purpose: when sort is 2, if two file are not directory && file1 size bigger than file2 size,
     * result is positive Input: FileListSorter(-1,2,1) sort is 2 / compare(file1,file2) file1 size >
     * file2 size Expected: return positive integer
     */
    @Test
    fun testSort2NoDirAndFile1SizeBigger() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 2, if two file are not directory && file1 size smaller than file2 size,
     * result is negative Input: FileListSorter(-1,2,1) sort is 2 / compare(file1,file2) file1 size <
     * file2 size Expected: return negative integer
     */
    @Test
    fun testSort2NoDirAndFile2SizeBigger() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 2, if two file are not directory && file1 size and file2 size are same,
     * result is zero Input: FileListSorter(-1,2,1) sort is 2 / compare(file1,file2) file1 size =
     * file2 size Expected: return zero
     */
    @Test
    fun testSort2NoDirAndFileSizeSame() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 2, if file1 is directory && file1 title's length bigger than file2
     * title's length, result is positive Input: FileListSorter(-1,2,1) sort is 2 /
     * compare(file1,file2) file1 title's length > file2 title's length Expected: return positive
     * integer
     */
    @Test
    fun testSort2File1DirAndFile1TitleBigger() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 2, if file1 is directory && file1 title's length smaller than file2
     * title's length, result is negative Input: FileListSorter(-1,2,1) sort is 2 /
     * compare(file1,file2) file1 title's length < file2 title's length Expected: return negative
     * integer
     */
    @Test
    fun testSort2File1DirAndFile2TitleBigger() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 2, if file2 is directory && file1 title's length bigger than file2
     * title's length, result is positive Input: FileListSorter(-1,2,1) sort is 2 /
     * compare(file1,file2) file1 title's length > file2 title's length Expected: return positive
     * integer
     */
    @Test
    fun testSort2File2DirAndFile1TitleBigger() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 2, if file2 is directory && file1 title's length smaller than file2
     * title's length, result is negative Input: FileListSorter(-1,2,1) sort is 2 /
     * compare(file1,file2) file1 title's length < file2 title's length Expected: return negative
     * integer
     */
    @Test
    fun testSort2File2DirAndFile2TitleBigger() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 2, if file2 is directory && file1 title's length and file2 title's length
     * are same, result is zero Input: FileListSorter(-1,2,1) sort is 2 / compare(file1,file2) file1
     * title's length = file2 title's length Expected: return zero
     */
    @Test
    fun testSort2File2DirAndFileTitleSame() {
        val fileListSorter = FileListSorter(-1, 2, 1)
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
     * Purpose: when sort is 3, if file1 extension's length and file2 extension's length are same &&
     * file1 title's length bigger than file2 title's length, result is positive Input:
     * FileListSorter(-1,3,1) sort is 3 / compare(file1,file2) file1 extension's length = file2
     * extension's length && file1 title's length > file2 title's length Expected: return positive
     * integer
     */
    @Test
    fun testSort3FileExtensionSameAndFile1TitleBigger() {
        val fileListSorter = FileListSorter(-1, 3, 1)
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
     * Purpose: when sort is 3, if file1 extension's length and file2 extension's length are same &&
     * file1 title's length smaller than file2 title's length, result is negative Input:
     * FileListSorter(-1,3,1) sort is 3 / compare(file1,file2) file1 extension's length = file2
     * extension's length && file1 title's length < file2 title's length Expected: return negative
     * integer
     */
    @Test
    fun testSort3FileExtensionSameAndFile2TitleBigger() {
        val fileListSorter = FileListSorter(-1, 3, 1)
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
     * Purpose: when sort is 3, if file1 extension's length and file2 extension's length are same &&
     * file1 title's length and file2 title's length are same, result is zero Input:
     * FileListSorter(-1,3,1) sort is 3 / compare(file1,file2) file1 extension's length = file2
     * extension's length && file1 title's length = file2 title's length Expected: return zero
     */
    @Test
    fun testSort3FileExtensionSameAndFileTitleSame() {
        val fileListSorter = FileListSorter(-1, 3, 1)
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
     * Purpose: when sort is not 0,1,2,3, result is zero Input: FileListSorter(-1,4,1) sort is 4
     * Expected: return zero
     */
    @Test
    fun testSortAnotherNumber() {
        val fileListSorter = FileListSorter(-1, 4, 1)
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
}
