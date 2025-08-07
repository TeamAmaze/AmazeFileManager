/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>,
 * Vishnu Sanal <t.v.s10123@gmail.com> and Contributors.
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

package com.amaze.filemanager.shadows

import android.content.SharedPreferences
import com.amaze.filemanager.filesystem.files.FileUtils
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Shadows [FileUtils].
 */
@Implements(FileUtils::class)
class ShadowFileUtils {
    companion object {
        /**
         * Convenience always true
         */
        @JvmStatic @Implementation
        fun isPathAccessible(
            dir: String?,
            pref: SharedPreferences,
        ) = true
    }
}
