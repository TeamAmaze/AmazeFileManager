/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.files.sort

/** Describes how to sort with [sortBy] and which direction to use for the sort with [sortOrder] */
data class SortType(val sortBy: SortBy, val sortOrder: SortOrder) {

    /**
     * Returns the Int corresponding to the combination of [sortBy] and [sortOrder]
     */
    fun toDirectorySortInt(): Int {
        val sortIndex = if (sortBy.sortDirectory) sortBy.index else 0
        return when (sortOrder) {
            SortOrder.ASC -> sortIndex
            SortOrder.DESC -> sortIndex + 4
        }
    }

    companion object {
        /**
         * Returns the [SortType] with the [SortBy] and [SortOrder] corresponding to [index]
         */
        @JvmStatic
        fun getDirectorySortType(index: Int): SortType {
            val sortOrder = if (index <= 3) SortOrder.ASC else SortOrder.DESC
            val normalizedIndex = if (index <= 3) index else index - 4
            val sortBy = SortBy.getDirectorySortBy(normalizedIndex)
            return SortType(sortBy, sortOrder)
        }
    }
}
