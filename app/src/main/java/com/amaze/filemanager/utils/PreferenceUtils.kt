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
package com.amaze.filemanager.utils

import android.graphics.Color

/** Created by Vishal on 12-05-2015 edited by Emmanuel Messulam <emmanuelbendavid></emmanuelbendavid>@gmail.com>  */
object PreferenceUtils {
    const val DEFAULT_PRIMARY = 4
    const val DEFAULT_ACCENT = 1
    const val DEFAULT_ICON = -1
    const val DEFAULT_CURRENT_TAB = 1
    fun getStatusColor(skin: String?): Int {
        return darker(Color.parseColor(skin))
    }

    fun getStatusColor(skin: Int): Int {
        return darker(skin)
    }

    private fun darker(color: Int): Int {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(
                a,
                Math.max((r * 0.6f).toInt(), 0),
                Math.max((g * 0.6f).toInt(), 0),
                Math.max((b * 0.6f).toInt(), 0))
    }
}