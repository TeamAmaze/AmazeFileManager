package com.amaze.filemanager.filesystem

import kotlin.random.Random

object RandomPathGenerator {
    val CHARS_FOR_PATH = ('A'..'Z').toList() + ('a'..'z').toList()
    val SEPARATOR = '/'

    fun generateRandomPath(random: Random, length: Int): String {
        val randomString = (1..length)
            .map { i -> random.nextInt(0, CHARS_FOR_PATH.count()) }
            .map(CHARS_FOR_PATH::get)

        val path = randomString.mapIndexed { i, e ->
            if (random.nextInt(10) < 1 && i > 0 && randomString[i-1] != SEPARATOR) {
                SEPARATOR
            } else {
                e
            }
        }

        return path.joinToString("")
    }

}