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

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.root.ListFilesCommand
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.EnumSet

class DeepSearchTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @MockK(relaxed = true, relaxUnitFun = true)
    lateinit var context: Context

    @MockK(relaxed = true, relaxUnitFun = true)
    lateinit var foundFileMock: HybridFileParcelable

    val filePath = "/test/abc.txt"
    val fileName = "abc.txt"

    /** Set up all mocks */
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.applicationContext } returns context

        mockkConstructor(HybridFile::class)
        every { anyConstructed<HybridFile>().isDirectory(any()) } returns true

        mockkObject(ListFilesCommand)
        every { ListFilesCommand.listFiles(any(), any(), any(), any(), any()) } answers {
            val onFileFoundCallback = lastArg<(HybridFileParcelable) -> Unit>()
            onFileFoundCallback(foundFileMock)
        }

        every { foundFileMock.isDirectory(any()) } returns false
        every { foundFileMock.isHidden } returns true
        every { foundFileMock.path } returns filePath
        every { foundFileMock.getName(any()) } returns fileName
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
        val deepSearch = DeepSearch(
            "ab",
            filePath,
            EnumSet.of(SearchParameter.SHOW_HIDDEN_FILES),
            context,
            OpenMode.FILE
        )

        val expectedMatchRanges = listOf(0..1)

        deepSearch.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertEquals(listOf(foundFileMock), actualResults.map { it.file })
            Assert.assertEquals(expectedMatchRanges, actualResults.map { it.matchRange })
        }

        runTest {
            deepSearch.search()
        }
    }

    /**
     * If the file name does not match the query, the file should not be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testSimpleSearchNotMatch() {
        val deepSearch = DeepSearch(
            "ba",
            filePath,
            EnumSet.of(SearchParameter.SHOW_HIDDEN_FILES),
            context,
            OpenMode.FILE
        )

        deepSearch.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertTrue(
                listNotEmptyError(actualResults.size),
                actualResults.isEmpty()
            )
        }

        runTest {
            deepSearch.search()
        }
    }

    /**
     * If the match is in the path but not in the name, it should not be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testSearchWithPathMatchButNameNotMatch() {
        val deepSearch = DeepSearch(
            "test",
            filePath,
            EnumSet.of(SearchParameter.SHOW_HIDDEN_FILES),
            context,
            OpenMode.FILE
        )

        deepSearch.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertTrue(
                listNotEmptyError(actualResults.size),
                actualResults.isEmpty()
            )
        }

        runTest {
            deepSearch.search()
        }
    }

    /**
     * If a file is hidden and hidden files should not be shown, it should not be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testMatchHiddenFile() {
        val basicSearch = DeepSearch(
            "ab",
            filePath,
            EnumSet.noneOf(SearchParameter::class.java),
            context,
            OpenMode.FILE
        )

        basicSearch.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertTrue(
                listNotEmptyError(actualResults.size),
                actualResults.isEmpty()
            )
        }

        runTest {
            basicSearch.search()
        }
    }

    private fun listNotEmptyError(size: Int) =
        "List was not empty as expected but had $size elements"
}
