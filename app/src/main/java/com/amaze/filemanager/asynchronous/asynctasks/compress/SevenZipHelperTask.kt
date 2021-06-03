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

import android.widget.EditText
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZFile
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import org.apache.commons.compress.PasswordRequiredException
import org.apache.commons.compress.archivers.ArchiveException
import org.tukaani.xz.CorruptedInputException
import java.io.File
import java.io.IOException
import java.util.*

class SevenZipHelperTask(
    private val filePath: String,
    private val relativePath: String,
    goBack: Boolean,
    l: OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>>
) :
    CompressedHelperTask(goBack, l) {

    private var paused = false
    @Throws(ArchiveException::class)
    override fun addElements(elements: ArrayList<CompressedObjectParcelable>) {
        while (true) {
            if (paused) continue
            try {
                val sevenzFile = if (ArchivePasswordCache.getInstance().containsKey(filePath)) {
                    SevenZFile(
                        File(filePath),
                        ArchivePasswordCache.getInstance()[filePath]!!.toCharArray()
                    )
                } else {
                    SevenZFile(File(filePath))
                }
                for (entry in sevenzFile.entries) {
                    val name = entry.name
                    val isInBaseDir = (
                        relativePath == "" &&
                            !name.contains(CompressedHelper.SEPARATOR)
                        )
                    val isInRelativeDir = (
                        name.contains(CompressedHelper.SEPARATOR) &&
                            name.substring(0, name.lastIndexOf(CompressedHelper.SEPARATOR))
                            == relativePath
                        )
                    if (isInBaseDir || isInRelativeDir) {
                        elements.add(
                            CompressedObjectParcelable(
                                entry.name,
                                entry.lastModifiedDate.time,
                                entry.size,
                                entry.isDirectory
                            )
                        )
                    }
                }
                paused = false
                break
            } catch (e: PasswordRequiredException) {
                paused = true
                publishProgress(e)
            } catch (e: IOException) {
                throw ArchiveException(String.format("7zip archive %s is corrupt", filePath))
            }
        }
    }

    override fun onProgressUpdate(vararg values: IOException) {
        super.onProgressUpdate(*values)
        if (values.isEmpty()) return
        val result = values[0]
        // We only handle PasswordRequiredException here.
        if (result is PasswordRequiredException || result is CorruptedInputException) {
            ArchivePasswordCache.getInstance().remove(filePath)
            GeneralDialogCreation.showPasswordDialog(
                AppConfig.getInstance().mainActivityContext!!,
                (AppConfig.getInstance().mainActivityContext as MainActivity?)!!,
                AppConfig.getInstance().utilsProvider.appTheme,
                R.string.archive_password_prompt,
                R.string.authenticate_password,
                { dialog: MaterialDialog, _: DialogAction? ->
                    val editText = dialog.view.findViewById<EditText>(R.id.singleedittext_input)
                    val password = editText.text.toString()
                    ArchivePasswordCache.getInstance()[filePath] = password
                    paused = false
                    dialog.dismiss()
                },
                null
            )
        }
    }
}
