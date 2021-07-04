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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.exceptions.StreamNotFoundException;
import com.amaze.filemanager.filesystem.EditableFileAbstraction;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.CopyFilesCommand;
import com.amaze.filemanager.ui.activities.texteditor.ReturnedValueOnReadFile;

import android.content.ContentResolver;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

public class ReadFileTask implements Callable<ReturnedValueOnReadFile> {

  private static final String TAG = ReadFileTask.class.getSimpleName();

  private final ContentResolver contentResolver;
  private final EditableFileAbstraction fileAbstraction;
  private final File externalCacheDir;
  private final boolean isRootExplorer;

  private File cachedFile = null;

  public ReadFileTask(
      ContentResolver contentResolver,
      EditableFileAbstraction file,
      File cacheDir,
      boolean isRootExplorer) {
    this.contentResolver = contentResolver;
    this.fileAbstraction = file;
    this.externalCacheDir = cacheDir;
    this.isRootExplorer = isRootExplorer;
  }

  @Override
  public ReturnedValueOnReadFile call()
      throws StreamNotFoundException, IOException, OutOfMemoryError {
    StringBuilder stringBuilder = new StringBuilder();

    InputStream inputStream = null;

    switch (fileAbstraction.scheme) {
      case CONTENT:
        if (fileAbstraction.uri == null)
          throw new NullPointerException("Something went really wrong!");

        if (fileAbstraction.uri.getAuthority().equals(AppConfig.getInstance().getPackageName())) {
          DocumentFile documentFile =
              DocumentFile.fromSingleUri(AppConfig.getInstance(), fileAbstraction.uri);
          if (documentFile != null && documentFile.exists() && documentFile.canWrite())
            inputStream = contentResolver.openInputStream(documentFile.getUri());
          else inputStream = loadFile(FileUtils.fromContentUri(fileAbstraction.uri));
        } else {
          inputStream = contentResolver.openInputStream(fileAbstraction.uri);
        }
        break;
      case FILE:
        final HybridFileParcelable hybridFileParcelable = fileAbstraction.hybridFileParcelable;
        if (hybridFileParcelable == null)
          throw new NullPointerException("Something went really wrong!");

        File file = hybridFileParcelable.getFile();
        inputStream = loadFile(file);

        break;
      default:
        throw new IllegalArgumentException(
            "The scheme for '" + fileAbstraction.scheme + "' cannot be processed!");
    }

    if (inputStream == null) throw new StreamNotFoundException();

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

    String buffer;
    while ((buffer = bufferedReader.readLine()) != null) {
      stringBuilder.append(buffer).append("\n");
    }

    inputStream.close();
    bufferedReader.close();

    return new ReturnedValueOnReadFile(stringBuilder.toString(), cachedFile);
  }

  private InputStream loadFile(File file) {
    InputStream inputStream = null;
    if (!file.canWrite() && isRootExplorer) {
      // try loading stream associated using root
      try {
        cachedFile = new File(externalCacheDir, file.getName());
        // creating a cache file
        CopyFilesCommand.INSTANCE.copyFiles(file.getAbsolutePath(), cachedFile.getPath());

        inputStream = new FileInputStream(cachedFile);
      } catch (ShellNotRunningException e) {
        e.printStackTrace();
        inputStream = null;
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        inputStream = null;
      }
    } else if (file.canRead()) {
      // readable file in filesystem
      try {
        inputStream = new FileInputStream(file.getAbsolutePath());
      } catch (FileNotFoundException e) {
        Log.e(TAG, "Unable to open file [" + file.getAbsolutePath() + "] for reading", e);
        inputStream = null;
      }
    }

    return inputStream;
  }
}
