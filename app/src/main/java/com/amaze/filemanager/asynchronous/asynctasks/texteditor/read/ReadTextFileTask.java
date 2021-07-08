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

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.read;

import static com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.ReadTextFileCallable.MAX_FILE_SIZE_CHARS;
import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.FILE;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.Task;
import com.amaze.filemanager.file_operations.exceptions.StreamNotFoundException;
import com.amaze.filemanager.ui.activities.texteditor.ReturnedValueOnReadFile;
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivity;
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivityViewModel;
import com.google.android.material.snackbar.Snackbar;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

public class ReadTextFileTask implements Task<ReturnedValueOnReadFile> {
  private static final String TAG = ReadTextFileTask.class.getSimpleName();

  private final WeakReference<TextEditorActivity> textEditorActivityWR;
  private final WeakReference<Context> appContextWR;
  private final Callable<ReturnedValueOnReadFile> task;

  public ReadTextFileTask(
      TextEditorActivity activity,
      WeakReference<TextEditorActivity> textEditorActivityWR,
      WeakReference<Context> appContextWR) {
    this.textEditorActivityWR = textEditorActivityWR;
    this.appContextWR = appContextWR;

    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(activity).get(TextEditorActivityViewModel.class);

    task =
        new ReadTextFileCallable(
            activity.getContentResolver(),
            viewModel.getFile(),
            activity.getExternalCacheDir(),
            activity.isRootExplorer());
  }

  @NonNull
  @Override
  public Callable<ReturnedValueOnReadFile> getTask() {
    return task;
  }

  @Override
  public void onError(@NonNull Throwable error) {
    Log.e(TAG, "Error on text read", error);

    final Context applicationContext = appContextWR.get();
    if (applicationContext == null) {
      return;
    }

    @StringRes int errorMessage;

    if (error instanceof StreamNotFoundException) {
      errorMessage = R.string.error_file_not_found;
    } else if (error instanceof IOException) {
      errorMessage = R.string.error_io;
    } else if (error instanceof OutOfMemoryError) {
      errorMessage = R.string.error_file_too_large;
    } else {
      errorMessage = R.string.error;
    }

    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show();

    final TextEditorActivity textEditorActivity = textEditorActivityWR.get();
    if (textEditorActivity == null) {
      return;
    }

    textEditorActivity.dismissLoadingSnackbar();

    textEditorActivity.finish();
  }

  @Override
  public void onFinish(ReturnedValueOnReadFile data) {
    final TextEditorActivity textEditorActivity = textEditorActivityWR.get();
    if (textEditorActivity == null) {
      return;
    }

    final TextEditorActivityViewModel viewModel =
        new ViewModelProvider(textEditorActivity).get(TextEditorActivityViewModel.class);

    textEditorActivity.dismissLoadingSnackbar();

    viewModel.setCacheFile(data.getCachedFile());
    viewModel.setOriginal(data.getFileContents());

    textEditorActivity.mainTextView.setText(data.getFileContents());

    if (viewModel.getFile().scheme.equals(FILE)
        && textEditorActivity.getExternalCacheDir() != null
        && viewModel
            .getFile()
            .hybridFileParcelable
            .getPath()
            .contains(textEditorActivity.getExternalCacheDir().getPath())
        && viewModel.getCacheFile() == null) {
      // file in cache, and not a root temporary file

      textEditorActivity.setReadOnly();

      Snackbar snackbar =
          Snackbar.make(
              textEditorActivity.mainTextView, R.string.file_read_only, Snackbar.LENGTH_INDEFINITE);
      snackbar.setAction(
          textEditorActivity.getResources().getString(R.string.got_it).toUpperCase(),
          v -> snackbar.dismiss());
      snackbar.show();
    }

    if (data.getFileContents().isEmpty()) {
      textEditorActivity.mainTextView.setHint(R.string.file_empty);
    } else {
      textEditorActivity.mainTextView.setHint(null);
    }

    if (data.getFileIsTooLong()) {
      textEditorActivity.setReadOnly();

      Snackbar snackbar =
          Snackbar.make(
              textEditorActivity.mainTextView,
              textEditorActivity
                  .getResources()
                  .getString(R.string.file_too_long, MAX_FILE_SIZE_CHARS),
              Snackbar.LENGTH_INDEFINITE);
      snackbar.setAction(
          textEditorActivity.getResources().getString(R.string.got_it).toUpperCase(),
          v -> snackbar.dismiss());
      snackbar.show();
    }
  }
}
