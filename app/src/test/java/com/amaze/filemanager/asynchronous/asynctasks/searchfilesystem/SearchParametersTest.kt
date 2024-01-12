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

import org.junit.Assert
import org.junit.Test
import java.util.EnumSet

class SearchParametersTest {

    /** Tests [SearchParameters.and] */
    @Test
    fun testAnd() {
        val expected = EnumSet.of(SearchParameter.SHOW_HIDDEN_FILES, SearchParameter.ROOT)
        val actual = SearchParameter.SHOW_HIDDEN_FILES and SearchParameter.ROOT
        Assert.assertEquals(expected, actual)
    }

    /** Tests [SearchParameters.plus] */
    @Test
    fun testPlus() {
        val expected = EnumSet.of(SearchParameter.REGEX, SearchParameter.ROOT)
        val actual = EnumSet.of(SearchParameter.REGEX) + SearchParameter.ROOT
        Assert.assertEquals(expected, actual)
    }

    /** Tests [searchParametersFromBoolean] with no flag turned on */
    @Test
    fun testSearchParametersFromBooleanWithNone() {
        val expected = EnumSet.noneOf(SearchParameter::class.java)
        val actual = searchParametersFromBoolean()
        Assert.assertEquals(expected, actual)
    }

    /** Tests [searchParametersFromBoolean] with one flag turned on */
    @Test
    fun testSearchParametersFromBooleanWithOne() {
        val expected = EnumSet.of(SearchParameter.ROOT)
        val actual = searchParametersFromBoolean(isRoot = true)
        Assert.assertEquals(expected, actual)
    }

    /** Tests [searchParametersFromBoolean] with two flags turned on */
    @Test
    fun testSearchParametersFromBooleanWithTwo() {
        val expected = EnumSet.of(SearchParameter.ROOT, SearchParameter.SHOW_HIDDEN_FILES)
        val actual = searchParametersFromBoolean(isRoot = true, showHiddenFiles = true)
        Assert.assertEquals(expected, actual)
    }

    /** Tests [searchParametersFromBoolean] with three flags turned on */
    @Test
    fun testSearchParametersFromBooleanWithThree() {
        val expected = EnumSet.of(
            SearchParameter.ROOT,
            SearchParameter.REGEX,
            SearchParameter.REGEX_MATCHES
        )
        val actual = searchParametersFromBoolean(
            isRoot = true,
            isRegexEnabled = true,
            isRegexMatchesEnabled = true
        )
        Assert.assertEquals(expected, actual)
    }

    /** Tests [searchParametersFromBoolean] with four flags turned on */
    @Test
    fun testSearchParametersFromBooleanWithFour() {
        val expected = EnumSet.of(
            SearchParameter.ROOT,
            SearchParameter.REGEX,
            SearchParameter.REGEX_MATCHES,
            SearchParameter.SHOW_HIDDEN_FILES
        )
        val actual = searchParametersFromBoolean(
            isRoot = true,
            isRegexEnabled = true,
            isRegexMatchesEnabled = true,
            showHiddenFiles = true
        )
        Assert.assertEquals(expected, actual)
    }
}
