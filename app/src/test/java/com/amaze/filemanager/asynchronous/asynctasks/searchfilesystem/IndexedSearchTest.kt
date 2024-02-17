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

import android.database.Cursor
import android.provider.MediaStore
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.EnumSet

class IndexedSearchTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    val dataColumn = 0
    val displayNameColumn = 1

    @RelaxedMockK
    lateinit var mockCursor: Cursor

    val filePath = "/test/abc.txt"
    val fileName = "abc.txt"

    /** Set up all mocks */
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { mockCursor.count } returns 1
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns false
        every {
            mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
        } returns dataColumn
        every {
            mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        } returns displayNameColumn

        every { mockCursor.getString(dataColumn) } returns filePath
        every { mockCursor.getString(displayNameColumn) } returns fileName
    }

    /** Clean up all mocks */
    @After
    fun cleanup() {
        unmockkAll()
    }

    /**
     * If the file name matches the query, the file should be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testSimpleSearchMatch() {
        val expectedNames = listOf(fileName)
        val expectedPaths = listOf(filePath)
        val expectedRanges = listOf<MatchRange>(0..1)

        val indexedSearch = IndexedSearch(
            "ab",
            "/",
            EnumSet.noneOf(SearchParameter::class.java),
            mockCursor
        )
        indexedSearch.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertEquals(expectedNames, actualResults!!.map { (file, _) -> file.name })
            Assert.assertEquals(expectedPaths, actualResults!!.map { (file, _) -> file.path })
            Assert.assertEquals(expectedRanges, actualResults.map { it.matchRange })
        }
        runTest {
            indexedSearch.search()
        }
    }

    /**
     * If the file name does not match the query, the file should not be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testSimpleSearchNotMatch() {
        val indexedSearch = IndexedSearch(
            "ba",
            "/",
            EnumSet.noneOf(SearchParameter::class.java),
            mockCursor
        )
        indexedSearch.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertTrue(
                listNotEmptyError(actualResults.size),
                actualResults.isEmpty()
            )
        }
        runTest {
            indexedSearch.search()
        }
    }

    /**
     * If the match is in the path but not in the name, it should not be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testSearchWithPathMatchButNameNotMatch() {
        val indexedSearch = IndexedSearch(
            "te",
            "/",
            EnumSet.noneOf(SearchParameter::class.java),
            mockCursor
        )
        indexedSearch.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertTrue(
                listNotEmptyError(actualResults.size),
                actualResults.isEmpty()
            )
        }
        runTest {
            indexedSearch.search()
        }
    }

    private fun listNotEmptyError(size: Int) =
        "List was not empty as expected but had $size elements"
}
