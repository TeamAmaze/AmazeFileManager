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

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.write

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.asynctasks.Task
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException
import com.amaze.filemanager.file_operations.exceptions.StreamNotFoundException
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivity
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivityViewModel
import java.io.IOException
import java.lang.ref.WeakReference

class WriteTextFileTask(
    activity: TextEditorActivity,
    private val editTextString: String,
    private val textEditorActivityWR: WeakReference<TextEditorActivity>,
    private val appContextWR: WeakReference<Context>
) : Task<Unit, WriteTextFileCallable> {

    companion object {
        private val TAG = WriteTextFileTask::class.java.simpleName
    }

    private val task: WriteTextFileCallable

    init {
        val viewModel: TextEditorActivityViewModel by activity.viewModels()
        task = WriteTextFileCallable(
            activity,
            activity.contentResolver,
            viewModel.file,
            editTextString,
            viewModel.cacheFile,
            activity.isRootExplorer
        )
    }

    override fun getTask(): WriteTextFileCallable = task

    @MainThread
    override fun onError(error: Throwable) {
        Log.e(TAG, "Error on text write", error)
        val applicationContext = appContextWR.get() ?: return
        @StringRes val errorMessage: Int = when (error) {
            is StreamNotFoundException -> {
                R.string.error_file_not_found
            }
            is IOException -> {
                R.string.error_io
            }
            is ShellNotRunningException -> {
                R.string.root_failure
            }
            else -> {
                R.string.error
            }
        }
        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
    }

    @MainThread
    override fun onFinish(value: Unit) {
        val applicationContext = appContextWR.get() ?: return
        Toast.makeText(applicationContext, R.string.done, Toast.LENGTH_SHORT).show()
        val textEditorActivity = textEditorActivityWR.get() ?: return
        val viewModel: TextEditorActivityViewModel by textEditorActivity.viewModels()

        viewModel.original = editTextString
        viewModel.modified = false
        textEditorActivity.invalidateOptionsMenu()
    }
}
