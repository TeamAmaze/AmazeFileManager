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

import java.util.*

class OneCharacterCharSequence(private val value: Char, override val length: Int) : CharSequence {
    override fun get(index: Int): Char =
        if (index < length) value
        else throw IndexOutOfBoundsException()

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        OneCharacterCharSequence(value, endIndex - startIndex)

    override fun toString(): String {
        val array = CharArray(length)
        Arrays.fill(array, value)
        return String(array)
    }
}
