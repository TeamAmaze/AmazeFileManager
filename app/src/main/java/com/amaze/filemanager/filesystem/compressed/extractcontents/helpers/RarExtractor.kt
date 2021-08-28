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

package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers

import android.content.Context
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.file_operations.utils.UpdatePosition
import com.amaze.filemanager.filesystem.FileUtil
import com.amaze.filemanager.filesystem.MakeDirectoryOperation
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor
import com.amaze.filemanager.filesystem.compressed.isPasswordProtectedCompat
import com.amaze.filemanager.filesystem.files.GenericCopyUtil
import com.github.junrar.Archive
import com.github.junrar.exception.CorruptHeaderException
import com.github.junrar.exception.RarException
import com.github.junrar.exception.UnsupportedRarV5Exception
import com.github.junrar.rarfile.FileHeader
import org.apache.commons.compress.PasswordRequiredException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream

class RarExtractor(
    context: Context,
    filePath: String,
    outputPath: String,
    listener: OnUpdate,
    updatePosition: UpdatePosition
) : Extractor(context, filePath, outputPath, listener, updatePosition) {

    @Throws(IOException::class)
    override fun extractWithFilter(filter: Filter) {
        try {
            var totalBytes: Long = 0
            val rarFile: Archive = runCatching {
                ArchivePasswordCache.getInstance()[filePath]?.let {
                    Archive(File(filePath), it).also { archive ->
                        archive.password = it
                    }
                } ?: Archive(File(filePath))
            }.onFailure {
                if (UnsupportedRarV5Exception::class.java.isAssignableFrom(it::class.java)) {
                    throw it
                } else {
                    throw PasswordRequiredException(filePath)
                }
            }.getOrNull()!!

            if (rarFile.isPasswordProtectedCompat() || rarFile.isEncrypted) {
                if (ArchivePasswordCache.getInstance().containsKey(filePath)) {
                    runCatching {
                        tryExtractSmallestFileInArchive(context, rarFile)
                    }.onFailure {
                        throw PasswordRequiredException(filePath)
                    }.onSuccess {
                        File(it).delete()
                    }
                } else {
                    throw PasswordRequiredException(filePath)
                }
            }

            val fileHeaders: List<FileHeader>
            // iterating archive elements to find file names that are to be extracted
            rarFile.fileHeaders.partition { header ->
                CompressedHelper.isEntryPathValid(header.fileName)
            }.apply {
                fileHeaders = first
                totalBytes = first.sumOf { it.fullUnpackSize }
                invalidArchiveEntries = second.map { it.fileName }
            }

            if (fileHeaders.isNotEmpty()) {
                listener.onStart(totalBytes, fileHeaders[0].fileName)
                fileHeaders.forEach { entry ->
                    if (!listener.isCancelled) {
                        listener.onUpdate(entry.fileName)
                        extractEntry(context, rarFile, entry, outputPath)
                    }
                }
                listener.onFinish()
            } else {
                throw EmptyArchiveNotice()
            }
        } catch (e: RarException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    private fun extractEntry(
        context: Context,
        rarFile: Archive,
        entry: FileHeader,
        outputDir: String
    ) {
        var _entry = entry
        val entrySpawnsVolumes = entry.isSplitAfter
        val name = fixEntryName(entry.fileName).replace(
            "\\\\".toRegex(),
            CompressedHelper.SEPARATOR
        )
        val outputFile = File(outputDir, name)
        if (!outputFile.canonicalPath.startsWith(outputDir)) {
            throw IOException("Incorrect RAR FileHeader path!")
        }
        if (entry.isDirectory) {
            MakeDirectoryOperation.mkdir(outputFile, context)
            outputFile.setLastModified(entry.mTime.time)
            return
        }
        if (!outputFile.parentFile.exists()) {
            MakeDirectoryOperation.mkdir(outputFile.parentFile, context)
            outputFile.parentFile.setLastModified(entry.mTime.time)
        }
        /* junrar doesn't throw exceptions if wrong archive password is supplied, until extracted file
           CRC is compared against the one stored in archive. So we can only rely on verifying CRC
           during extracting
        */
        val inputStream = BufferedInputStream(rarFile.getInputStream(entry))
        val outputStream = CheckedOutputStream(
            BufferedOutputStream(FileUtil.getOutputStream(outputFile, context)), CRC32()
        )
        try {
            var len: Int
            val buf = ByteArray(GenericCopyUtil.DEFAULT_BUFFER_SIZE)
            while (inputStream.read(buf).also { len = it } != -1) {
                if (!listener.isCancelled) {
                    outputStream.write(buf, 0, len)
                    ServiceWatcherUtil.position += len.toLong()
                } else break
            }
            /* In multi-volume archives, FileHeader may have changed as the other parts of the
               archive is processed. Need to lookup the FileHeader in the volume the archive
               currently resides on again, as the correct CRC of the extract file will be there
               instead.
             */
            if (entrySpawnsVolumes) {
                _entry = rarFile.fileHeaders.find { it.fileName.equals(entry.fileName) }!!
            }

            /* junrar does not provide convenient way to verify archive password is correct, we
               can only rely on post-extract file checksum matching to see if the file is
               extracted correctly = correct password.

               RAR header stores checksum in signed 2's complement (as hex though). Some bitwise
               ops needed to compare with CheckOutputStream used above, which always produces
               checksum in unsigned long
             */
            if (_entry.fileCRC.toLong() and 0xffffffffL != outputStream.checksum.value) {
                throw IOException("Checksum verification failed for entry $name")
            }
        } finally {
            outputStream.close()
            inputStream.close()
            outputFile.setLastModified(entry.mTime.time)
        }
    }

    private fun tryExtractSmallestFileInArchive(context: Context, archive: Archive): String {
        archive.fileHeaders ?: throw IOException(CorruptHeaderException())
        with(
            archive.fileHeaders.filter {
                !it.isDirectory
            }
        ) {
            if (isEmpty()) {
                throw IOException(CorruptHeaderException())
            } else {
                associateBy({ it.fileName }, { it.fullUnpackSize })
                    .minByOrNull {
                        it.value
                    }!!.run {
                    val header = archive.fileHeaders.find {
                        it.fileName.equals(this.key)
                    }!!
                    val filename = fixEntryName(header.fileName).replace(
                        "\\\\".toRegex(),
                        CompressedHelper.SEPARATOR
                    )
                    extractEntry(context, archive, header, context.externalCacheDir!!.absolutePath)
                    return "${context.externalCacheDir!!.absolutePath}/$filename"
                }
            }
        }
    }
}
