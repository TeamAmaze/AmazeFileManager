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

import java.util.EnumSet

typealias SearchParameters = EnumSet<SearchParameter>

/**
 * Returns [SearchParameters] extended by [other]
 */
infix fun SearchParameters.and(other: SearchParameter): SearchParameters = SearchParameters.of(
    other,
    *this.toTypedArray()
)

/**
 * Returns [SearchParameters] extended by [other]
 */
operator fun SearchParameters.plus(other: SearchParameter): SearchParameters = this and other

/**
 * Returns [SearchParameters] that reflect the given Booleans
 */
fun searchParametersFromBoolean(
    showHiddenFiles: Boolean = false,
    isRegexEnabled: Boolean = false,
    isRegexMatchesEnabled: Boolean = false,
    isRoot: Boolean = false
): SearchParameters {
    val searchParameterList = mutableListOf<SearchParameter>()

    if (showHiddenFiles) searchParameterList.add(SearchParameter.SHOW_HIDDEN_FILES)
    if (isRegexEnabled) searchParameterList.add(SearchParameter.REGEX)
    if (isRegexMatchesEnabled) searchParameterList.add(SearchParameter.REGEX_MATCHES)
    if (isRoot) searchParameterList.add(SearchParameter.ROOT)

    return if (searchParameterList.isEmpty()) {
        SearchParameters.noneOf(SearchParameter::class.java)
    } else {
        SearchParameters.copyOf(searchParameterList)
    }
}
