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
import java.util.concurrent.Callable;

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

public class WriteFileAbstraction implements Callable<Void> {
  private final WeakReference<Context> context;
  private final ContentResolver contentResolver;
  private final EditableFileAbstraction fileAbstraction;
  private final File cachedFile;
  private final boolean isRootExplorer;
  private final String dataToSave;

  public WriteFileAbstraction(Context context, ContentResolver contentResolver,
                              EditableFileAbstraction file, String dataToSave, File cachedFile,
                              boolean isRootExplorer) {
    this.context = new WeakReference<>(context);
    this.contentResolver = contentResolver;
    this.fileAbstraction = file;
    this.cachedFile = cachedFile;
    this.dataToSave = dataToSave;
    this.isRootExplorer = isRootExplorer;
  }

  @Override
  public Void call()
          throws IOException, StreamNotFoundException, ShellNotRunningException, IllegalArgumentException {
    OutputStream outputStream;
    File destFile = null;

    switch (fileAbstraction.scheme) {
      case CONTENT:
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
        } catch (FileNotFoundException e) {
          throw new StreamNotFoundException(e);
        }
        break;
      case FILE:
        final HybridFileParcelable hybridFileParcelable = fileAbstraction.hybridFileParcelable;

        Context context = this.context.get();
        if (context == null) {
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
    return null;
  }

  private OutputStream openFile(@NonNull File file, @NonNull Context context)
          throws FileNotFoundException {
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
