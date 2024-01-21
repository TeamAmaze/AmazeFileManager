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

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.util.EnumSet

class FileSearchTest {
    private fun getFileSearchMatch(
        query: String,
        path: String,
        searchParameters: SearchParameters
    ): FileSearch = object : FileSearch(query, path, searchParameters) {
        override suspend fun search(
            filter: SearchFilter
        ) {
            val matchRange = filter.searchFilter(path)
            Assert.assertNotNull("Expected $path to match filter", matchRange)
            Assert.assertTrue("Start of match range is negative", matchRange!!.first >= 0)
            Assert.assertTrue(
                "End of match range is larger than length of $path",
                matchRange.last < path.length
            )
            val expectedRange = 5..9
            Assert.assertEquals(
                "Range was not as expected $expectedRange but $matchRange",
                expectedRange,
                matchRange
            )
        }
    }

    private fun getFileSearchNotMatch(
        query: String,
        path: String,
        searchParameters: SearchParameters
    ): FileSearch =
        object : FileSearch(query, path, searchParameters) {
            override suspend fun search(filter: SearchFilter) {
                val matchRange = filter.searchFilter(path)
                Assert.assertNull("Expected $path to not match filter", matchRange)
            }
        }

    private fun getFileSearchRegexMatches(
        query: String,
        path: String,
        searchParameters: SearchParameters
    ): FileSearch = object : FileSearch(query, path, searchParameters) {
        override suspend fun search(
            filter: SearchFilter
        ) {
            val matchRange = filter.searchFilter(path)
            Assert.assertNotNull("Expected $path to match filter", matchRange)
            Assert.assertTrue("Start of match range is negative", matchRange!!.first >= 0)
            Assert.assertTrue(
                "End of match range is larger than length of $path",
                matchRange.last < path.length
            )
            val expectedRange = path.indices
            Assert.assertEquals(
                "Range was not as expected $expectedRange but $matchRange",
                expectedRange,
                matchRange
            )
        }
    }

    /** Test the simple filter with a path that matches the query */
    @Test
    fun simpleFilterMatchTest() = runTest {
        getFileSearchMatch(
            "abcde",
            "01234ABcDe012",
            EnumSet.noneOf(SearchParameter::class.java)
        ).search()
    }

    /** Test the simple filter with a path that does not match the query */
    @Test
    fun simpleFilterNotMatchTest() = runTest {
        // There is no "e"
        getFileSearchNotMatch(
            "abcde",
            "01234abcd9012",
            EnumSet.noneOf(SearchParameter::class.java)
        ).search()
    }

    /** Test the regex filter with a path that matches the query. The query contains `*`. */
    @Test
    fun regexFilterStarMatchTest() = runTest {
        getFileSearchMatch(
            "a*e",
            "01234ABcDe012",
            SearchParameters.of(SearchParameter.REGEX)
        ).search()
    }

    /** Test the regex filter with a path that does not match the query. The query contains `*`. */
    @Test
    fun regexFilterStarNotMatchTest() = runTest {
        // There is no "e"
        getFileSearchNotMatch(
            "a*e",
            "01234aBcD9012",
            SearchParameters.of(SearchParameter.REGEX)
        ).search()
    }

    /** Test the regex filter with a path that matches the query. The query contains `?`. */
    @Test
    fun regexFilterQuestionMarkMatchTest() = runTest {
        getFileSearchMatch(
            "a???e",
            "01234ABcDe0123",
            SearchParameters.of(SearchParameter.REGEX)
        ).search()
    }

    /** Test the regex filter with a path that does not match the query. The query contains `?`. */
    @Test
    fun regexFilterQuestionMarkNotMatchTest() = runTest {
        // There is one character missing between "a" and "e"
        getFileSearchNotMatch(
            "a???e",
            "01234ABce9012",
            SearchParameters.of(SearchParameter.REGEX)
        ).search()
    }

    /**
     * Test the regex filter with a path that does not match the query
     * because `-` is not recognized by `?` or `*`.
     */
    @Test
    fun regexFilterNotMatchNonWordCharacterTest() = runTest {
        getFileSearchNotMatch(
            "a?c*e",
            "0A-corn search",
            SearchParameters.of(SearchParameter.REGEX)
        ).search()
    }

    /** Test the regex match filter with a path that completely matches the query */
    @Test
    fun regexMatchFilterMatchTest() = runTest {
        getFileSearchRegexMatches(
            "a*e",
            "A1234ABcDe0123e",
            SearchParameter.REGEX + SearchParameter.REGEX_MATCHES
        ).search()
    }

    /** Test the regex match filter with a path that does not completely match the query */
    @Test
    fun regexMatchFilterNotMatchTest() = runTest {
        // Pattern does not match whole name
        getFileSearchNotMatch(
            "a*e",
            "01234ABcDe0123",
            SearchParameter.REGEX + SearchParameter.REGEX_MATCHES
        ).search()
    }
}
