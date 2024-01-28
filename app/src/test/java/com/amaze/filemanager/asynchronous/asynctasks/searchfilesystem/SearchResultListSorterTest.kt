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

package com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem

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
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [Build.VERSION_CODES.KITKAT, Build.VERSION_CODES.P, Build.VERSION_CODES.R]
)
@Suppress("StringLiteralDuplication", "ComplexMethod", "LongMethod", "LargeClass")
class SearchResultListSorterTest {

    private fun getSimpleMatchRange(searchTerm: String, fileName: String): MatchRange {
        val startIndex = fileName.lowercase().indexOf(searchTerm.lowercase())
        return startIndex..(startIndex + searchTerm.length)
    }

    private fun getPatternMatchRange(pattern: Pattern, fileName: String): MatchRange {
        val matcher = pattern.matcher(fileName)
        matcher.find()
        return matcher.start()..matcher.end()
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term more than file2, result is positive
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" more than file2 title
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile1MoreMatchThanFile2() {
        val searchTerm = "abc"
        val title1 = "abc.txt"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABCDE.txt"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )

        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()

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
        ).generateBaseFile()

        Assert.assertEquals(
            -1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term less than file2, result is positive
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" less than file2 title
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile1LessMatchThanFile2() {
        val searchTerm = "abc"
        val title1 = "abcdefg.txt"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC.txt"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)
        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2
     * and file1 starts with search term, result is negative
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title and file1 starts with "abc"
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile1StartsWithSearchTerm() {
        val searchTerm = "abc"
        val title1 = "abc.txt"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "XYZ_ABC"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            -1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2
     * and file2 starts with search term, result is positive
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title and file2 starts with "abc"
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile2StartWithSearchTerm() {
        val searchTerm = "abc"
        val title1 = "txt-abc"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC.txt"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file1 contains the search term as a word (surrounded by
     * separators), result is negative
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file1 contains "abc" as word (separated by "-")
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile1HasSearchTermAsWord() {
        val searchTerm = "abc"
        val title1 = "abc-efg.txt"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABCD-FG.txt"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            -1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file2 contains the search term as a word (surrounded by
     * separators), result is positive
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file2 contains "abc" as word (separated by "_")
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile2HasSearchTermAsWord() {
        val searchTerm = "abc"
        val title1 = "abcdefg"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC_EFG"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)
        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file2 contains the search term as a word (surrounded by
     * separators), result is positive
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file2 contains "abc" as word (separated by " ")
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithSpaceWordSeparator() {
        val searchTerm = "abc"
        val title1 = "abcdefg"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC EFG"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term and file2 contains the search term as a word (surrounded by
     * separators), result is positive
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc"
     * and file2 contains "abc" as word (separated by ".")
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithDotWordSeparator() {
        val searchTerm = "abc"
        val title1 = "abcdefg"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC.EFG"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term, both contain the search term as a word and file1 date is more recent,
     * result is negative
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc",
     * both contain "abc" as word and file1 date is more recent
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWithFile1MoreRecent() {
        val searchTerm = "abc"
        val title1 = "abc.efg"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC_EFG"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val currentTime = Date().time
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
            "C:\\AmazeFileManager\\abc.efg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            (currentTime - TimeUnit.MINUTES.toMillis(5)).toString(),
            false,
            false,
            OpenMode.UNKNOWN
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
            "C:\\AmazeFileManager\\ABC_EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            (currentTime - TimeUnit.MINUTES.toMillis(10)).toString(),
            false,
            false,
            OpenMode.UNKNOWN
        ).generateBaseFile()
        Assert.assertEquals(
            -1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term, both contain the search term as a word and file2 date is more recent,
     * result is positive
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc",
     * both contain "abc" as word and file2 date is more recent
     *
     * Expected: return positive integer
     */
    @Test
    fun testSortByRelevanceWithFile2MoreRecent() {
        val searchTerm = "abc"
        val title1 = "abc.efg"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC_EFG"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val currentTime = Date().time
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
            "C:\\AmazeFileManager\\abc.efg",
            "user",
            "symlink",
            "100",
            123L,
            true,
            (currentTime - TimeUnit.MINUTES.toMillis(10)).toString(),
            false,
            false,
            OpenMode.UNKNOWN
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
            "C:\\AmazeFileManager\\ABC_EFG",
            "user",
            "symlink",
            "101",
            124L,
            true,
            (currentTime - TimeUnit.MINUTES.toMillis(5)).toString(),
            false,
            false,
            OpenMode.UNKNOWN
        ).generateBaseFile()
        Assert.assertEquals(
            1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file1 matches the search term as much as file2,
     * both start with search term, both contain the search term as a word and date is same,
     * result is zero
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file1 title matches "abc" as much as file2 title, both start with "abc",
     * both contain "abc" as word and the date of both is the same
     *
     * Expected: return zero
     */
    @Test
    fun testSortByRelevanceWithSameRelevance() {
        val searchTerm = "abc"
        val title1 = "abc.efg"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "ABC_EFG"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            searchTerm
        )
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
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
        ).generateBaseFile()
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
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
        ).generateBaseFile()
        Assert.assertEquals(
            0,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }

    /**
     * Purpose: when sort is [SortBy.RELEVANCE], if file2 matches the search term more than file1
     * and file2 date is more recent, but file1 starts with search term and contains the
     * search term as a word, the result is negative.
     *
     * Input: SearchResultListSorter with [DirSortBy.NONE_ON_TOP], [SortBy.RELEVANCE], [SortOrder.ASC] and search term "abc"
     * compare(file1,file2) file2 title matches "abc" more than file1 title and is more recent both start with "abc",
     * both contain "abc" as word and the date of both is the same
     *
     * Expected: return negative integer
     */
    @Test
    fun testSortByRelevanceWhole() {
        val searchTerm = "abc"
        val title1 = "abc.efghij"
        val matchRange1 = getSimpleMatchRange(searchTerm, title1)
        val title2 = "EFGABC"
        val matchRange2 = getSimpleMatchRange(searchTerm, title2)

        val searchResultListSorter = SearchResultListSorter(
            DirSortBy.NONE_ON_TOP,
            SortType(SortBy.RELEVANCE, SortOrder.ASC),
            "abc"
        )
        val currentTime = Date().time

        // matches 3/10
        // starts with search term
        // contains search as whole word
        // modification time is less recent
        val file1 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title1,
            "C:\\AmazeFileManager\\abc.efghij",
            "user",
            "symlink",
            "100",
            123L,
            true,
            (currentTime - TimeUnit.MINUTES.toMillis(10)).toString(),
            false,
            false,
            OpenMode.UNKNOWN
        ).generateBaseFile()
        // matches 3/6
        // doesn't start with search term
        // doesn't contain as whole word
        // modification time is more recent
        val file2 = LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            title2,
            "C:\\AmazeFileManager\\EFGABC",
            "user",
            "symlink",
            "101",
            124L,
            true,
            (currentTime - TimeUnit.MINUTES.toMillis(5)).toString(),
            false,
            false,
            OpenMode.UNKNOWN
        ).generateBaseFile()
        Assert.assertEquals(
            -1,
            searchResultListSorter.compare(
                SearchResult(file1, matchRange1),
                SearchResult(file2, matchRange2)
            ).toLong()
        )
    }
}
