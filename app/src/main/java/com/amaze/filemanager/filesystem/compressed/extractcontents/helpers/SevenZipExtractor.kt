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
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.fileoperations.utils.UpdatePosition
import com.amaze.filemanager.filesystem.FileUtil
import com.amaze.filemanager.filesystem.MakeDirectoryOperation
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZArchiveEntry
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZFile
import com.amaze.filemanager.filesystem.files.GenericCopyUtil
import org.apache.commons.compress.PasswordRequiredException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tukaani.xz.CorruptedInputException
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException

class SevenZipExtractor(
    context: Context,
    filePath: String,
    outputPath: String,
    listener: OnUpdate,
    updatePosition: UpdatePosition
) :
    Extractor(context, filePath, outputPath, listener, updatePosition) {

    companion object {
        @JvmStatic
        private val LOG: Logger = LoggerFactory.getLogger(SevenZipExtractor::class.java)
    }

    @Throws(IOException::class)
    override fun extractWithFilter(filter: Filter) {
        var totalBytes: Long = 0
        val sevenzFile = runCatching {
            if (ArchivePasswordCache.getInstance().containsKey(filePath)) {
                SevenZFile(
                    File(filePath),
                    ArchivePasswordCache.getInstance()[filePath]!!.toCharArray()
                )
            } else {
                SevenZFile(File(filePath))
            }
        }.getOrElse {
            if (it is PasswordRequiredException || it is CorruptedInputException) {
                throw it
            } else {
                throw BadArchiveNotice(it)
            }
        }
        val arrayList = ArrayList<SevenZArchiveEntry>()

        // iterating archive elements to find file names that are to be extracted
        for (entry in sevenzFile.entries) {
            if (filter.shouldExtract(entry.name, entry.isDirectory)) {
                // Entry to be extracted is at least the entry path
                // (may be more, when it is a directory)
                arrayList.add(entry)
                totalBytes += entry.size
            }
        }
        listener.onStart(totalBytes, arrayList[0].name)
        var entry: SevenZArchiveEntry?
        while (sevenzFile.nextEntry.also { entry = it } != null) {
            if (!arrayList.contains(entry)) {
                continue
            }
            if (!listener.isCancelled) {
                listener.onUpdate(entry!!.name)
                extractEntry(context, sevenzFile, entry!!, outputPath)
            }
        }
        sevenzFile.close()
        listener.onFinish()
    }

    @Throws(IOException::class)
    private fun extractEntry(
        context: Context,
        sevenzFile: SevenZFile,
        entry: SevenZArchiveEntry,
        outputDir: String
    ) {
        val name = entry.name
        if (entry.isDirectory) {
            MakeDirectoryOperation.mkdir(File(outputDir, name), context)
            return
        }
        val outputFile = File(outputDir, name)
        if (!outputFile.parentFile.exists()) {
            MakeDirectoryOperation.mkdir(outputFile.parentFile, context)
        }
        FileUtil.getOutputStream(outputFile, context)?.let { fileOutputStream ->
            BufferedOutputStream(fileOutputStream).runCatching {
                val content = ByteArray(GenericCopyUtil.DEFAULT_BUFFER_SIZE)
                var progress: Long = 0
                while (progress < entry.size) {
                    var length: Int
                    val bytesLeft = java.lang.Long.valueOf(entry.size - progress).toInt()
                    length = sevenzFile.read(
                        content,
                        0,
                        if (bytesLeft > GenericCopyUtil.DEFAULT_BUFFER_SIZE) {
                            GenericCopyUtil.DEFAULT_BUFFER_SIZE
                        } else {
                            bytesLeft
                        }
                    )
                    write(content, 0, length)
                    updatePosition.updatePosition(length.toLong())
                    progress += length.toLong()
                }
                close()
                val lastModifiedDate = try {
                    entry.lastModifiedDate.time
                } catch (e: UnsupportedOperationException) {
                    LOG.warn("Unable to get modified date for 7zip file")
                    System.currentTimeMillis()
                }
                outputFile.setLastModified(lastModifiedDate)
            }
        }?.onFailure {
            throw it
        } ?: AppConfig.toast(
            context,
            context.getString(
                R.string.error_archive_cannot_extract,
                entry.name,
                outputDir
            )
        )
    }
}
