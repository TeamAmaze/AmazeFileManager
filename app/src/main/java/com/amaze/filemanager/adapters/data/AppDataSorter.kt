/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters.data

import com.amaze.filemanager.utils.safeLet
import java.util.Comparator

class AppDataSorter(var sort: Int, isAscending: Boolean) :
    Comparator<AppDataParcelable?> {
    private val asc: Int = if (isAscending) 1 else -1

    /**
     * Compares two elements and return negative, zero and positive integer if first argument is
     * less than, equal to or greater than second
     */
    override fun compare(file1: AppDataParcelable?, file2: AppDataParcelable?): Int {
        safeLet(file1, file2) {
            f1, f2 ->
            when (sort) {
                SORT_NAME -> {
                    // sort by name
                    return asc * f1.label.compareTo(f2.label, ignoreCase = true)
                }
                SORT_MODIF -> {
                    // sort by last modified
                    return asc * java.lang.Long.valueOf(f1.lastModification)
                        .compareTo(f2.lastModification)
                }
                SORT_SIZE -> {
                    // sort by size
                    return asc * java.lang.Long.valueOf(f1.size).compareTo(f2.size)
                }
                else -> return 0
            }
        }
        return 0
    }

    companion object {
        const val SORT_NAME = 0
        const val SORT_MODIF = 1
        const val SORT_SIZE = 2
    }
}
