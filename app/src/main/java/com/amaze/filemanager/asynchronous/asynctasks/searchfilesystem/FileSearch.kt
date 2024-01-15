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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amaze.filemanager.filesystem.HybridFileParcelable
import java.util.Locale
import java.util.regex.Pattern

sealed class FileSearch {
    private val mutableFoundFilesLiveData: MutableLiveData<List<SearchResult>> =
        MutableLiveData()
    val foundFilesLiveData: LiveData<List<SearchResult>> = mutableFoundFilesLiveData

    /**
     * Search for files, whose names match [query], starting from [path] and add them to
     * [foundFilesLiveData]
     */
    suspend fun search(query: String, path: String, searchParameters: SearchParameters) {
        if (SearchParameter.REGEX !in searchParameters) {
            // regex not turned on so we use simpleFilter
            search(path, simpleFilter(query), searchParameters)
        } else {
            if (SearchParameter.REGEX_MATCHES !in searchParameters) {
                // only regex turned on so we use regexFilter
                search(path, regexFilter(query), searchParameters)
            } else {
                // regex turned on and names must match pattern so use regexMatchFilter
                search(path, regexMatchFilter(query), searchParameters)
            }
        }
    }

    /**
     * Search for files, whose names fulfill [filter], starting from [path] and add them to
     * [foundFilesLiveData].
     */
    protected abstract suspend fun search(
        path: String,
        filter: SearchFilter,
        searchParameters: SearchParameters
    )

    /**
     * Add [file] to list of found files and post it to [foundFilesLiveData]
     */
    protected fun publishProgress(
        file: HybridFileParcelable,
        matchRange: MatchRange
    ) {
        val files = mutableFoundFilesLiveData.value.orEmpty().toMutableList()
        files.add(SearchResult(file, matchRange))
        mutableFoundFilesLiveData.postValue(files)
    }

    private fun simpleFilter(query: String): SearchFilter =
        SearchFilter { fileName ->
            // check case-insensitively if query is contained in fileName
            val start = fileName.lowercase(Locale.getDefault()).indexOf(
                query.lowercase(
                    Locale.getDefault()
                )
            )
            if (start >= 0) {
                start..(start + query.length)
            } else {
                null
            }
        }

    private fun regexFilter(query: String): SearchFilter {
        val pattern = regexPattern(query)
        return SearchFilter { fileName ->
            // check case-insensitively if the pattern compiled from query can be found in fileName
            val matcher = pattern.matcher(fileName)
            if (matcher.find()) {
                matcher.start()..matcher.end()
            } else {
                null
            }
        }
    }

    private fun regexMatchFilter(query: String): SearchFilter {
        val pattern = regexPattern(query)
        return SearchFilter { fileName ->
            // check case-insensitively if the pattern compiled from query matches fileName
            if (pattern.matcher(fileName).matches()) {
                fileName.indices
            } else {
                null
            }
        }
    }

    private fun regexPattern(query: String): Pattern =
        // compiles the given query into a Pattern
        Pattern.compile(
            bashRegexToJava(query),
            Pattern.CASE_INSENSITIVE
        )

    /**
     * method converts bash style regular expression to java. See [Pattern]
     *
     * @return converted string
     */
    private fun bashRegexToJava(originalString: String): String {
        val stringBuilder = StringBuilder()
        for (i in originalString.indices) {
            when (originalString[i].toString() + "") {
                "*" -> stringBuilder.append("\\w*")
                "?" -> stringBuilder.append("\\w")
                else -> stringBuilder.append(originalString[i])
            }
        }
        return stringBuilder.toString()
    }

    fun interface SearchFilter {
        /** If the file with the given [fileName] fulfills some predicate, return the part that fulfills the predicate */
        fun searchFilter(fileName: String): MatchRange?
    }
}