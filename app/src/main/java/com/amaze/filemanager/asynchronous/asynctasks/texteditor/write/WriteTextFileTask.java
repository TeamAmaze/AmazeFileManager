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

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.write;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.Task;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.exceptions.StreamNotFoundException;
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivity;
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivityViewModel;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

import kotlin.Unit;

public class WriteTextFileTask implements Task<Unit> {
  private static final String TAG = WriteTextFileTask.class.getSimpleName();

  private final String editTextString;
  private final WeakReference<TextEditorActivity> textEditorActivityWR;
  private final WeakReference<Context> appContextWR;
  private final WriteTextFileCallable task;

  public WriteTextFileTask(
      TextEditorActivity activity,
      String editTextString,
      WeakReference<TextEditorActivity> textEditorActivityWR,
      WeakReference<Context> appContextWR) {
    this.editTextString = editTextString;
    this.textEditorActivityWR = textEditorActivityWR;
    this.appContextWR = appContextWR;

    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(activity).get(TextEditorActivityViewModel.class);

    this.task =
        new WriteTextFileCallable(
            activity,
            activity.getContentResolver(),
            viewModel.getFile(),
            editTextString,
            viewModel.getCacheFile(),
            activity.isRootExplorer());
  }

  @NonNull
  @Override
  public Callable<Unit> getTask() {
    return task;
  }

  @Override
  public void onError(@NonNull Throwable error) {
    Log.e(TAG, "Error on text write", error);

    final Context applicationContext = appContextWR.get();
    if (applicationContext == null) {
      return;
    }

    @StringRes int errorMessage;

    if (error instanceof StreamNotFoundException) {
      errorMessage = R.string.error_file_not_found;
    } else if (error instanceof IOException) {
      errorMessage = R.string.error_io;
    } else if (error instanceof ShellNotRunningException) {
      errorMessage = R.string.root_failure;
    } else {
      errorMessage = R.string.error;
    }

    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onFinish(Unit value) {
    final Context applicationContext = appContextWR.get();
    if (applicationContext == null) {
      return;
    }
    Toast.makeText(applicationContext, R.string.done, Toast.LENGTH_SHORT).show();

    final TextEditorActivity textEditorActivity = textEditorActivityWR.get();
    if (textEditorActivity == null) {
      return;
    }

    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(textEditorActivity).get(TextEditorActivityViewModel.class);

    viewModel.setOriginal(editTextString);
    viewModel.setModified(false);
    textEditorActivity.invalidateOptionsMenu();
  }
}
