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

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor.Companion.convertName
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import com.github.junrar.exception.UnsupportedRarV5Exception
import org.apache.commons.compress.archivers.ArchiveException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * AsyncTask to load RAR file items.
 *
 * @param realFileDirectory the location of the zip file
 * @param dir relativeDirectory to access inside the zip file
 */
class RarHelperTask(
    private val fileLocation: String,
    private val relativeDirectory: String,
    goBack: Boolean,
    l: OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>>
) :
    CompressedHelperTask(goBack, l) {

    @Throws(ArchiveException::class)
    override fun addElements(elements: ArrayList<CompressedObjectParcelable>) {
        try {
            val zipfile = Archive(File(fileLocation))
            val relativeDirDiffSeparator = relativeDirectory.replace(
                CompressedHelper.SEPARATOR, "\\"
            )
            for (rarArchive in zipfile.fileHeaders) {
                val name = rarArchive.fileName
                if (!CompressedHelper.isEntryPathValid(name)) {
                    continue
                }
                val isInBaseDir = (
                    (relativeDirDiffSeparator == null || relativeDirDiffSeparator == "") &&
                        !name.contains("\\")
                    )
                val isInRelativeDir = (
                    relativeDirDiffSeparator != null && name.contains("\\") &&
                        name.substring(0, name.lastIndexOf("\\"))
                        == relativeDirDiffSeparator
                    )
                if (isInBaseDir || isInRelativeDir) {
                    elements.add(
                        CompressedObjectParcelable(
                            convertName(rarArchive),
                            rarArchive.mTime.time,
                            rarArchive.fullUnpackSize,
                            rarArchive.isDirectory
                        )
                    )
                }
            }
        } catch (e: UnsupportedRarV5Exception) {
            throw ArchiveException("RAR v5 archives are not supported", e)
        } catch (e: FileNotFoundException) {
            throw ArchiveException("First part of multipart archive not found", e)
        } catch (e: RarException) {
            throw ArchiveException(String.format("RAR archive %s is corrupt", fileLocation))
        } catch (e: IOException) {
            throw ArchiveException(String.format("RAR archive %s is corrupt", fileLocation))
        }
    }
}
