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

import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_FAILED_OPS;
import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_GENERAL;

import java.util.ArrayList;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.fragments.CompressedExplorerFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.cloudrail.si.interfaces.CloudStorage;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import jcifs.smb.SmbException;

public class DeleteTask
    extends AsyncTask<ArrayList<HybridFileParcelable>, String, AsyncTaskResult<Boolean>> {

  private ArrayList<HybridFileParcelable> files;
  private Context cd;
  private boolean rootMode;
  private CompressedExplorerFragment compressedExplorerFragment;
  private DataUtils dataUtils = DataUtils.getInstance();

  public DeleteTask(@NonNull Context cd) {
    this.cd = cd;
    rootMode =
        PreferenceManager.getDefaultSharedPreferences(cd)
            .getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
  }

  public DeleteTask(@NonNull Context cd, CompressedExplorerFragment compressedExplorerFragment) {
    this.cd = cd;
    rootMode =
        PreferenceManager.getDefaultSharedPreferences(cd)
            .getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
    this.compressedExplorerFragment = compressedExplorerFragment;
  }

  @Override
  protected void onProgressUpdate(String... values) {
    super.onProgressUpdate(values);
    Toast.makeText(cd, values[0], Toast.LENGTH_SHORT).show();
  }

  protected AsyncTaskResult<Boolean> doInBackground(ArrayList<HybridFileParcelable>... p1) {
    files = p1[0];
    boolean wasDeleted = true;
    if (files.size() == 0) return new AsyncTaskResult<>(true);

    if (files.get(0).isOtgFile()) {
      for (HybridFileParcelable file : files) {
        DocumentFile documentFile = OTGUtil.getDocumentFile(file.getPath(), cd, false);
        wasDeleted = documentFile.delete();
      }
    } else if (files.get(0).isDropBoxFile()) {
      CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
      for (HybridFileParcelable baseFile : files) {
        try {
          cloudStorageDropbox.delete(CloudUtil.stripPath(OpenMode.DROPBOX, baseFile.getPath()));
        } catch (Exception e) {
          e.printStackTrace();
          wasDeleted = false;
          break;
        }
      }
    } else if (files.get(0).isBoxFile()) {
      CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
      for (HybridFileParcelable baseFile : files) {
        try {
          cloudStorageBox.delete(CloudUtil.stripPath(OpenMode.BOX, baseFile.getPath()));
        } catch (Exception e) {
          e.printStackTrace();
          wasDeleted = false;
          break;
        }
      }
    } else if (files.get(0).isGoogleDriveFile()) {
      CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
      for (HybridFileParcelable baseFile : files) {
        try {
          cloudStorageGdrive.delete(CloudUtil.stripPath(OpenMode.GDRIVE, baseFile.getPath()));
        } catch (Exception e) {
          e.printStackTrace();
          wasDeleted = false;
          break;
        }
      }
    } else if (files.get(0).isOneDriveFile()) {
      CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
      for (HybridFileParcelable baseFile : files) {
        try {
          cloudStorageOnedrive.delete(CloudUtil.stripPath(OpenMode.ONEDRIVE, baseFile.getPath()));
        } catch (Exception e) {
          e.printStackTrace();
          wasDeleted = false;
          break;
        }
      }
    } else {
      for (HybridFileParcelable file : files) {
        try {
          if (file.delete(cd, rootMode)) {
            wasDeleted = true;
          } else {
            wasDeleted = false;
            break;
          }
        } catch (ShellNotRunningException | SmbException e) {
          e.printStackTrace();
          wasDeleted = false;
          return new AsyncTaskResult<>(e);
        }
      }
    }

    // delete file from media database
    if (!files.get(0).isSmb()) {
      try {
        for (HybridFileParcelable f : files) {
          delete(cd, f.getPath());
        }
      } catch (Exception e) {
        FileUtils.scanFile(cd, files.toArray(new HybridFile[files.size()]));
      }
    }

    // delete file entry from encrypted database
    for (HybridFileParcelable file : files) {
      if (file.getName(cd).endsWith(CryptUtil.CRYPT_EXTENSION)) {
        CryptHandler handler = CryptHandler.getInstance();
        handler.clear(file.getPath());
      }
    }

    return new AsyncTaskResult<>(wasDeleted);
  }

  @Override
  public void onPostExecute(AsyncTaskResult<Boolean> result) {

    Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
    if (files.size() > 0) {
      String path = files.get(0).getParent(cd);
      intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, path);
      cd.sendBroadcast(intent);
    }

    if (result.result == null || !result.result) {
      cd.sendBroadcast(
          new Intent(TAG_INTENT_FILTER_GENERAL)
              .putParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS, files));
    } else if (compressedExplorerFragment == null) {
      AppConfig.toast(cd, R.string.done);
    }

    if (compressedExplorerFragment != null) {
      compressedExplorerFragment.files.clear();
    }

    // cancel any processing notification because of cut/paste operation
    NotificationManager notificationManager =
        (NotificationManager) cd.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancel(NotificationConstants.COPY_ID);
  }

  private void delete(final Context context, final String file) {
    final String where = MediaStore.MediaColumns.DATA + "=?";
    final String[] selectionArgs = new String[] {file};
    final ContentResolver contentResolver = context.getContentResolver();
    final Uri filesUri = MediaStore.Files.getContentUri("external");
    // Delete the entry from the media database. This will actually delete media files.
    contentResolver.delete(filesUri, where, selectionArgs);
  }
}
