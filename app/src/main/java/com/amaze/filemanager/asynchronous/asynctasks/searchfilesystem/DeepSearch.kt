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
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext

class DeepSearch(
    query: String,
    path: String,
    searchParameters: SearchParameters,
    context: Context,
    private val openMode: OpenMode
) : FileSearch(query, path, searchParameters) {
    private val LOG = LoggerFactory.getLogger(DeepSearch::class.java)

    private val applicationContext: Context

    init {
        applicationContext = context.applicationContext
    }

    /**
     * Search for occurrences of a given text in file names and publish the result
     *
     * @param directory the current path
     */
    override suspend fun search(filter: SearchFilter) {
        val directory = HybridFile(openMode, path)
        if (directory.isSmb) return

        if (directory.isDirectory(applicationContext)) {
            // you have permission to read this directory
            val worklist = ArrayDeque<HybridFile>()
            worklist.add(directory)
            while (coroutineContext.isActive && worklist.isNotEmpty()) {
                val nextFile = worklist.removeFirst()
                nextFile.forEachChildrenFile(
                    applicationContext,
                    SearchParameter.ROOT in searchParameters
                ) { file ->
                    if (!file.isHidden || SearchParameter.SHOW_HIDDEN_FILES in searchParameters) {
                        val resultRange = filter.searchFilter(file.getName(applicationContext))
                        if (resultRange != null) {
                            publishProgress(file, resultRange)
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
}
