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

    val RESERVED_FILE_NAMES = setOf("", ".", "..")

    /**
     * Generates a valid random path
     */
    fun generateRandomPath(random: Random, length: Int): String {
        assert(length > 0)

        val slashesInPath = random.nextInt(length/4)

        return generateRandomPath(random, length, slashesInPath)
    }

    /**
     * Generates a valid random path, with a specific amount of directories
     */
    fun generateRandomPath(random: Random, length: Int, slashesInPath: Int): String {
        val namesInPath = slashesInPath + 1

        val filenameLengths = List(namesInPath) {
            (length - slashesInPath) / namesInPath
        }

        val pathBuilder = StringBuilder()

        for (filenameLength in filenameLengths) {
            val filename = generateRandomFilename(random, filenameLength)
            pathBuilder.append(filename)
            pathBuilder.append(SEPARATOR)
        }

        return pathBuilder.toString()
    }

    private fun generateRandomFilename(random: Random, length: Int): String {
        assert(length > 0)

        var name = ""

        while (RESERVED_FILE_NAMES.contains(name)) {
            name = List(length) { random.nextInt(0, CHARS_FOR_PATH.count()) }
                .map(CHARS_FOR_PATH::get)
                .joinToString("")
        }

        return name
    }
}
