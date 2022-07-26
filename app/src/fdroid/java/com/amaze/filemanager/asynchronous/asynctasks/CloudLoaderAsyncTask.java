/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import java.lang.ref.WeakReference;

import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.ui.activities.MainActivity;

import android.database.Cursor;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

public class CloudLoaderAsyncTask extends AsyncTask<Void, Void, Boolean> {

  private final WeakReference<MainActivity> mainActivity;

  public CloudLoaderAsyncTask(MainActivity mainActivity, CloudHandler unused1, Cursor unused2) {
    this.mainActivity = new WeakReference<>(mainActivity);
  }

  @Override
  @NonNull
  public Boolean doInBackground(Void... voids) {
    return false;
  }

  @Override
  protected void onCancelled() {
    super.onCancelled();
    final MainActivity mainActivity = this.mainActivity.get();
    if (mainActivity != null) {
      mainActivity
          .getSupportLoaderManager()
          .destroyLoader(MainActivity.REQUEST_CODE_CLOUD_LIST_KEY);
      mainActivity
          .getSupportLoaderManager()
          .destroyLoader(MainActivity.REQUEST_CODE_CLOUD_LIST_KEYS);
    }
  }

  @Override
  public void onPostExecute(@NonNull Boolean result) {
    if (result) {
      final MainActivity mainActivity = this.mainActivity.get();
      if (mainActivity != null) {
        mainActivity.getDrawer().refreshDrawer();
        mainActivity.invalidateFragmentAndBundle(null, true);
      }
    }
  }
}
