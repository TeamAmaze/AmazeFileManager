/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.exceptions.StreamNotFoundException;
import com.amaze.filemanager.filesystem.EditableFileAbstraction;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.ConcatenateFileCommand;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 16/1/2018, at 18:36. */
public class WriteFileAbstraction extends AsyncTask<Void, String, Integer> {

  public static final int NORMAL = 0;
  public static final int EXCEPTION_STREAM_NOT_FOUND = -1;
  public static final int EXCEPTION_IO = -2;
  public static final int EXCEPTION_SHELL_NOT_RUNNING = -3;

  private WeakReference<Context> context;
  private ContentResolver contentResolver;
  private EditableFileAbstraction fileAbstraction;
  private File cachedFile;
  private boolean isRootExplorer;
  private OnAsyncTaskFinished<Integer> onAsyncTaskFinished;

  private String dataToSave;

  public WriteFileAbstraction(
      Context context,
      ContentResolver contentResolver,
      EditableFileAbstraction file,
      String dataToSave,
      File cachedFile,
      boolean isRootExplorer,
      OnAsyncTaskFinished<Integer> onAsyncTaskFinished) {
    this.context = new WeakReference<>(context);
    this.contentResolver = contentResolver;
    this.fileAbstraction = file;
    this.cachedFile = cachedFile;
    this.dataToSave = dataToSave;
    this.isRootExplorer = isRootExplorer;
    this.onAsyncTaskFinished = onAsyncTaskFinished;
  }

  @Override
  protected Integer doInBackground(Void... voids) {
    try {
      OutputStream outputStream;
      File destFile = null;

      switch (fileAbstraction.scheme) {
        case CONTENT:
          if (fileAbstraction.uri == null)
            throw new NullPointerException("Something went really wrong!");

          try {
            if (fileAbstraction.uri.getAuthority().equals(context.get().getPackageName())) {
              DocumentFile documentFile =
                  DocumentFile.fromSingleUri(AppConfig.getInstance(), fileAbstraction.uri);
              if (documentFile != null && documentFile.exists() && documentFile.canWrite())
                outputStream = contentResolver.openOutputStream(fileAbstraction.uri);
              else {
                destFile = FileUtils.fromContentUri(fileAbstraction.uri);
                outputStream = openFile(destFile, context.get());
              }
            } else {
              outputStream = contentResolver.openOutputStream(fileAbstraction.uri);
            }
          } catch (RuntimeException e) {
            throw new StreamNotFoundException(e);
          }
          break;
        case FILE:
          final HybridFileParcelable hybridFileParcelable = fileAbstraction.hybridFileParcelable;
          if (hybridFileParcelable == null)
            throw new NullPointerException("Something went really wrong!");

          Context context = this.context.get();
          if (context == null) {
            cancel(true);
            return null;
          }
          outputStream = openFile(hybridFileParcelable.getFile(), context);
          destFile = fileAbstraction.hybridFileParcelable.getFile();
          break;
        default:
          throw new IllegalArgumentException(
              "The scheme for '" + fileAbstraction.scheme + "' cannot be processed!");
      }

      if (outputStream == null) throw new StreamNotFoundException();

      outputStream.write(dataToSave.getBytes());
      outputStream.close();

      if (cachedFile != null && cachedFile.exists() && destFile != null) {
        // cat cache content to original file and delete cache file
        ConcatenateFileCommand.INSTANCE.concatenateFile(cachedFile.getPath(), destFile.getPath());
        cachedFile.delete();
      }

    } catch (IOException e) {
      e.printStackTrace();
      return EXCEPTION_IO;
    } catch (StreamNotFoundException e) {
      e.printStackTrace();
      return EXCEPTION_STREAM_NOT_FOUND;
    } catch (ShellNotRunningException e) {
      e.printStackTrace();
      return EXCEPTION_SHELL_NOT_RUNNING;
    }

    return NORMAL;
  }

  @Override
  protected void onPostExecute(Integer integer) {
    super.onPostExecute(integer);

    onAsyncTaskFinished.onAsyncTaskFinished(integer);
  }

  private OutputStream openFile(@Nullable File file, @NonNull Context context)
      throws FileNotFoundException {
    if (file == null) throw new IllegalArgumentException("File cannot be null");

    OutputStream outputStream = FileUtil.getOutputStream(file, context);

    if (isRootExplorer && outputStream == null) {
      // try loading stream associated using root
      try {
        if (cachedFile != null && cachedFile.exists()) {
          outputStream = new FileOutputStream(cachedFile);
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        outputStream = null;
      }
    }

    return outputStream;
  }
}
