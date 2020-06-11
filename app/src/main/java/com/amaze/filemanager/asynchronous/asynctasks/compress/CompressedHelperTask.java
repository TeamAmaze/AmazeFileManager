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

package com.amaze.filemanager.asynchronous.asynctasks.compress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.compress.archivers.ArchiveException;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

public abstract class CompressedHelperTask
    extends AsyncTask<Void, IOException, AsyncTaskResult<ArrayList<CompressedObjectParcelable>>> {

  private boolean createBackItem;
  private OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>> onFinish;

  CompressedHelperTask(
      boolean goBack,
      OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>> l) {
    createBackItem = goBack;
    onFinish = l;
  }

  @Override
  protected final AsyncTaskResult<ArrayList<CompressedObjectParcelable>> doInBackground(
      Void... voids) {
    AsyncTaskResult<ArrayList<CompressedObjectParcelable>> result = null;
    ArrayList<CompressedObjectParcelable> elements = new ArrayList<>();
    if (createBackItem) elements.add(0, new CompressedObjectParcelable());

    try {
      addElements(elements);
      Collections.sort(elements, new CompressedObjectParcelable.Sorter());

      return new AsyncTaskResult<>(elements);
    } catch (ArchiveException ifArchiveIsCorruptOrInvalid) {
      return new AsyncTaskResult<>(ifArchiveIsCorruptOrInvalid);
    }
  }

  @Override
  protected final void onPostExecute(
      AsyncTaskResult<ArrayList<CompressedObjectParcelable>> zipEntries) {
    super.onPostExecute(zipEntries);
    onFinish.onAsyncTaskFinished(zipEntries);
  }

  abstract void addElements(@NonNull ArrayList<CompressedObjectParcelable> elements)
      throws ArchiveException;
}
