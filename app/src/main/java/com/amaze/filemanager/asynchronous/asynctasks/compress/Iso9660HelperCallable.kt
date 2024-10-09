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

package com.amaze.filemanager.asynchronous.asynctasks.compress

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor
import net.didion.loopy.FileEntry
import net.didion.loopy.iso9660.ISO9660FileEntry
import net.didion.loopy.iso9660.ISO9660FileSystem
import java.io.File
import java.io.IOException
import java.lang.reflect.Field

class Iso9660HelperCallable(
    private val filePath: String,
    private val relativePath: String,
    createBackItem: Boolean,
) : CompressedHelperCallable(createBackItem) {
    private val SLASH = Regex("/")

    // Hack. ISO9660FileEntry doesn't have getter for parentPath, we need to read it on our own
    private val parentPathField: Field =
        ISO9660FileEntry::class.java.getDeclaredField("parentPath").also {
            it.isAccessible = true
        }

    override fun addElements(elements: ArrayList<CompressedObjectParcelable>) {
        val isoFile = ISO9660FileSystem(File(filePath), true)

        val fileEntries: List<FileEntry> =
            runCatching {
                isoFile.entries?.let { isoFileEntries ->
                    isoFileEntries.runCatching {
                        isoFileEntries.toList().partition { entry ->
                            CompressedHelper.isEntryPathValid((entry as FileEntry).path)
                        }.let { pair ->
                            pair.first as List<FileEntry>
                        }
                    }.onFailure {
                        return
                    }.getOrThrow()
                } ?: throw IOException("Empty archive or file is corrupt")
            }.onFailure {
                throw Extractor.BadArchiveNotice(it)
            }.getOrThrow().filter {
                it.name != "."
            }

        val slashCount =
            if (relativePath == "") {
                0
            } else {
                SLASH.findAll("$relativePath/").count()
            }

        fileEntries.filter {
            val parentPath = parentPathField.get(it)?.toString() ?: ""
            (
                if (slashCount == 0) {
                    parentPath == ""
                } else {
                    parentPath == "$relativePath/"
                }
            )
        }.forEach { entry ->
            elements.add(
                CompressedObjectParcelable(
                    entry.name,
                    entry.lastModifiedTime,
                    entry.size.toLong(),
                    entry.isDirectory,
                ),
            )
        }
    }
}
