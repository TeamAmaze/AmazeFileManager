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

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.read;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
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

import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

public class ReadTextFileCallable implements Callable<ReturnedValueOnReadFile> {

  public static final int MAX_FILE_SIZE_CHARS = 50 * 1024;

  private final ContentResolver contentResolver;
  private final EditableFileAbstraction fileAbstraction;
  private final File externalCacheDir;
  private final boolean isRootExplorer;

  private File cachedFile = null;

  public ReadTextFileCallable(
      ContentResolver contentResolver,
      EditableFileAbstraction file,
      File cacheDir,
      boolean isRootExplorer) {
    this.contentResolver = contentResolver;
    this.fileAbstraction = file;
    this.externalCacheDir = cacheDir;
    this.isRootExplorer = isRootExplorer;
  }

  @WorkerThread
  @Override
  public ReturnedValueOnReadFile call()
      throws StreamNotFoundException, IOException, OutOfMemoryError, ShellNotRunningException {
    InputStream inputStream;

    switch (fileAbstraction.scheme) {
      case CONTENT:
        Objects.requireNonNull(fileAbstraction.uri);

        final AppConfig appConfig = AppConfig.getInstance();

        if (fileAbstraction.uri.getAuthority().equals(appConfig.getPackageName())) {
          DocumentFile documentFile = DocumentFile.fromSingleUri(appConfig, fileAbstraction.uri);

          if (documentFile != null && documentFile.exists() && documentFile.canWrite()) {
            inputStream = contentResolver.openInputStream(documentFile.getUri());
          } else {
            inputStream = loadFile(FileUtils.fromContentUri(fileAbstraction.uri));
          }
        } else {
          inputStream = contentResolver.openInputStream(fileAbstraction.uri);
        }
        break;
      case FILE:
        final HybridFileParcelable hybridFileParcelable = fileAbstraction.hybridFileParcelable;
        Objects.requireNonNull(hybridFileParcelable);

        File file = hybridFileParcelable.getFile();
        inputStream = loadFile(file);

        break;
      default:
        throw new IllegalArgumentException(
            "The scheme for '" + fileAbstraction.scheme + "' cannot be processed!");
    }

    Objects.requireNonNull(inputStream);

    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

    char[] buffer = new char[MAX_FILE_SIZE_CHARS];

    final int readChars = inputStreamReader.read(buffer);
    boolean tooLong = -1 != inputStream.read();

    inputStreamReader.close();

    final String fileContents = String.valueOf(buffer, 0, readChars);

    return new ReturnedValueOnReadFile(fileContents, cachedFile, tooLong);
  }

  private InputStream loadFile(File file) throws ShellNotRunningException, IOException {
    InputStream inputStream;

    if (!file.canWrite() && isRootExplorer) {
      // try loading stream associated using root
      cachedFile = new File(externalCacheDir, file.getName());
      // creating a cache file
      CopyFilesCommand.INSTANCE.copyFiles(file.getAbsolutePath(), cachedFile.getPath());

      inputStream = new FileInputStream(cachedFile);
    } else if (file.canRead()) {
      // readable file in filesystem
      try {
        inputStream = new FileInputStream(file.getAbsolutePath());
      } catch (FileNotFoundException e) {
        throw new FileNotFoundException(
            "Unable to open file [" + file.getAbsolutePath() + "] for reading");
      }
    } else {
      throw new IOException("Cannot read or write text file!");
    }

    return inputStream;
  }
}
