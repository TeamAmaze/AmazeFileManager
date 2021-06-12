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

import android.content.Context
import android.net.Uri
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import org.apache.commons.compress.archivers.ArchiveException
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class ZipHelperTask(
    c: Context,
    realFileDirectory: String,
    dir: String?,
    goback: Boolean,
    l: OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>>
) :
    CompressedHelperTask(goback, l) {

    private val context: WeakReference<Context> = WeakReference(c)
    private val fileLocation: Uri = Uri.parse(realFileDirectory)
    private val relativeDirectory: String? = dir

    @Throws(ArchiveException::class)
    @Suppress("ComplexMethod", "LongMethod")
    public override fun addElements(elements: ArrayList<CompressedObjectParcelable>) = try {
        fileLocation.path?.run {
            val zipfile = ZipFile(fileLocation.path)
            val wholelist = filterValidEntryList(zipfile)
            val strings = ArrayList<String>()
            for (entry in wholelist) {
                val file = File(entry.path)
                val y = entry.path.let {
                    if (it.startsWith("/")) {
                        it.substring(1, it.length)
                    } else {
                        it
                    }
                }
                if (relativeDirectory == null || relativeDirectory.trim { it <= ' ' }.isEmpty()) {
                    var path: String
                    var zipObj: CompressedObjectParcelable
                    if (file.parent == null || file.parent!!.isEmpty() || file.parent == "/") {
                        path = y
                        zipObj = CompressedObjectParcelable(
                            y,
                            entry.date,
                            entry.size,
                            entry.directory
                        )
                    } else {
                        path = y.substring(0, y.indexOf("/") + 1)
                        zipObj = CompressedObjectParcelable(
                            path,
                            entry.date,
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
                            file.parent == relativeDirectory ||
                                file.parent == "/$relativeDirectory"
                            )
                    ) {
                        if (!strings.contains(y)) {
                            elements.add(
                                CompressedObjectParcelable(
                                    y,
                                    entry.date,
                                    entry.size,
                                    entry.directory
                                )
                            )
                            strings.add(y)
                        }
                    } else if (y.startsWith("$relativeDirectory/") &&
                        y.length > relativeDirectory.length + 1
                    ) {
                        val path1 = y.substring(relativeDirectory.length + 1, y.length)
                        val index = relativeDirectory.length + 1 + path1.indexOf("/")
                        val path = y.substring(0, index + 1)
                        if (!strings.contains(path)) {
                            elements.add(
                                CompressedObjectParcelable(
                                    y.substring(0, index + 1),
                                    entry.date,
                                    entry.size,
                                    true
                                )
                            )
                            strings.add(path)
                        }
                    }
                }
            }
        } ?: throw ArchiveException(null)
    } catch (e: ZipException) {
        throw ArchiveException("Zip file is corrupt", e)
    }

    private fun filterValidEntryList(zipFile: ZipFile): List<CompressedObjectParcelable> {
        val retval = ArrayList<CompressedObjectParcelable>()
        val headers: Iterator<FileHeader> = zipFile.fileHeaders.iterator()
        while (headers.hasNext()) {
            val entry = headers.next()
            if (!CompressedHelper.isEntryPathValid(entry.fileName)) {
                AppConfig.toast(
                    context.get(),
                    context.get()!!.getString(R.string.multiple_invalid_archive_entries)
                )
                continue
            }
            retval.add(
                CompressedObjectParcelable(
                    entry.fileName,
                    entry.lastModifiedTimeEpoch,
                    entry.uncompressedSize,
                    entry.isDirectory
                )
            )
        }
        return retval
    }
}
