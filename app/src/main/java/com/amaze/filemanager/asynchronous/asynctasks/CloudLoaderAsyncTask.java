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

import java.lang.ref.WeakReference;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.models.explorer.CloudEntry;
import com.amaze.filemanager.file_operations.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.utils.DataUtils;
import com.cloudrail.si.CloudRail;
import com.cloudrail.si.exceptions.AuthenticationException;
import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;

import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class CloudLoaderAsyncTask extends AsyncTask<Void, Void, Boolean> {

  private Cursor data;
  private WeakReference<MainActivity> mainActivity;
  private CloudHandler cloudHandler;
  private DataUtils dataUtils;

  public CloudLoaderAsyncTask(MainActivity mainActivity, CloudHandler cloudHandler, Cursor data) {
    this.data = data;
    this.mainActivity = new WeakReference<>(mainActivity);
    this.cloudHandler = cloudHandler;
    this.dataUtils = DataUtils.getInstance();
  }

  @Override
  @NonNull
  public Boolean doInBackground(Void... voids) {
    boolean hasUpdatedDrawer = false;

    if (data == null) return false;
    if (data.getCount() > 0 && data.moveToFirst()) {
      do {
        if (mainActivity.get() == null || isCancelled()) {
          cancel(true);
          return false;
        }

        switch (data.getInt(0)) {
          case 1:
            try {
              CloudRail.setAppKey(data.getString(1));
            } catch (Exception e) {
              // any other exception due to network conditions or other error
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.failed_cloud_api_key);
              } else {
                cancel(true);
              }
              return false;
            }
            break;
          case 2:
            // DRIVE
            try {
              CloudEntry cloudEntryGdrive = null;
              CloudEntry savedCloudEntryGdrive;
              GoogleDrive cloudStorageDrive;
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                cloudStorageDrive =
                    new GoogleDrive(
                        mainActivity.getApplicationContext(),
                        data.getString(1),
                        "",
                        MainActivity.CLOUD_AUTHENTICATOR_REDIRECT_URI,
                        data.getString(2));
              } else {
                cancel(true);
                return false;
              }
              cloudStorageDrive.useAdvancedAuthentication();

              if ((savedCloudEntryGdrive = cloudHandler.findEntry(OpenMode.GDRIVE)) != null) {
                // we already have the entry and saved state, get it

                try {
                  cloudStorageDrive.loadAsString(savedCloudEntryGdrive.getPersistData().value);
                } catch (ParseException e) {
                  e.printStackTrace();
                  // we need to update the persist string as existing one is been compromised

                  cloudStorageDrive.login();
                  cloudEntryGdrive =
                      new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                  cloudHandler.updateEntry(OpenMode.GDRIVE, cloudEntryGdrive);
                }
              } else {
                cloudStorageDrive.login();
                cloudEntryGdrive =
                    new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                cloudHandler.addEntry(cloudEntryGdrive);
              }

              dataUtils.addAccount(cloudStorageDrive);
              hasUpdatedDrawer = true;
            } catch (CloudPluginException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_error_plugin);
                mainActivity.deleteConnection(OpenMode.GDRIVE);
              } else {
                cancel(true);
              }
              return false;
            } catch (AuthenticationException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_fail_authenticate);
                mainActivity.deleteConnection(OpenMode.GDRIVE);
              } else {
                cancel(true);
              }
              return false;
            } catch (Exception e) {
              // any other exception due to network conditions or other error
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.failed_cloud_new_connection);
                mainActivity.deleteConnection(OpenMode.GDRIVE);
              } else {
                cancel(true);
              }
              return false;
            }
            break;
          case 3:
            // DROPBOX
            try {
              CloudEntry cloudEntryDropbox = null;
              CloudEntry savedCloudEntryDropbox;
              CloudStorage cloudStorageDropbox;
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                cloudStorageDropbox =
                    new Dropbox(
                        mainActivity.getApplicationContext(), data.getString(1), data.getString(2));
              } else {
                cancel(true);
                return false;
              }

              if ((savedCloudEntryDropbox = cloudHandler.findEntry(OpenMode.DROPBOX)) != null) {
                // we already have the entry and saved state, get it
                try {
                  cloudStorageDropbox.loadAsString(savedCloudEntryDropbox.getPersistData().value);
                } catch (ParseException e) {
                  e.printStackTrace();
                  // we need to persist data again

                  cloudStorageDropbox.login();
                  cloudEntryDropbox =
                      new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                  cloudHandler.updateEntry(OpenMode.DROPBOX, cloudEntryDropbox);
                }
              } else {
                cloudStorageDropbox.login();
                cloudEntryDropbox =
                    new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                cloudHandler.addEntry(cloudEntryDropbox);
              }

              dataUtils.addAccount(cloudStorageDropbox);
              hasUpdatedDrawer = true;
            } catch (CloudPluginException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_error_plugin);
                mainActivity.deleteConnection(OpenMode.DROPBOX);
              } else {
                cancel(true);
              }
              return false;
            } catch (AuthenticationException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_fail_authenticate);
                mainActivity.deleteConnection(OpenMode.DROPBOX);
              } else cancel(true);
              return false;
            } catch (Exception e) {
              // any other exception due to network conditions or other error
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.failed_cloud_new_connection);
                mainActivity.deleteConnection(OpenMode.DROPBOX);
              } else cancel(true);
              return false;
            }
            break;
          case 4:
            // BOX
            try {
              CloudEntry cloudEntryBox = null;
              CloudEntry savedCloudEntryBox;
              CloudStorage cloudStorageBox;
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                cloudStorageBox =
                    new Box(
                        mainActivity.getApplicationContext(), data.getString(1), data.getString(2));
              } else {
                cancel(true);
                return false;
              }

              if ((savedCloudEntryBox = cloudHandler.findEntry(OpenMode.BOX)) != null) {
                // we already have the entry and saved state, get it
                try {
                  cloudStorageBox.loadAsString(savedCloudEntryBox.getPersistData().value);
                } catch (ParseException e) {
                  e.printStackTrace();
                  // we need to persist data again
                  cloudStorageBox.login();
                  cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                  cloudHandler.updateEntry(OpenMode.BOX, cloudEntryBox);
                }
              } else {
                cloudStorageBox.login();
                cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                cloudHandler.addEntry(cloudEntryBox);
              }

              dataUtils.addAccount(cloudStorageBox);
              hasUpdatedDrawer = true;
            } catch (CloudPluginException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_error_plugin);
                mainActivity.deleteConnection(OpenMode.BOX);
              } else cancel(true);
              return false;
            } catch (AuthenticationException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_fail_authenticate);
                mainActivity.deleteConnection(OpenMode.BOX);
              } else cancel(true);
              return false;
            } catch (Exception e) {
              // any other exception due to network conditions or other error
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.failed_cloud_new_connection);
                mainActivity.deleteConnection(OpenMode.BOX);
              } else cancel(true);
              return false;
            }
            break;
          case 5:
            // ONEDRIVE
            try {
              CloudEntry cloudEntryOnedrive = null;
              CloudEntry savedCloudEntryOnedrive;
              CloudStorage cloudStorageOnedrive;
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                cloudStorageOnedrive =
                    new OneDrive(
                        mainActivity.getApplicationContext(), data.getString(1), data.getString(2));
              } else {
                cancel(true);
                return false;
              }

              if ((savedCloudEntryOnedrive = cloudHandler.findEntry(OpenMode.ONEDRIVE)) != null) {
                // we already have the entry and saved state, get it
                try {
                  cloudStorageOnedrive.loadAsString(savedCloudEntryOnedrive.getPersistData().value);
                } catch (ParseException e) {
                  e.printStackTrace();
                  // we need to persist data again

                  cloudStorageOnedrive.login();
                  cloudEntryOnedrive =
                      new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                  cloudHandler.updateEntry(OpenMode.ONEDRIVE, cloudEntryOnedrive);
                }
              } else {
                cloudStorageOnedrive.login();
                cloudEntryOnedrive =
                    new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                cloudHandler.addEntry(cloudEntryOnedrive);
              }

              dataUtils.addAccount(cloudStorageOnedrive);
              hasUpdatedDrawer = true;
            } catch (CloudPluginException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_error_plugin);
                mainActivity.deleteConnection(OpenMode.ONEDRIVE);
              } else cancel(true);
              return false;
            } catch (AuthenticationException e) {
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.cloud_fail_authenticate);
                mainActivity.deleteConnection(OpenMode.ONEDRIVE);
              } else cancel(true);
              return false;
            } catch (Exception e) {
              // any other exception due to network conditions or other error
              e.printStackTrace();
              final MainActivity mainActivity = this.mainActivity.get();
              if (mainActivity != null) {
                AppConfig.toast(mainActivity, R.string.failed_cloud_new_connection);
                mainActivity.deleteConnection(OpenMode.ONEDRIVE);
              } else cancel(true);
              return false;
            }
            break;
          default:
            final MainActivity mainActivity = this.mainActivity.get();
            if (mainActivity != null) {
              Toast.makeText(mainActivity, R.string.cloud_error_failed_restart, Toast.LENGTH_LONG)
                  .show();
            } else cancel(true);
            return false;
        }
      } while (data.moveToNext());
    }
    return hasUpdatedDrawer;
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
      }
    }
  }
}
