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

import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.exceptions.StreamNotFoundException;
import com.amaze.filemanager.filesystem.EditableFileAbstraction;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.RootUtils;

import android.content.ContentResolver;
import android.os.AsyncTask;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 16/1/2018, at 18:05. */
public class ReadFileTask extends AsyncTask<Void, Void, ReadFileTask.ReturnedValues> {

  public static final int NORMAL = 0;
  public static final int EXCEPTION_STREAM_NOT_FOUND = -1;
  public static final int EXCEPTION_IO = -2;

  private ContentResolver contentResolver;
  private EditableFileAbstraction fileAbstraction;
  private File externalCacheDir;
  private boolean isRootExplorer;
  private OnAsyncTaskFinished<ReturnedValues> onAsyncTaskFinished;

  private File cachedFile = null;

  public ReadFileTask(
      ContentResolver contentResolver,
      EditableFileAbstraction file,
      File cacheDir,
      boolean isRootExplorer,
      OnAsyncTaskFinished<ReturnedValues> onAsyncTaskFinished) {
    this.contentResolver = contentResolver;
    this.fileAbstraction = file;
    this.externalCacheDir = cacheDir;
    this.isRootExplorer = isRootExplorer;
    this.onAsyncTaskFinished = onAsyncTaskFinished;
  }

  @Override
  protected ReturnedValues doInBackground(Void... params) {
    StringBuilder stringBuilder = new StringBuilder();

    try {
      InputStream inputStream = null;

      switch (fileAbstraction.scheme) {
        case EditableFileAbstraction.SCHEME_CONTENT:
          if (fileAbstraction.uri == null)
            throw new NullPointerException("Something went really wrong!");

          inputStream = contentResolver.openInputStream(fileAbstraction.uri);
          break;
        case EditableFileAbstraction.SCHEME_FILE:
          final HybridFileParcelable hybridFileParcelable = fileAbstraction.hybridFileParcelable;
          if (hybridFileParcelable == null)
            throw new NullPointerException("Something went really wrong!");

          File file = hybridFileParcelable.getFile();

          if (!file.canWrite() && isRootExplorer) {
            // try loading stream associated using root
            try {
              cachedFile = new File(externalCacheDir, hybridFileParcelable.getName());
              // creating a cache file
              RootUtils.copy(hybridFileParcelable.getPath(), cachedFile.getPath());

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
              inputStream = new FileInputStream(hybridFileParcelable.getPath());
            } catch (FileNotFoundException e) {
              inputStream = null;
            }
          }
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
    } catch (StreamNotFoundException e) {
      e.printStackTrace();
      return new ReturnedValues(EXCEPTION_STREAM_NOT_FOUND);
    } catch (IOException e) {
      e.printStackTrace();
      return new ReturnedValues(EXCEPTION_IO);
    }

    return new ReturnedValues(stringBuilder.toString(), cachedFile);
  }

  @Override
  protected void onPostExecute(ReturnedValues s) {
    super.onPostExecute(s);

    onAsyncTaskFinished.onAsyncTaskFinished(s);
  }

  public static class ReturnedValues {
    public final String fileContents;
    public final int error;
    public final File cachedFile;

    private ReturnedValues(String fileContents, File cachedFile) {
      this.fileContents = fileContents;
      this.cachedFile = cachedFile;

      this.error = NORMAL;
    }

    private ReturnedValues(int error) {
      this.error = error;

      this.fileContents = null;
      this.cachedFile = null;
    }
  }
}
