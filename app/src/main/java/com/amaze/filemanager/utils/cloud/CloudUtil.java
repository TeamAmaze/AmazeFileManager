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

package com.amaze.filemanager.utils.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OpenMode;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import jcifs.smb.SmbFile;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;

/**
 * Created by vishal on 19/4/17.
 *
 * <p>Class provides helper methods for cloud utilities
 */
public class CloudUtil {

  public static final String TAG = "Explorer";

  /** @deprecated use getCloudFiles() */
  public static ArrayList<HybridFileParcelable> listFiles(
      String path, CloudStorage cloudStorage, OpenMode openMode) throws CloudPluginException {
    final ArrayList<HybridFileParcelable> baseFiles = new ArrayList<>();
    getCloudFiles(path, cloudStorage, openMode, baseFiles::add);
    return baseFiles;
  }

  public static void getCloudFiles(
      String path, CloudStorage cloudStorage, OpenMode openMode, OnFileFound fileFoundCallback)
      throws CloudPluginException {
    String strippedPath = stripPath(openMode, path);
    try {
      for (CloudMetaData cloudMetaData : cloudStorage.getChildren(strippedPath)) {
        HybridFileParcelable baseFile =
            new HybridFileParcelable(
                path + "/" + cloudMetaData.getName(),
                "",
                (cloudMetaData.getModifiedAt() == null) ? 0l : cloudMetaData.getModifiedAt(),
                cloudMetaData.getSize(),
                cloudMetaData.getFolder());
        baseFile.setName(cloudMetaData.getName());
        baseFile.setMode(openMode);
        fileFoundCallback.onFileFound(baseFile);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new CloudPluginException();
    }
  }

  /** Strips down the cloud path to remove any prefix */
  public static String stripPath(OpenMode openMode, String path) {
    String strippedPath = path;
    switch (openMode) {
      case DROPBOX:
        if (path.equals(CloudHandler.CLOUD_PREFIX_DROPBOX + "/")) {
          // we're at root, just replace the prefix
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_DROPBOX, "");
        } else {
          // we're not at root, replace prefix + /
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_DROPBOX + "/", "");
        }
        break;
      case BOX:
        if (path.equals(CloudHandler.CLOUD_PREFIX_BOX + "/")) {
          // we're at root, just replace the prefix
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_BOX, "");
        } else {
          // we're not at root, replace prefix + /
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_BOX + "/", "");
        }
        break;
      case ONEDRIVE:
        if (path.equals(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/")) {
          // we're at root, just replace the prefix
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_ONE_DRIVE, "");
        } else {
          // we're not at root, replace prefix + /
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/", "");
        }
        break;
      case GDRIVE:
        if (path.equals(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/")) {
          // we're at root, just replace the prefix
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE, "");
        } else {
          // we're not at root, replace prefix + /
          strippedPath = path.replace(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/", "");
        }
        break;
      default:
        break;
    }
    return strippedPath;
  }

  public static void launchCloud(
      final HybridFileParcelable baseFile, final OpenMode serviceType, final Activity activity) {
    final CloudStreamer streamer = CloudStreamer.getInstance();

    new Thread(
            () -> {
              try {
                streamer.setStreamSrc(
                    baseFile.getInputStream(activity),
                    baseFile.getName(activity),
                    baseFile.length(activity));
                activity.runOnUiThread(
                    () -> {
                      try {
                        File file =
                            new File(
                                Uri.parse(CloudUtil.stripPath(serviceType, baseFile.getPath()))
                                    .getPath());
                        Uri uri =
                            Uri.parse(CloudStreamer.URL + Uri.fromFile(file).getEncodedPath());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(
                            uri, MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
                        PackageManager packageManager = activity.getPackageManager();
                        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                        if (resInfos != null && resInfos.size() > 0) activity.startActivity(i);
                        else
                          Toast.makeText(
                                  activity,
                                  activity.getString(R.string.smb_launch_error),
                                  Toast.LENGTH_SHORT)
                              .show();
                      } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                      }
                    });
              } catch (Exception e) {

                e.printStackTrace();
              }
            })
        .start();
  }

  /**
   * Asynctask checks if the item pressed on is a cloud account, and if the token that is saved for
   * it is invalid or not, in which case, we'll clear off the saved token and authenticate the user
   * again
   *
   * @param path the path of item in drawer
   * @param mainActivity reference to main activity to fire callbacks to delete/add connection
   */
  public static void checkToken(String path, final MainActivity mainActivity) {

    new AsyncTask<String, Void, Boolean>() {

      OpenMode serviceType;
      private DataUtils dataUtils = DataUtils.getInstance();

      @Override
      protected Boolean doInBackground(String... params) {
        boolean isTokenValid = true;
        String path = params[0];

        if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {
          // dropbox account
          serviceType = OpenMode.DROPBOX;
          CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);

          try {
            cloudStorageDropbox.getUserLogin();
          } catch (Exception e) {
            e.printStackTrace();

            isTokenValid = false;
          }
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {

          serviceType = OpenMode.ONEDRIVE;
          CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

          try {
            cloudStorageOneDrive.getUserLogin();
          } catch (Exception e) {
            e.printStackTrace();

            isTokenValid = false;
          }
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {

          serviceType = OpenMode.BOX;
          CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);

          try {
            cloudStorageBox.getUserLogin();
          } catch (Exception e) {
            e.printStackTrace();

            isTokenValid = false;
          }
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {

          serviceType = OpenMode.GDRIVE;
          CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);

          try {
            cloudStorageGDrive.getUserLogin();
          } catch (Exception e) {
            e.printStackTrace();

            isTokenValid = false;
          }
        }
        return isTokenValid;
      }

      @Override
      protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);

        if (!aBoolean) {
          // delete account and create a new one
          Toast.makeText(
                  mainActivity,
                  mainActivity.getResources().getString(R.string.cloud_token_lost),
                  Toast.LENGTH_LONG)
              .show();
          mainActivity.deleteConnection(serviceType);
          mainActivity.addConnection(serviceType);
        }
      }
    }.execute(path);
  }

  /**
   * Get an input stream for thumbnail for a given {@link IconDataParcelable}
   *
   * @param context
   * @param path
   * @return
   */
  public static InputStream getThumbnailInputStreamForCloud(Context context, String path) {
    InputStream inputStream;
    HybridFile hybridFile = new HybridFile(OpenMode.UNKNOWN, path);
    hybridFile.generateMode(context);
    DataUtils dataUtils = DataUtils.getInstance();

    switch (hybridFile.getMode()) {
      case SFTP:
        inputStream =
            SshClientUtils.execute(
                new SFtpClientTemplate(hybridFile.getPath(), false) {
                  @Override
                  public InputStream execute(final SFTPClient client) throws IOException {
                    final RemoteFile rf =
                        client.open(SshClientUtils.extractRemotePathFrom(hybridFile.getPath()));
                    return rf.new RemoteFileInputStream() {
                      @Override
                      public void close() throws IOException {
                        try {
                          super.close();
                        } finally {
                          rf.close();
                          client.close();
                        }
                      }
                    };
                  }
                });
        break;
      case SMB:
        try {
          inputStream = new SmbFile(hybridFile.getPath()).getInputStream();
        } catch (IOException e) {
          inputStream = null;
          e.printStackTrace();
        }
        break;
      case OTG:
        ContentResolver contentResolver = context.getContentResolver();
        DocumentFile documentSourceFile =
            OTGUtil.getDocumentFile(hybridFile.getPath(), context, false);
        try {
          inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          inputStream = null;
        }
        break;
      case DROPBOX:
        CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
        inputStream =
            cloudStorageDropbox.getThumbnail(
                CloudUtil.stripPath(OpenMode.DROPBOX, hybridFile.getPath()));
        break;
      case BOX:
        CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
        inputStream =
            cloudStorageBox.getThumbnail(CloudUtil.stripPath(OpenMode.BOX, hybridFile.getPath()));
        break;
      case GDRIVE:
        CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);
        inputStream =
            cloudStorageGDrive.getThumbnail(
                CloudUtil.stripPath(OpenMode.GDRIVE, hybridFile.getPath()));
        break;
      case ONEDRIVE:
        CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
        inputStream =
            cloudStorageOneDrive.getThumbnail(
                CloudUtil.stripPath(OpenMode.ONEDRIVE, hybridFile.getPath()));
        break;
      default:
        try {
          inputStream = new FileInputStream(hybridFile.getPath());
        } catch (FileNotFoundException e) {
          inputStream = null;
          e.printStackTrace();
        }
        break;
    }
    return inputStream;
  }
}
