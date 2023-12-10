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

import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.filesystem.files.sort.ComparableParcelable
import com.amaze.filemanager.filesystem.files.sort.DirSortBy
import com.amaze.filemanager.filesystem.files.sort.SortBy
import com.amaze.filemanager.filesystem.files.sort.SortType
import java.lang.Long
import java.util.Locale

/**
 * [Comparator] implementation to sort [LayoutElementParcelable]s.
 */
class FileListSorter(
    dirArg: DirSortBy,
    sortType: SortType,
    searchTerm: String?
) : Comparator<ComparableParcelable> {
    private var dirsOnTop = dirArg
    private val asc: Int = sortType.sortOrder.sortFactor
    private val sort: SortBy = sortType.sortBy

    private val relevanceComparator: Comparator<ComparableParcelable> by lazy {
        if (searchTerm == null) {
            // no search term given, so every result is equally relevant
            Comparator { _, _ ->
                0
            }
        } else {
            Comparator { o1, o2 ->
                // Sorts in a way that least relevant is first
                val comparator = compareBy<ComparableParcelable> {
                    // first we compare by the match percentage of the name
                    searchTerm.length.toDouble() / it.getParcelableName().length.toDouble()
                }.thenBy {
                    // if match percentage is the same, we compare if the name starts with the match
                    it.getParcelableName().startsWith(searchTerm, ignoreCase = true)
                }.thenBy { file ->
                    // if the match in the name could a word because it is surrounded by separators, it could be more relevant
                    // e.g. "my-cat" more relevant than "mysterious"
                    file.getParcelableName().split('-', '_', '.', ' ').any {
                        it.contentEquals(
                            searchTerm,
                            ignoreCase = true
                        )
                    }
                }.thenBy { file ->
                    // sort by modification date as last resort
                    file.getDate()
                }
                // Reverts the sorting to make most relevant first
                comparator.compare(o1, o2) * -1
            }
        }
    }

    /** Constructor for convenience if there is no searchTerm */
    constructor(dirArg: DirSortBy, sortType: SortType) : this(dirArg, sortType, null)

    private fun isDirectory(path: ComparableParcelable): Boolean {
        return path.isDirectory()
    }

    /** Compares the names of [file1] and [file2] */
    private fun compareName(file1: ComparableParcelable, file2: ComparableParcelable): Int {
        return file1.getParcelableName().compareTo(file2.getParcelableName(), ignoreCase = true)
    }

    /**
     * Compares two elements and return negative, zero and positive integer if first argument is less
     * than, equal to or greater than second
     */
    override fun compare(file1: ComparableParcelable, file2: ComparableParcelable): Int {
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
        if (dirsOnTop == DirSortBy.DIR_ON_TOP) {
            if (isDirectory(file1) && !isDirectory(file2)) {
                return -1
            } else if (isDirectory(file2) && !isDirectory(file1)) {
                return 1
            }
        } else if (dirsOnTop == DirSortBy.FILE_ON_TOP) {
            if (isDirectory(file1) && !isDirectory(file2)) {
                return 1
            } else if (isDirectory(file2) && !isDirectory(file1)) {
                return -1
            }
        }

        when (sort) {
            SortBy.NAME -> {
                // sort by name
                return asc * compareName(file1, file2)
            }
            SortBy.LAST_MODIFIED -> {
                // sort by last modified
                return asc * Long.valueOf(file1.getDate()).compareTo(file2.getDate())
            }
            SortBy.SIZE -> {
                // sort by size
                return if (!isDirectory(file1) && !isDirectory(file2)) {
                    asc * Long.valueOf(file1.getSize()).compareTo(file2.getSize())
                } else {
                    compareName(file1, file2)
                }
            }
            SortBy.TYPE -> {
                // sort by type
                return if (!isDirectory(file1) && !isDirectory(file2)) {
                    val ext_a = getExtension(file1.getParcelableName())
                    val ext_b = getExtension(file2.getParcelableName())
                    val res = asc * ext_a.compareTo(ext_b)
                    if (res == 0) {
                        asc * compareName(file1, file2)
                    } else {
                        res
                    }
                } else {
                    compareName(file1, file2)
                }
            }
            SortBy.RELEVANCE -> {
                // sort by relevance to the search query
                return asc * relevanceComparator.compare(file1, file2)
            }
        }
    }

    companion object {

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
