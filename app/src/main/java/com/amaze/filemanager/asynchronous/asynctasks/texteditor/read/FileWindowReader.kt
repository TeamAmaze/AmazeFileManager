/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.read

import android.content.ContentResolver
import android.net.Uri
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Seekable file reader that supports reading a window of characters from a file,
 * snapping to UTF-8 character boundaries and line boundaries.
 *
 * All I/O methods are regular (non-suspend) functions — the caller is responsible
 * for dispatching to Dispatchers.IO.
 */
class FileWindowReader(
    private val channel: FileChannel,
    private val charset: Charset = Charsets.UTF_8,
    val fileSize: Long,
    private val closeable: Closeable? = null,
) : Closeable {
    data class WindowResult(
        val text: String,
        /** Actual start byte offset (snapped to char/line boundary). */
        val startByte: Long,
        /** Byte offset after the last byte read. */
        val endByte: Long,
        val isStartOfFile: Boolean,
        val isEndOfFile: Boolean,
    )

    /**
     * Read a window of text starting at approximately [byteOffset].
     *
     * The method:
     * 1. Adjusts the offset to land on a UTF-8 character boundary
     * 2. Reads enough bytes to decode up to [maxChars] characters
     * 3. Snaps the start to the first newline (unless at file start)
     * 4. Snaps the end to the last newline (unless at file end)
     * 5. Returns the decoded string and actual byte range consumed
     */
    @Suppress("LongMethod")
    fun readWindow(
        byteOffset: Long,
        maxChars: Int,
    ): WindowResult {
        val safeOffset = snapToCharBoundary(byteOffset.coerceIn(0L, fileSize))

        // Read enough bytes for worst-case UTF-8: maxChars * 4
        val maxBytes = (maxChars.toLong() * 4).coerceAtMost(fileSize - safeOffset)
        if (maxBytes <= 0) {
            return WindowResult(
                text = "",
                startByte = safeOffset,
                endByte = safeOffset,
                isStartOfFile = safeOffset == 0L,
                isEndOfFile = true,
            )
        }

        val buffer = ByteBuffer.allocate(maxBytes.toInt())
        synchronized(channel) {
            channel.position(safeOffset)
            var totalRead = 0
            while (totalRead < maxBytes) {
                val read = channel.read(buffer)
                if (read == -1) break
                totalRead += read
            }
        }
        buffer.flip()

        val actualBytesRead = buffer.remaining()
        if (actualBytesRead == 0) {
            return WindowResult(
                text = "",
                startByte = safeOffset,
                endByte = safeOffset,
                isStartOfFile = safeOffset == 0L,
                isEndOfFile = true,
            )
        }

        // Decode bytes to string
        val decoder =
            charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)

        val decoded = decoder.decode(buffer).toString()

        // Trim to maxChars
        val trimmed = if (decoded.length > maxChars) decoded.substring(0, maxChars) else decoded

        // Calculate the byte length of the trimmed text
        val trimmedBytes = trimmed.toByteArray(charset)

        val isAtStart = safeOffset == 0L
        val isAtEnd = safeOffset + trimmedBytes.size >= fileSize

        // Snap to line boundaries
        var text = trimmed
        var startByteAdjust = 0

        // Snap start: if not at file start, skip to after first newline
        if (!isAtStart) {
            val firstNewline = text.indexOf('\n')
            if (firstNewline >= 0 && firstNewline < text.length - 1) {
                val skipped = text.substring(0, firstNewline + 1)
                startByteAdjust = skipped.toByteArray(charset).size
                text = text.substring(firstNewline + 1)
            }
        }

        // Snap end: if not at file end, trim to last newline
        if (!isAtEnd) {
            val lastNewline = text.lastIndexOf('\n')
            if (lastNewline >= 0) {
                text = text.substring(0, lastNewline + 1)
            }
        }

        val actualStartByte = safeOffset + startByteAdjust
        val actualEndByte = actualStartByte + text.toByteArray(charset).size

        return WindowResult(
            text = text,
            startByte = actualStartByte,
            endByte = actualEndByte.coerceAtMost(fileSize),
            isStartOfFile = actualStartByte == 0L,
            isEndOfFile = actualEndByte >= fileSize,
        )
    }

    /**
     * Snap a byte offset to a UTF-8 character boundary by backing up
     * past any continuation bytes (10xxxxxx pattern).
     */
    private fun snapToCharBoundary(offset: Long): Long {
        if (offset !in 1..<fileSize) return offset.coerceIn(0L, fileSize)

        val buf = ByteBuffer.allocate(1)
        var pos = offset
        // Back up at most 3 bytes (max UTF-8 continuation)
        val minPos = (offset - 3).coerceAtLeast(0L)

        while (pos > minPos) {
            synchronized(channel) {
                channel.position(pos)
                buf.clear()
                channel.read(buf)
            }
            buf.flip()
            val b = buf.get().toInt() and 0xFF
            // If this byte is NOT a continuation byte, we're at a char boundary
            if (b and 0xC0 != 0x80) {
                return pos
            }
            pos--
        }
        return pos
    }

    override fun close() {
        try {
            channel.close()
        } catch (_: Exception) {
        }
        try {
            closeable?.close()
        } catch (_: Exception) {
        }
    }

    companion object {
        /**
         * Create a FileWindowReader from a regular File using RandomAccessFile.
         */
        fun fromFile(file: File): FileWindowReader {
            val raf = RandomAccessFile(file, "r")
            return FileWindowReader(
                channel = raf.channel,
                fileSize = raf.length(),
                closeable = raf,
            )
        }

        /**
         * Create a FileWindowReader from a content:// URI using ContentResolver.
         */
        fun fromContentUri(
            contentResolver: ContentResolver,
            uri: Uri,
        ): FileWindowReader {
            val pfd =
                contentResolver.openFileDescriptor(uri, "r")
                    ?: throw IllegalArgumentException("Cannot open file descriptor for URI: $uri")
            val fis = FileInputStream(pfd.fileDescriptor)
            val channel = fis.channel
            val size = channel.size()
            return FileWindowReader(
                channel = channel,
                fileSize = size,
                closeable =
                    Closeable {
                        try {
                            fis.close()
                        } catch (_: Exception) {
                        }
                        try {
                            pfd.close()
                        } catch (_: Exception) {
                        }
                    },
            )
        }
    }
}
