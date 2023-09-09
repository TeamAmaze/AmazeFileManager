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

package com.amaze.filemanager.filesystem

import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.SLASH
import kotlin.math.absoluteValue

/**
 * Convenient extension to return path element of a path string = the part before the last slash.
 */
fun String.pathDirname(): String = if (contains(SLASH)) {
    substringBeforeLast(SLASH)
} else {
    ""
}

/**
 * Convenient extension to return the name element of a path = the part after the last slash.
 */
fun String.pathBasename(): String = if (contains(SLASH)) {
    substringAfterLast(SLASH)
} else {
    this
}

/**
 * Convenient extension to return the basename element of a filename = the part after the last
 * slash and before the extension (.).
 */
fun String.pathFileBasename(): String = if (contains('.')) {
    pathBasename().substringBeforeLast('.')
} else {
    pathBasename()
}

/**
 * Convenient extension to return the extension element of a filename = the part after the last
 * slash and after the extension (.). Returns empty string if no extension dot exist.
 */
fun String.pathFileExtension(): String = if (contains('.')) {
    pathBasename().substringAfterLast('.')
} else {
    ""
}

enum class FilenameFormatFlag {
    DARWIN, DEFAULT, WINDOWS, LINUX
}

object FilenameHelper {

    /* Don't split complex regexs into multiple lines. */

    /* ktlint-disable max-line-length */
    private const val REGEX_RAW_NUMBERS = "| [0-9]+"
    private const val REGEX_SOURCE = " \\((?:(another|[0-9]+(th|st|nd|rd)) )?copy\\)|copy( [0-9]+)?|\\.\\(incomplete\\)| \\([0-9]+\\)|[- ]+"
    /* ktlint-enable max-line-length */

    private val ordinals = arrayOf("th", "st", "nd", "rd")

    /**
     * Strip the file path to one without increments or numbers.
     *
     * Default will not strip the raw numbers; specify removeRawNumbers = true to do so.
     */
    @JvmStatic
    fun strip(input: String, removeRawNumbers: Boolean = false): String {
        val filepath = stripIncrementInternal(input, removeRawNumbers)
        val extension = filepath.pathFileExtension()
        val dirname = stripIncrementInternal(filepath.pathDirname(), removeRawNumbers)
        val stem = stem(filepath, removeRawNumbers)
        return StringBuilder().run {
            if (dirname.isNotBlank()) {
                append(dirname).append(SLASH)
            }
            append(stem)
            if (extension.isNotBlank()) {
                append('.').append(extension)
            }
            toString()
        }
    }

    /**
     * Returns the ordinals of the given number. So that
     *
     * - toOrdinal(1) returns "1st"
     * - toOrdinal(2) returns "2nd"
     * - toOrdinal(10) returns "10th"
     * - toOrdinal(11) returns "11th"
     * - toOrdinal(12) returns "12th"
     * - toOrdinal(21) returns "21st"
     * - toOrdinal(22) returns "22nd"
     * - toOrdinal(23) returns "23rd"
     *
     * etc.
     */
    @JvmStatic
    fun toOrdinal(n: Int): String = "$n${ordinal(n.absoluteValue)}"

    /**
     * Increment the filename of a given [HybridFile].
     *
     * Uses [HybridFile.exists] to check file existence and if it exists, returns a HybridFile
     * with new filename which does not exist.
     */
    @JvmStatic
    fun increment(
        file: HybridFile,
        platform: FilenameFormatFlag = FilenameFormatFlag.DEFAULT,
        strip: Boolean = true,
        removeRawNumbers: Boolean = false,
        startArg: Int = 1
    ): HybridFile {
        var filename = file.getName(AppConfig.getInstance())
        var dirname = file.path.pathDirname()
        var basename = filename.pathFileBasename()
        val extension = filename.pathFileExtension()

        var start: Int = startArg

        if (strip) {
            filename = stripIncrementInternal(filename, removeRawNumbers)
            dirname = stripIncrementInternal(dirname, removeRawNumbers)
            basename = strip(basename, removeRawNumbers)
        }

        var retval = HybridFile(
            file.mode,
            dirname,
            filename,
            file.isDirectory(AppConfig.getInstance())
        )

        while (retval.exists(AppConfig.getInstance())) {
            filename = if (extension.isNotBlank()) {
                format(platform, basename, start++) + ".$extension"
            } else {
                format(platform, basename, start++)
            }
            retval = HybridFile(
                file.mode,
                dirname,
                filename,
                file.isDirectory(AppConfig.getInstance())
            )
        }

        return retval
    }

    private fun stripIncrementInternal(input: String, removeRawNumbers: Boolean = false): String {
        val source = StringBuilder().run {
            append(REGEX_SOURCE)
            if (removeRawNumbers) {
                append(REGEX_RAW_NUMBERS)
            }
            toString()
        }
        return Regex("($source)+$", RegexOption.IGNORE_CASE).replace(input, "")
    }

    private fun stem(filepath: String, removeRawNumbers: Boolean = false): String {
        val extension = filepath.pathFileExtension()
        return stripIncrementInternal(
            filepath.pathBasename().substringBefore(".$extension"),
            removeRawNumbers
        )
    }

    private fun ordinal(n: Int): String {
        var retval = ordinals.getOrNull(((n % 100) - 20) % 10)
        if (retval == null) {
            retval = ordinals.getOrNull(n % 100)
        }
        if (retval == null) {
            retval = ordinals[0]
        }
        return retval
    }

    // TODO: i18n
    private fun format(flag: FilenameFormatFlag, stem: String, n: Int): String {
        return when (flag) {
            FilenameFormatFlag.DARWIN -> {
                if (n == 1) {
                    "$stem copy"
                } else if (n > 1) {
                    "$stem copy $n"
                } else {
                    stem
                }
            }
            FilenameFormatFlag.LINUX -> {
                when (n) {
                    0 -> {
                        stem
                    }
                    1 -> {
                        "$stem (copy)"
                    }
                    2 -> {
                        "$stem (another copy)"
                    }
                    else -> {
                        "$stem (${toOrdinal(n)} copy)"
                    }
                }
            }
            // Windows and default formatting are the same.
            else -> {
                if (n >= 1) {
                    "$stem ($n)"
                } else {
                    stem
                }
            }
        }
    }
}
