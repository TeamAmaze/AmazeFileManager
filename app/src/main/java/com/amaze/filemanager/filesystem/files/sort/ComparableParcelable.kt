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

/** Used by [FileListSorter] to get the needed information from a `Parcelable` */
interface ComparableParcelable {

    /** Returns if the parcelable represents a directory */
    fun isDirectory(): Boolean

    /** Returns the name of the item represented by the parcelable */
    fun getParcelableName(): String

    /** Returns the date of the item represented by the parcelable as a Long */
    fun getDate(): Long

    /** Returns the size of the item represented by the parcelable */
    fun getSize(): Long
}
