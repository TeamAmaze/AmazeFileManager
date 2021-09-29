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

package com.amaze.filemanager.database;

import java.util.List;

import com.amaze.filemanager.database.models.explorer.CloudEntry;
import com.amaze.filemanager.file_operations.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.ui.fragments.CloudSheetFragment;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import io.reactivex.schedulers.Schedulers;

/** Created by vishal on 18/4/17. */
public class CloudHandler {

  public static final String CLOUD_PREFIX_BOX = "box:/";
  public static final String CLOUD_PREFIX_DROPBOX = "dropbox:/";
  public static final String CLOUD_PREFIX_GOOGLE_DRIVE = "gdrive:/";
  public static final String CLOUD_PREFIX_ONE_DRIVE = "onedrive:/";

  public static final String CLOUD_NAME_GOOGLE_DRIVE = "Google Drive™";
  public static final String CLOUD_NAME_DROPBOX = "Dropbox";
  public static final String CLOUD_NAME_ONE_DRIVE = "One Drive";
  public static final String CLOUD_NAME_BOX = "Box";

  private final ExplorerDatabase database;
  private final Context context;

  public CloudHandler(@NonNull Context context, @NonNull ExplorerDatabase explorerDatabase) {
    this.context = context;
    this.database = explorerDatabase;
  }

  public void addEntry(CloudEntry cloudEntry) throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();

    database.cloudEntryDao().insert(cloudEntry).subscribeOn(Schedulers.io()).subscribe();
  }

  public void clear(OpenMode serviceType) {
    database
        .cloudEntryDao()
        .findByServiceType(serviceType.ordinal())
        .subscribeOn(Schedulers.io())
        .subscribe(
            cloudEntry ->
                database
                    .cloudEntryDao()
                    .delete(cloudEntry)
                    .subscribeOn(Schedulers.io())
                    .subscribe());
  }

  public void updateEntry(OpenMode serviceType, CloudEntry newCloudEntry)
      throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();

    database.cloudEntryDao().update(newCloudEntry).subscribeOn(Schedulers.io()).subscribe();
  }

  public CloudEntry findEntry(OpenMode serviceType) throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();

    try {
      return database
          .cloudEntryDao()
          .findByServiceType(serviceType.ordinal())
          .subscribeOn(Schedulers.io())
          .blockingGet();
    } catch (Exception e) {
      // catch error to handle Single#onError for blockingGet
      Log.e(getClass().getSimpleName(), e.getMessage());
      return null;
    }
  }

  public List<CloudEntry> getAllEntries() throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();
    return database.cloudEntryDao().list().subscribeOn(Schedulers.io()).blockingGet();
  }
}
