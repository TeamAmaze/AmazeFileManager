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

package com.amaze.filemanager.filesystem

import kotlin.random.Random

object RandomPathGenerator {
    /**
     * From POSIX 3.282 Portable Filename Character Set
     */
    val CHARS_FOR_PATH = ('A'..'Z').toList() + ('a'..'z').toList() + ('0'..'9').toList() + listOf('.', '_', '-')
    val SEPARATOR = '/'

    /**
     * Generates a valid random path
     */
    fun generateRandomPath(random: Random, length: Int): String {
        val randomString = (1..length)
            .map { i -> random.nextInt(0, CHARS_FOR_PATH.count()) }
            .map(CHARS_FOR_PATH::get)

        val path = randomString.mapIndexed { i, e ->
            if (random.nextInt(10) < 1 && i > 0 && randomString[i - 1] != SEPARATOR) {
                SEPARATOR
            } else {
                e
            }
        }

        return path.joinToString("")
    }
}
