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
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.file_operations.utils.UpdatePosition
import com.amaze.filemanager.filesystem.FileUtil
import com.amaze.filemanager.filesystem.MakeDirectoryOperation
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor
import com.amaze.filemanager.filesystem.files.GenericCopyUtil
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class ZipExtractor(
    context: Context,
    filePath: String,
    outputPath: String,
    listener: OnUpdate,
    updatePosition: UpdatePosition
) : Extractor(context, filePath, outputPath, listener, updatePosition) {
    @Throws(IOException::class)
    override fun extractWithFilter(filter: Filter) {
        var totalBytes: Long = 0
        val entriesToExtract: MutableList<FileHeader> = ArrayList()
        try {
            val zipfile = ZipFile(filePath)
            if (ArchivePasswordCache.getInstance().containsKey(filePath)) {
                zipfile.setPassword(ArchivePasswordCache.getInstance()[filePath]!!.toCharArray())
            }

            // iterating archive elements to find file names that are to be extracted
            zipfile.fileHeaders.forEach { obj ->
                val fileHeader = obj as FileHeader
                if (CompressedHelper.isEntryPathValid(fileHeader.fileName)) {
                    if (filter.shouldExtract(fileHeader.fileName, fileHeader.isDirectory)) {
                        entriesToExtract.add(fileHeader)
                        totalBytes += fileHeader.uncompressedSize
                    }
                } else {
                    invalidArchiveEntries.add(fileHeader.fileName)
                }
            }
            if (entriesToExtract.size > 0) {
                listener.onStart(totalBytes, entriesToExtract[0].fileName)
                for (entry in entriesToExtract) {
                    if (!listener.isCancelled) {
                        listener.onUpdate(entry.fileName)
                        extractEntry(context, zipfile, entry, outputPath)
                    }
                }
            } else {
                throw EmptyArchiveNotice()
            }
            listener.onFinish()
        } catch (e: ZipException) {
            throw IOException(e)
        }
    }

    /**
     * Method extracts [FileHeader] from [ZipFile]
     *
     * @param zipFile zip file from which entriesToExtract are to be extracted
     * @param entry zip entry that is to be extracted
     * @param outputDir output directory
     */
    @Throws(IOException::class)
    private fun extractEntry(
        context: Context,
        zipFile: ZipFile,
        entry: FileHeader,
        outputDir: String
    ) {
        val outputFile = File(outputDir, fixEntryName(entry.fileName))
        if (!outputFile.canonicalPath.startsWith(outputDir)) {
            throw IOException("Incorrect ZipEntry path!")
        }
        if (entry.isDirectory) {
            // zip entry is a directory, return after creating new directory
            MakeDirectoryOperation.mkdir(outputFile, context)
            return
        }
        if (!outputFile.parentFile.exists()) {
            // creating directory if not already exists
            MakeDirectoryOperation.mkdir(outputFile.parentFile, context)
        }
        val inputStream = BufferedInputStream(zipFile.getInputStream(entry))
        FileUtil.getOutputStream(outputFile, context)?.let { fileOutputStream ->
            BufferedOutputStream(fileOutputStream).run {
                var len: Int
                val buf = ByteArray(GenericCopyUtil.DEFAULT_BUFFER_SIZE)
                while (inputStream.read(buf).also { len = it } != -1) {
                    if (!listener.isCancelled) {
                        write(buf, 0, len)
                        updatePosition.updatePosition(len.toLong())
                    } else break
                }
                close()
                outputFile.setLastModified(entry.lastModifiedTimeEpoch)
            }
        } ?: AppConfig.toast(
            context,
            context.getString(
                R.string.error_archive_cannot_extract,
                entry.fileName,
                outputDir
            )
        )
    }
}
