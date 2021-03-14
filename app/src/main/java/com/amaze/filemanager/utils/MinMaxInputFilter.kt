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

package com.amaze.filemanager.utils

import android.text.InputFilter
import android.text.Spanned

class MinMaxInputFilter(private val min: Int, private val max: Int) : InputFilter {

    constructor(range: IntRange) : this(range.first, range.last)

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        runCatching {
            val input = (dest.toString() + source.toString()).toInt()
            if (isInRange(min, max, input)) {
                return null
            }
        }
        return ""
    }

    private fun isInRange(minValue: Int, maxValue: Int, input: Int): Boolean {
        return if (maxValue > minValue) {
            input in minValue..maxValue
        } else {
            input in maxValue..minValue
        }
    }
}
