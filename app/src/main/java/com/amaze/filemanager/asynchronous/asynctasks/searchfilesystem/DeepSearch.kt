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
import androidx.lifecycle.MutableLiveData
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.coroutineContext

class DeepSearch(
    context: Context,
    private val query: String,
    private val path: String,
    private val openMode: OpenMode,
    private val rootMode: Boolean,
    private val isRegexEnabled: Boolean,
    private val isMatchesEnabled: Boolean,
    private val showHiddenFiles: Boolean
) {
    private val LOG = LoggerFactory.getLogger(DeepSearch::class.java)

    private val hybridFileParcelables: ArrayList<HybridFileParcelable> = ArrayList()
    val mutableLiveData: MutableLiveData<ArrayList<HybridFileParcelable>> = MutableLiveData(
        hybridFileParcelables
    )

    private val applicationContext: Context

    init {
        applicationContext = context.applicationContext
    }

    /**
     * Search for files, whose names match [query], starting from [path] and add them to
     * [mutableLiveData].
     */
    suspend fun search() {
        val file = HybridFile(openMode, path)
        if (file.isSmb) return

        // level 1
        // if regex or not
        if (!isRegexEnabled) {
            search(file, query)
        } else {
            // compile the regular expression in the input
            val pattern = Pattern.compile(
                bashRegexToJava(
                    query
                )
            )
            // level 2
            if (!isMatchesEnabled) searchRegExFind(file, pattern) else searchRegExMatch(
                file,
                pattern
            )
        }
    }

    /**
     * Search for occurrences of a given text in file names and publish the result
     *
     * @param directory the current path
     */
    private suspend fun search(directory: HybridFile, filter: SearchFilter) {
        if (directory.isDirectory(applicationContext)) {
            // you have permission to read this directory
            val worklist = ArrayDeque<HybridFile>()
            worklist.add(directory)
            while (coroutineContext.isActive && worklist.isNotEmpty()) {
                val nextFile = worklist.removeFirst()
                nextFile.forEachChildrenFile(applicationContext, rootMode) { file ->
                    if (!file.isHidden || showHiddenFiles) {
                        if (filter.searchFilter(file.getName(applicationContext))) {
                            publishProgress(file)
                        }
                        if (file.isDirectory(applicationContext)) {
                            worklist.add(file)
                        }
                    }
                }
            }
        } else {
            LOG.warn("Cannot search " + directory.path + ": Permission Denied")
        }
    }

    private fun publishProgress(file: HybridFileParcelable) {
        hybridFileParcelables.add(file)
        mutableLiveData.postValue(hybridFileParcelables)
    }

    /**
     * Recursively search for occurrences of a given text in file names and publish the result
     *
     * @param file the current path
     * @param query the searched text
     */
    private suspend fun search(file: HybridFile, query: String) {
        search(file) { fileName: String ->
            fileName.lowercase(Locale.getDefault()).contains(
                query.lowercase(
                    Locale.getDefault()
                )
            )
        }
    }

    /**
     * Recursively find a java regex pattern [Pattern] in the file names and publish the result
     *
     * @param file the current file
     * @param pattern the compiled java regex
     */
    private suspend fun searchRegExFind(file: HybridFile, pattern: Pattern) {
        search(file) { fileName: String -> pattern.matcher(fileName).find() }
    }

    /**
     * Recursively match a java regex pattern [Pattern] with the file names and publish the
     * result
     *
     * @param file the current file
     * @param pattern the compiled java regex
     */
    private suspend fun searchRegExMatch(file: HybridFile, pattern: Pattern) {
        search(file) { fileName: String -> pattern.matcher(fileName).matches() }
    }

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
        /** Returns if the file with [fileName] as name should fulfills some predicate */
        fun searchFilter(fileName: String): Boolean
    }
}
