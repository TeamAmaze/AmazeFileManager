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

package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers

import android.content.Context
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.utils.UpdatePosition
import com.amaze.filemanager.filesystem.FileUtil
import com.amaze.filemanager.filesystem.MakeDirectoryOperation
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor
import com.amaze.filemanager.filesystem.files.GenericCopyUtil
import net.didion.loopy.AbstractBlockFileSystem
import net.didion.loopy.FileEntry
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException

abstract class AbstractIsoExtractor(
    context: Context,
    filePath: String,
    outputPath: String,
    listener: OnUpdate,
    updatePosition: UpdatePosition,
) : Extractor(context, filePath, outputPath, listener, updatePosition) {
    protected abstract val fileSystemImplementation: Class<out AbstractBlockFileSystem>

    override fun extractWithFilter(filter: Filter) {
        val isoFile =
            fileSystemImplementation.getDeclaredConstructor(
                File::class.java,
                Boolean::class.java,
            ).newInstance(File(filePath), true)
        var totalBytes = 0L
        // Quirk. AbstractBlockFileSystem.getEntries().hasNextElement() would return true even if
        // it's empty or nothing, by then it's already too late but had to catch NPE on our own
        val fileEntries: List<FileEntry> =
            runCatching {
                isoFile.entries?.let { isoFileEntries ->
                    isoFileEntries.toList().partition { entry ->
                        CompressedHelper.isEntryPathValid((entry as FileEntry).path)
                    }.let { pair ->
                        pair.first as List<FileEntry>
                    }
                } ?: throw IOException("Empty archive or file is corrupt")
            }.onFailure {
                throw BadArchiveNotice(it)
            }.getOrThrow()

        if (fileEntries.isNotEmpty()) {
            totalBytes = fileEntries.sumOf { it.size.toLong() }
            listener.onStart(totalBytes, fileEntries.first().name)
            fileEntries.forEach { entry ->
                if (!listener.isCancelled) {
                    listener.onUpdate(entry.name)
                    extractEntry(context, isoFile, entry, outputPath)
                }
            }
            listener.onFinish()
        } else {
            throw EmptyArchiveNotice()
        }
    }

    private fun extractEntry(
        context: Context,
        archive: AbstractBlockFileSystem,
        entry: FileEntry,
        outputDir: String,
    ) {
        val name =
            fixEntryName(entry.path).replace(
                "\\\\".toRegex(),
                CompressedHelper.SEPARATOR,
            )
        val outputFile = File(outputDir, name)
        if (!outputFile.canonicalPath.startsWith(outputDir)) {
            throw IOException("Incorrect RAR FileHeader path!")
        }
        if (entry.isDirectory) {
            MakeDirectoryOperation.mkdir(outputFile, context)
            outputFile.setLastModified(entry.lastModifiedTime)
            return
        }
        if (!outputFile.parentFile.exists()) {
            MakeDirectoryOperation.mkdir(outputFile.parentFile, context)
            outputFile.parentFile.setLastModified(entry.lastModifiedTime)
        }

        archive.getInputStream(entry).use { inputStream ->
            FileUtil.getOutputStream(outputFile, context)?.let { fileOutputStream ->
                BufferedOutputStream(fileOutputStream).run {
                    var len: Int
                    val buf = ByteArray(GenericCopyUtil.DEFAULT_BUFFER_SIZE)
                    while (inputStream.read(buf).also { len = it } != -1) {
                        if (!listener.isCancelled) {
                            write(buf, 0, len)
                            updatePosition.updatePosition(len.toLong())
                        } else {
                            break
                        }
                    }
                    close()
                    outputFile.setLastModified(entry.lastModifiedTime)
                }
            } ?: AppConfig.toast(
                context,
                context.getString(
                    R.string.error_archive_cannot_extract,
                    entry.path,
                    outputDir,
                ),
            )
        }
    }
}
