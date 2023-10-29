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

package com.amaze.filemanager.asynchronous.asynctasks.movecopy

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.Task
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.asynchronous.services.CopyService
import com.amaze.filemanager.database.CryptHandler
import com.amaze.filemanager.database.models.explorer.EncryptedEntry
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.files.MediaConnectionUtils
import com.amaze.filemanager.ui.activities.MainActivity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class MoveFilesReturn(
    val movedCorrectly: Boolean,
    val invalidOperation: Boolean,
    val destinationSize: Long,
    val totalSize: Long
)

class MoveFilesTask(
    val files: ArrayList<ArrayList<HybridFileParcelable>>,
    val isRootExplorer: Boolean,
    val currentPath: String,
    context: Context,
    val mode: OpenMode,
    val paths: ArrayList<String>
) : Task<MoveFilesReturn, MoveFiles> {

    private val log: Logger = LoggerFactory.getLogger(MoveFilesTask::class.java)

    private val task: MoveFiles = MoveFiles(files, isRootExplorer, context, mode, paths)
    private val applicationContext: Context = context.applicationContext

    override fun getTask(): MoveFiles = task

    override fun onError(error: Throwable) {
        log.error("Unexpected error on file move: ", error)
    }

    override fun onFinish(value: MoveFilesReturn) {
        val (movedCorrectly, invalidOperation, destinationSize, totalBytes) = value

        if (movedCorrectly) {
            onMovedCorrectly(invalidOperation)
        } else {
            onMovedFail(destinationSize, totalBytes)
        }
    }

    private fun onMovedCorrectly(invalidOperation: Boolean) {
        if (currentPath == paths[0]) {
            // mainFrag.updateList();
            val intent = Intent(MainActivity.KEY_INTENT_LOAD_LIST)
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, paths[0])
            applicationContext.sendBroadcast(intent)
        }

        if (invalidOperation) {
            Toast.makeText(
                applicationContext,
                R.string.some_files_failed_invalid_operation,
                Toast.LENGTH_LONG
            )
                .show()
        }

        for (i in paths.indices) {
            val targetFiles: MutableList<HybridFile> = ArrayList()
            val sourcesFiles: MutableList<HybridFileParcelable> = ArrayList()
            for (f in files[i]) {
                val file = HybridFile(
                    OpenMode.FILE,
                    paths[i] + "/" + f.getName(applicationContext)
                )
                targetFiles.add(file)
            }
            for (hybridFileParcelables in files) {
                sourcesFiles.addAll(hybridFileParcelables)
            }
            MediaConnectionUtils.scanFile(applicationContext, sourcesFiles.toTypedArray())
            MediaConnectionUtils.scanFile(applicationContext, targetFiles.toTypedArray())
        }

        // updating encrypted db entry if any encrypted file was moved
        AppConfig.getInstance()
            .runInBackground {
                for (i in paths.indices) {
                    for (file in files[i]) {
                        if (file.getName(applicationContext).endsWith(CryptUtil.CRYPT_EXTENSION)) {
                            val oldEntry = CryptHandler.findEntry(file.path)
                            if (oldEntry != null) {
                                val newEntry = EncryptedEntry()
                                newEntry.id = oldEntry.id
                                newEntry.password = oldEntry.password
                                newEntry.path = paths[i] + "/" + file.getName(applicationContext)
                                CryptHandler.updateEntry(oldEntry, newEntry)
                            }
                        }
                    }
                }
            }
    }

    private fun onMovedFail(destinationSize: Long, totalBytes: Long) {
        if (totalBytes > 0 && destinationSize < totalBytes) {
            // destination don't have enough space; return
            Toast.makeText(
                applicationContext,
                applicationContext.resources.getString(R.string.in_safe),
                Toast.LENGTH_LONG
            )
                .show()
            return
        }
        for (i in paths.indices) {
            val intent = Intent(applicationContext, CopyService::class.java)
            intent.putExtra(CopyService.TAG_COPY_SOURCES, files[i])
            intent.putExtra(CopyService.TAG_COPY_TARGET, paths[i])
            intent.putExtra(CopyService.TAG_COPY_MOVE, true)
            intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, mode.ordinal)
            intent.putExtra(CopyService.TAG_IS_ROOT_EXPLORER, isRootExplorer)
            ServiceWatcherUtil.runService(applicationContext, intent)
        }
    }
}
