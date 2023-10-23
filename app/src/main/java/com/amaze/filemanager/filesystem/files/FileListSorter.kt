/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.files

import androidx.annotation.IntDef
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import java.util.Locale

/**
 * [Comparator] implementation to sort [LayoutElementParcelable]s.
 */
class FileListSorter(
    @DirSortMode dirArg: Int,
    @SortBy sortArg: Int,
    @SortOrder ascArg: Int
) : Comparator<LayoutElementParcelable> {
    private var dirsOnTop = dirArg
    private val asc = ascArg
    private val sort = sortArg

    private fun isDirectory(path: LayoutElementParcelable): Boolean {
        return path.isDirectory
    }

    /**
     * Compares two elements and return negative, zero and positive integer if first argument is less
     * than, equal to or greater than second
     */
    override fun compare(file1: LayoutElementParcelable, file2: LayoutElementParcelable): Int {
        /*File f1;

    if(!file1.hasSymlink()) {

        f1=new File(file1.getDesc());
    } else {
        f1=new File(file1.getSymlink());
    }

    File f2;

    if(!file2.hasSymlink()) {

        f2=new File(file2.getDesc());
    } else {
        f2=new File(file1.getSymlink());
    }*/
        if (dirsOnTop == SORT_DIR_ON_TOP) {
            if (isDirectory(file1) && !isDirectory(file2)) {
                return -1
            } else if (isDirectory(file2) && !isDirectory(file1)) {
                return 1
            }
        } else if (dirsOnTop == SORT_FILE_ON_TOP) {
            if (isDirectory(file1) && !isDirectory(file2)) {
                return 1
            } else if (isDirectory(file2) && !isDirectory(file1)) {
                return -1
            }
        }

        when (sort) {
            SORT_BY_NAME -> {
                // sort by name
                return asc * file1.title.compareTo(file2.title, ignoreCase = true)
            }
            SORT_BY_LAST_MODIFIED -> {
                // sort by last modified
                return asc * java.lang.Long.valueOf(file1.date).compareTo(file2.date)
            }
            SORT_BY_SIZE -> {
                // sort by size
                return if (!file1.isDirectory && !file2.isDirectory) {
                    asc * java.lang.Long.valueOf(file1.longSize).compareTo(file2.longSize)
                } else {
                    file1.title.compareTo(file2.title, ignoreCase = true)
                }
            }
            SORT_BY_TYPE -> {
                // sort by type
                return if (!file1.isDirectory && !file2.isDirectory) {
                    val ext_a = getExtension(file1.title)
                    val ext_b = getExtension(file2.title)
                    val res = asc * ext_a.compareTo(ext_b)
                    if (res == 0) {
                        asc * file1.title.compareTo(file2.title, ignoreCase = true)
                    } else {
                        res
                    }
                } else {
                    file1.title.compareTo(file2.title, ignoreCase = true)
                }
            }
            else -> return 0
        }
    }

    companion object {

        const val SORT_BY_NAME = 0
        const val SORT_BY_LAST_MODIFIED = 1
        const val SORT_BY_SIZE = 2
        const val SORT_BY_TYPE = 3

        const val SORT_DIR_ON_TOP = 0
        const val SORT_FILE_ON_TOP = 1
        const val SORT_NONE_ON_TOP = 2

        const val SORT_ASC = 1
        const val SORT_DSC = -1

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(SORT_BY_NAME, SORT_BY_LAST_MODIFIED, SORT_BY_SIZE, SORT_BY_TYPE)
        annotation class SortBy

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(SORT_DIR_ON_TOP, SORT_FILE_ON_TOP, SORT_NONE_ON_TOP)
        annotation class DirSortMode

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(SORT_ASC, SORT_DSC)
        annotation class SortOrder

        /**
         * Convenience method to get the file extension in given path.
         *
         * TODO: merge with same definition somewhere else (if any)
         */
        @JvmStatic
        fun getExtension(a: String): String {
            return a.substringAfterLast('.').lowercase(Locale.getDefault())
        }
    }
}
