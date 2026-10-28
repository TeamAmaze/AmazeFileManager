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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.EnumSet

@RunWith(AndroidJUnit4::class)
class ListElementsSearchTest {
    private companion object {
        const val QUERY = "report"
    }

    @get:Rule
    val rule = InstantTaskExecutorRule()

    /**
     * If the visible item title matches the query, the item should be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testSimpleSearchMatch() {
        val search =
            ListElementsSearch(
                QUERY,
                "/",
                EnumSet.noneOf(SearchParameter::class.java),
                listOf(
                    layoutElement("Annual Report.pdf", "/drawer/Annual Report.pdf"),
                    layoutElement("Vacation Photo.jpg", "/drawer/Vacation Photo.jpg"),
                ),
            )

        search.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertEquals(listOf("Annual Report.pdf"), actualResults.map { it.file.getName() })
            Assert.assertEquals(listOf("/drawer/Annual Report.pdf"), actualResults.map { it.file.getPath() })
            Assert.assertEquals(listOf(7..12), actualResults.map { it.matchRange })
        }

        runTest {
            search.search()
        }
    }

    /**
     * Back and header items should not be added to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testSkipsBackAndHeaderItems() {
        val search =
            ListElementsSearch(
                QUERY,
                "/",
                EnumSet.noneOf(SearchParameter::class.java),
                listOf(
                    layoutElement("Report Back", "/drawer/back", isBack = true),
                    layoutElement("Report Header", "/drawer/header", header = true),
                    layoutElement("Report File.txt", "/drawer/Report File.txt"),
                ),
            )

        search.foundFilesLiveData.observeForever { actualResults ->
            Assert.assertNotNull(actualResults)
            Assert.assertEquals(listOf("Report File.txt"), actualResults.map { it.file.getName() })
            Assert.assertEquals(listOf("/drawer/Report File.txt"), actualResults.map { it.file.getPath() })
        }

        runTest {
            search.search()
        }
    }

    /**
     * If no visible item title matches the query, no results should be published to
     * [FileSearch.foundFilesLiveData]
     */
    @Test
    fun testNoMatchDoesNotPublishResults() {
        var observerCalled = false

        val search =
            ListElementsSearch(
                QUERY,
                "/",
                EnumSet.noneOf(SearchParameter::class.java),
                listOf(
                    layoutElement("Vacation Photo.jpg", "/drawer/Vacation Photo.jpg"),
                    layoutElement("Budget 2025.xlsx", "/drawer/Budget 2025.xlsx"),
                ),
            )

        search.foundFilesLiveData.observeForever {
            observerCalled = true
        }

        runTest {
            search.search()
        }

        Assert.assertFalse(observerCalled)
    }

    private fun layoutElement(
        title: String,
        path: String,
        isBack: Boolean = false,
        header: Boolean = false,
    ): LayoutElementParcelable =
        LayoutElementParcelable(
            ApplicationProvider.getApplicationContext(),
            isBack,
            title,
            path,
            "",
            "",
            "",
            0,
            header,
            "",
            false,
            false,
            OpenMode.FILE,
        )
}
