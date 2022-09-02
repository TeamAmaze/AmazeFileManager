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

package com.amaze.filemanager.asynchronous.asynctasks.compress

import android.util.Log
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.fileoperations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZArchiveEntry
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZFile
import org.apache.commons.compress.PasswordRequiredException
import org.apache.commons.compress.archivers.ArchiveException
import java.io.File
import java.io.IOException
import java.lang.UnsupportedOperationException

class SevenZipHelperCallable(
    private val filePath: String,
    private val relativePath: String,
    goBack: Boolean
) :
    CompressedHelperCallable(goBack) {

    @Throws(ArchiveException::class)
    @Suppress("Detekt.RethrowCaughtException")
    override fun addElements(elements: ArrayList<CompressedObjectParcelable>) {
        try {
            val sevenzFile = if (ArchivePasswordCache.getInstance().containsKey(filePath)) {
                SevenZFile(
                    File(filePath),
                    ArchivePasswordCache.getInstance()[filePath]!!.toCharArray()
                )
            } else {
                SevenZFile(File(filePath))
            }
            val strings = ArrayList<String>()
            for (entry in sevenzFile.entries) {
                val file = File(entry.name)
                val y = entry.name.let {
                    if (it.startsWith(CompressedHelper.SEPARATOR)) {
                        it.substring(1, it.length)
                    } else {
                        it
                    }
                }
                if (relativePath.trim { it <= ' ' }.isEmpty()) {
                    var path: String
                    var zipObj: CompressedObjectParcelable
                    if (file.parent == null || file.parent!!.isEmpty() || file.parent == "/") {
                        path = y
                        zipObj = CompressedObjectParcelable(
                            y,
                            getEntryDate(entry),
                            entry.size,
                            entry.isDirectory
                        )
                    } else {
                        path = y.substring(0, y.indexOf(CompressedHelper.SEPARATOR) + 1)
                        zipObj = CompressedObjectParcelable(
                            path,
                            getEntryDate(entry),
                            entry.size,
                            true
                        )
                    }
                    if (!strings.contains(path)) {
                        elements.add(zipObj)
                        strings.add(path)
                    }
                } else {
                    if (file.parent != null &&
                        (
                                file.parent == relativePath ||
                                        file.parent == "/$relativePath"
                                )
                    ) {
                        if (!strings.contains(y)) {
                            elements.add(
                                CompressedObjectParcelable(
                                    y,
                                    getEntryDate(entry),
                                    entry.size,
                                    entry.isDirectory
                                )
                            )
                            strings.add(y)
                        }
                    } else if (y.startsWith("$relativePath/") &&
                        y.length > relativePath.length + 1
                    ) {
                        val path1 = y.substring(relativePath.length + 1, y.length)
                        val index = relativePath.length + 1 + path1.indexOf("/")
                        val path = y.substring(0, index + 1)
                        if (!strings.contains(path)) {
                            elements.add(
                                CompressedObjectParcelable(
                                    y.substring(0, index + 1),
                                    getEntryDate(entry),
                                    entry.size,
                                    true
                                )
                            )
                            strings.add(path)
                        }
                    }
                }
            }
        } catch (e: PasswordRequiredException) {
            // this is so that the caller can use onError to ask the user for the password
            throw e
        } catch (e: IOException) {
            throw ArchiveException(String.format("7zip archive %s is corrupt", filePath))
        }
    }

    private fun getEntryDate(entry: SevenZArchiveEntry) =
        try {
            entry.lastModifiedDate.time
        } catch (e: UnsupportedOperationException) {
            Log.w(
                javaClass.simpleName,
                "Unable to get modified date for 7zip file"
            )
            0L
        }
}
