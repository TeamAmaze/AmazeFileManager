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

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.read

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.asynctasks.Task
import com.amaze.filemanager.file_operations.exceptions.StreamNotFoundException
import com.amaze.filemanager.filesystem.EditableFileAbstraction
import com.amaze.filemanager.ui.activities.texteditor.ReturnedValueOnReadFile
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivity
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivityViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

class ReadTextFileTask(
    activity: TextEditorActivity,
    private val textEditorActivityWR: WeakReference<TextEditorActivity>,
    private val appContextWR: WeakReference<Context>
) : Task<ReturnedValueOnReadFile, ReadTextFileCallable> {

    private val task: ReadTextFileCallable

    companion object {
        private val TAG = ReadTextFileTask::class.java.simpleName
    }

    init {
        val viewModel: TextEditorActivityViewModel by activity.viewModels()
        task = ReadTextFileCallable(
            activity.contentResolver,
            viewModel.file,
            activity.externalCacheDir,
            activity.isRootExplorer
        )
    }

    override fun getTask(): ReadTextFileCallable = task

    @MainThread
    override fun onError(error: Throwable) {
        Log.e(TAG, "Error on text read", error)
        val applicationContext = appContextWR.get() ?: return
        @StringRes val errorMessage: Int = when (error) {
            is StreamNotFoundException -> {
                R.string.error_file_not_found
            }
            is IOException -> {
                R.string.error_io
            }
            is OutOfMemoryError -> {
                R.string.error_file_too_large
            }
            else -> {
                R.string.error
            }
        }
        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
        val textEditorActivity = textEditorActivityWR.get() ?: return
        textEditorActivity.dismissLoadingSnackbar()
        textEditorActivity.finish()
    }

    @MainThread
    override fun onFinish(value: ReturnedValueOnReadFile) {
        val textEditorActivity = textEditorActivityWR.get() ?: return
        val viewModel: TextEditorActivityViewModel by textEditorActivity.viewModels()
        textEditorActivity.dismissLoadingSnackbar()
        viewModel.cacheFile = value.cachedFile
        viewModel.original = value.fileContents
        val file = viewModel.file ?: return
        val externalCacheDir = textEditorActivity.externalCacheDir

        textEditorActivity.mainTextView.setText(value.fileContents)

        // file in cache, and not a root temporary file
        val isFileInCacheAndNotRoot =
            file.scheme == EditableFileAbstraction.Scheme.FILE &&
                externalCacheDir != null &&
                file.hybridFileParcelable.path.contains(externalCacheDir.path) &&
                viewModel.cacheFile == null

        if (isFileInCacheAndNotRoot) {
            textEditorActivity.setReadOnly()
            val snackbar = Snackbar.make(
                textEditorActivity.mainTextView,
                R.string.file_read_only,
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.setAction(
                textEditorActivity.resources.getString(R.string.got_it)
                    .uppercase(Locale.getDefault())
            ) { snackbar.dismiss() }
            snackbar.show()
        }

        if (value.fileContents.isEmpty()) {
            textEditorActivity.mainTextView.setHint(R.string.file_empty)
        } else {
            textEditorActivity.mainTextView.hint = null
        }

        if (value.fileIsTooLong) {
            textEditorActivity.setReadOnly()
            val snackbar = Snackbar.make(
                textEditorActivity.mainTextView,
                textEditorActivity.resources
                    .getString(R.string.file_too_long, ReadTextFileCallable.MAX_FILE_SIZE_CHARS),
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.setAction(
                textEditorActivity.resources.getString(R.string.got_it)
                    .uppercase(Locale.getDefault())
            ) { snackbar.dismiss() }
            snackbar.show()
        }
    }
}
