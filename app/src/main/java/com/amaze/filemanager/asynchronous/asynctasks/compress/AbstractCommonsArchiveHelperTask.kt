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
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*

abstract class AbstractCommonsArchiveHelperTask(
    context: Context,
    private val filePath: String,
    private val relativePath: String,
    goBack: Boolean,
    l: OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>>
) :
    CompressedHelperTask(goBack, l) {

    private val context: WeakReference<Context> = WeakReference(context)

    /**
     * Subclasses implement this method to create [ArchiveInputStream] instances with given archive
     * as [InputStream].
     *
     * @param inputStream archive as [InputStream]
     */
    abstract fun createFrom(inputStream: InputStream): ArchiveInputStream

    @Throws(ArchiveException::class)
    @Suppress("LabeledExpression")
    public override fun addElements(elements: ArrayList<CompressedObjectParcelable>) {
        var tarInputStream: ArchiveInputStream?
        try {
            tarInputStream = createFrom(FileInputStream(filePath))
            var entry: ArchiveEntry?
            while (tarInputStream.nextEntry.also { entry = it } != null) {
                entry?.run {
                    var name = name
                    if (!CompressedHelper.isEntryPathValid(name)) {
                        AppConfig.toast(
                            context.get(),
                            context.get()!!.getString(R.string.multiple_invalid_archive_entries)
                        )
                        return@run
                    }
                    if (name.endsWith(CompressedHelper.SEPARATOR)) {
                        name = name.substring(0, name.length - 1)
                    }
                    val isInBaseDir =
                        (relativePath == "" && !name.contains(CompressedHelper.SEPARATOR))
                    val isInRelativeDir = (
                        name.contains(CompressedHelper.SEPARATOR) &&
                            name.substring(0, name.lastIndexOf(CompressedHelper.SEPARATOR))
                            == relativePath
                        )
                    if (isInBaseDir || isInRelativeDir) {
                        elements.add(
                            CompressedObjectParcelable(
                                name,
                                lastModifiedDate.time,
                                size,
                                isDirectory
                            )
                        )
                    }
                }
            }
        } catch (e: IOException) {
            throw ArchiveException(String.format("Tarball archive %s is corrupt", filePath), e)
        }
    }
}
