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

package com.amaze.filemanager.database.typeconverters

import androidx.room.TypeConverter
import com.amaze.filemanager.fileoperations.filesystem.OpenMode

/** [TypeConverter] for [OpenMode] objects to database columns.  */
object OpenModeTypeConverter {

    /**
     * Convert given [OpenMode] to integer constant for database storage.
     */
    @JvmStatic
    @TypeConverter
    fun fromOpenMode(from: OpenMode): Int {
        return from.ordinal
    }

    /**
     * Convert value in database to [OpenMode].
     */
    @JvmStatic
    @TypeConverter
    fun fromDatabaseValue(from: Int): OpenMode {
        return OpenMode.getOpenMode(from)
    }
}
