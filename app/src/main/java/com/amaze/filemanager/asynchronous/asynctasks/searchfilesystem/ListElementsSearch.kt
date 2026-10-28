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

import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class ListElementsSearch(
    query: String,
    path: String,
    searchParameters: SearchParameters,
    private val items: List<LayoutElementParcelable>,
) : FileSearch(query, path, searchParameters) {
    override suspend fun search(filter: SearchFilter) {
        for (item in items) {
            if (!coroutineContext.isActive) {
                break
            }

            if (item.isBack || item.header) {
                continue
            }

            val resultRange = filter.searchFilter(item.title)
            if (resultRange != null) {
                publishProgress(item.generateBaseFile(), resultRange)
            }
        }
    }
}
