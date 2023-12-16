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

import android.content.Context
import com.amaze.filemanager.R

/**
 * Represents the sort by types.
 * [index] is the index of the sort in the xml string array resource
 * [sortDirectory] indicates if the sort can be used to sort an directory.
 */
enum class SortBy(val index: Int, val sortDirectory: Boolean) {
    NAME(0, true),
    LAST_MODIFIED(1, true),
    SIZE(2, true),
    TYPE(3, true),
    RELEVANCE(4, false);

    /** Returns the corresponding string resource of the enum */
    fun toResourceString(context: Context): String {
        return when (this) {
            NAME -> context.resources.getString(R.string.sort_name)
            LAST_MODIFIED -> context.resources.getString(R.string.lastModified)
            SIZE -> context.resources.getString(R.string.sort_size)
            TYPE -> context.resources.getString(R.string.type)
            RELEVANCE -> context.resources.getString(R.string.sort_relevance)
        }
    }

    companion object {
        const val NAME_INDEX = 0
        const val LAST_MODIFIED_INDEX = 1
        const val SIZE_INDEX = 2
        const val TYPE_INDEX = 3
        const val RELEVANCE_INDEX = 4

        /** Returns the SortBy corresponding to [index] which can be used to sort directories */
        @JvmStatic
        fun getDirectorySortBy(index: Int): SortBy {
            return when (index) {
                NAME_INDEX -> NAME
                LAST_MODIFIED_INDEX -> LAST_MODIFIED
                SIZE_INDEX -> SIZE
                TYPE_INDEX -> TYPE
                else -> NAME
            }
        }

        /** Returns the SortBy corresponding to [index] */
        @JvmStatic
        fun getSortBy(index: Int): SortBy {
            return when (index) {
                NAME_INDEX -> NAME
                LAST_MODIFIED_INDEX -> LAST_MODIFIED
                SIZE_INDEX -> SIZE
                TYPE_INDEX -> TYPE
                RELEVANCE_INDEX -> RELEVANCE
                else -> NAME
            }
        }
    }
}
