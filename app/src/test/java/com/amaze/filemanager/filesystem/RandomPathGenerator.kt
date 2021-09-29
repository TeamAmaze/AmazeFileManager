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
    const val separator = '/'

    private val letters = ('A'..'Z').toSet() + ('a'..'z').toSet()
    private val numbers = ('0'..'9').toSet()
    private val other = setOf('.', '_', '-')
    private val reservedFileNames = setOf("", ".", "..")

    /**
     * Generates a valid random path
     */
    fun generateRandomPath(random: Random, length: Int): String {
        assert(length > 0)

        val slashesInPath = random.nextInt(length / 4)

        return generateRandomPath(random, length, slashesInPath)
    }

    /**
     * Generates a valid random path, with a specific amount of directories
     */
    fun generateRandomPath(random: Random, length: Int, slashesInPath: Int): String {
        assert(length > slashesInPath * 2)

        val namesInPath = slashesInPath + 1

        val filenameLengths = List(namesInPath) {
            (length - slashesInPath) / namesInPath
        }

        val pathBuilder = mutableListOf<String>()

        for (filenameLength in filenameLengths) {
            val filename = generateRandomFilename(random, filenameLength)
            pathBuilder.add(filename)
        }

        var path = pathBuilder.joinToString(separator = separator.toString())

        val randomNumber = random.nextDouble(0.0, 1.0)

        if (randomNumber < 0.2) {
            // 20% end slash
            path = path.dropLast(1)
            path += separator
            return path
        }

        if (randomNumber < 0.6) {
            // 40% end extension
            val extension = List(3) { letters.random(random) }.joinToString(separator = "")

            path = path.dropLast(4)
            path += ".$extension"
            return path
        }

        return path
    }

    private fun generateRandomFilename(random: Random, length: Int): String {
        assert(length > 0)

        var name = ""

        while (reservedFileNames.contains(name)) {
            name = List(length) { generateRandomCharacter(random) }
                .joinToString("")
        }

        return name
    }

    /**
     * Characters from POSIX 3.282 Portable Filename Character Set.
     *
     * Not all characters should be tested equally,
     * this ensures that paths not only contain letters.
     */
    private fun generateRandomCharacter(random: Random): Char {
        val randomNumber = random.nextDouble(0.0, 1.0)

        if (randomNumber < 0.4) {
            // 40% characters
            return letters.random(random)
        }

        if (randomNumber < 0.8) {
            // 40% numbers
            return numbers.random(random)
        }

        if (randomNumber < 1.0) {
            // 20% other
            return other.random(random)
        }

        throw IllegalStateException()
    }
}
