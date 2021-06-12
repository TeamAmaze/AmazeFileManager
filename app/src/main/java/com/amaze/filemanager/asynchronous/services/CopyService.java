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

package com.amaze.filemanager.asynchronous.services;

import java.io.IOException;
import java.util.ArrayList;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.explorer.EncryptedEntry;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;
import com.amaze.filemanager.filesystem.root.CopyFilesCommand;
import com.amaze.filemanager.filesystem.root.MoveFileCommand;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.ProgressHandler;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

public class CopyService extends AbstractProgressiveService {

  public static final String TAG_IS_ROOT_EXPLORER = "is_root";
  public static final String TAG_COPY_TARGET = "COPY_DIRECTORY";
  public static final String TAG_COPY_SOURCES = "FILE_PATHS";
  public static final String TAG_COPY_OPEN_MODE = "MODE"; // target open mode
  public static final String TAG_COPY_MOVE = "move";
  private static final String TAG_COPY_START_ID = "id";

  public static final String TAG_BROADCAST_COPY_CANCEL = "copycancel";

  private NotificationManager mNotifyManager;
  private NotificationCompat.Builder mBuilder;
  private Context c;

  private final IBinder mBinder = new ObtainableServiceBinder<>(this);
  private ServiceWatcherUtil watcherUtil;
  private ProgressHandler progressHandler = new ProgressHandler();
  private ProgressListener progressListener;
  // list of data packages, to initiate chart in process viewer fragment
  private ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();
  private int accentColor;
  private SharedPreferences sharedPreferences;
  private RemoteViews customSmallContentViews, customBigContentViews;

  private boolean isRootExplorer;
  private long totalSize = 0L;
  private int totalSourceFiles = 0;

  @Override
  public void onCreate() {
    super.onCreate();
    c = getApplicationContext();
    registerReceiver(receiver3, new IntentFilter(TAG_BROADCAST_COPY_CANCEL));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, final int startId) {
    Bundle b = new Bundle();
    isRootExplorer = intent.getBooleanExtra(TAG_IS_ROOT_EXPLORER, false);
    ArrayList<HybridFileParcelable> files = intent.getParcelableArrayListExtra(TAG_COPY_SOURCES);
    String targetPath = intent.getStringExtra(TAG_COPY_TARGET);
    int mode = intent.getIntExtra(TAG_COPY_OPEN_MODE, OpenMode.UNKNOWN.ordinal());
    final boolean move = intent.getBooleanExtra(TAG_COPY_MOVE, false);
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
    accentColor =
        ((AppConfig) getApplication())
            .getUtilsProvider()
            .getColorPreference()
            .getCurrentUserColorPreferences(this, sharedPreferences)
            .getAccent();

    mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    b.putInt(TAG_COPY_START_ID, startId);

    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(Intent.ACTION_MAIN);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    customSmallContentViews =
        new RemoteViews(getPackageName(), R.layout.notification_service_small);
    customBigContentViews = new RemoteViews(getPackageName(), R.layout.notification_service_big);

    Intent stopIntent = new Intent(TAG_BROADCAST_COPY_CANCEL);
    PendingIntent stopPendingIntent =
        PendingIntent.getBroadcast(c, 1234, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    NotificationCompat.Action action =
        new NotificationCompat.Action(
            R.drawable.ic_content_copy_white_36dp, getString(R.string.stop_ftp), stopPendingIntent);

    mBuilder =
        new NotificationCompat.Builder(c, NotificationConstants.CHANNEL_NORMAL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_content_copy_white_36dp)
            .setCustomContentView(customSmallContentViews)
            .setCustomBigContentView(customBigContentViews)
            .setCustomHeadsUpContentView(customSmallContentViews)
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
            .addAction(action)
            .setOngoing(true)
            .setColor(accentColor);

    // set default notification views text

    NotificationConstants.setMetadata(c, mBuilder, NotificationConstants.TYPE_NORMAL);

    startForeground(NotificationConstants.COPY_ID, mBuilder.build());
    initNotificationViews();

    b.putBoolean(TAG_COPY_MOVE, move);
    b.putString(TAG_COPY_TARGET, targetPath);
    b.putInt(TAG_COPY_OPEN_MODE, mode);
    b.putParcelableArrayList(TAG_COPY_SOURCES, files);

    super.onStartCommand(intent, flags, startId);
    super.progressHalted();
    // going async
    new DoInBackground(isRootExplorer).execute(b);

    // If we get killed, after returning from here, restart
    return START_NOT_STICKY;
  }

  @Override
  protected NotificationManager getNotificationManager() {
    return mNotifyManager;
  }

  @Override
  protected NotificationCompat.Builder getNotificationBuilder() {
    return mBuilder;
  }

  @Override
  protected int getNotificationId() {
    return NotificationConstants.COPY_ID;
  }

  @Override
  protected RemoteViews getNotificationCustomViewSmall() {
    return customSmallContentViews;
  }

  @Override
  protected RemoteViews getNotificationCustomViewBig() {
    return customBigContentViews;
  }

  @Override
  @StringRes
  protected int getTitle(boolean move) {
    return move ? R.string.moving : R.string.copying;
  }

  public ProgressListener getProgressListener() {
    return progressListener;
  }

  @Override
  public void setProgressListener(ProgressListener progressListener) {
    this.progressListener = progressListener;
  }

  @Override
  protected ArrayList<DatapointParcelable> getDataPackages() {
    return dataPackages;
  }

  @Override
  protected ProgressHandler getProgressHandler() {
    return progressHandler;
  }

  @Override
  protected void clearDataPackages() {
    dataPackages.clear();
  }

  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(receiver3);
  }

  private class DoInBackground extends AsyncTask<Bundle, Void, Void> {
    boolean move;
    private Copy copy;
    private String targetPath;
    private boolean isRootExplorer;
    private int sourceProgress = 0;

    private DoInBackground(boolean isRootExplorer) {
      this.isRootExplorer = isRootExplorer;
    }

    protected Void doInBackground(Bundle... p1) {

      ArrayList<HybridFileParcelable> sourceFiles = p1[0].getParcelableArrayList(TAG_COPY_SOURCES);

      // setting up service watchers and initial data packages
      // finding total size on background thread (this is necessary condition for SMB!)
      totalSize = FileUtils.getTotalBytes(sourceFiles, c);
      totalSourceFiles = sourceFiles.size();

      progressHandler.setSourceSize(totalSourceFiles);
      progressHandler.setTotalSize(totalSize);

      progressHandler.setProgressListener((speed) -> publishResults(speed, false, move));

      watcherUtil = new ServiceWatcherUtil(progressHandler);

      addFirstDatapoint(sourceFiles.get(0).getName(c), sourceFiles.size(), totalSize, move);

      targetPath = p1[0].getString(TAG_COPY_TARGET);
      move = p1[0].getBoolean(TAG_COPY_MOVE);
      OpenMode openMode = OpenMode.getOpenMode(p1[0].getInt(TAG_COPY_OPEN_MODE));
      copy = new Copy();
      copy.execute(sourceFiles, targetPath, move, openMode);

      if (copy.failedFOps.size() == 0) {

        // adding/updating new encrypted db entry if any encrypted file was copied/moved
        for (HybridFileParcelable sourceFile : sourceFiles) {
          try {
            findAndReplaceEncryptedEntry(sourceFile);
          } catch (Exception e) {
            // unable to modify encrypted entry in database
            Toast.makeText(c, getString(R.string.encryption_fail_copy), Toast.LENGTH_SHORT).show();
          }
        }
      }
      return null;
    }

    @Override
    public void onPostExecute(Void b) {

      super.onPostExecute(b);
      //  publishResults(b, "", totalSourceFiles, totalSourceFiles, totalSize, totalSize, 0, true,
      // move);
      // stopping watcher if not yet finished
      watcherUtil.stopWatch();
      finalizeNotification(copy.failedFOps, move);

      Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
      intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, targetPath);
      sendBroadcast(intent);
      stopSelf();
    }

    /**
     * Iterates through every file to find an encrypted file and update/add a new entry about it's
     * metadata in the database
     *
     * @param sourceFile the file which is to be iterated
     */
    private void findAndReplaceEncryptedEntry(HybridFileParcelable sourceFile) {

      // even directories can end with CRYPT_EXTENSION
      if (sourceFile.isDirectory() && !sourceFile.getName(c).endsWith(CryptUtil.CRYPT_EXTENSION)) {
        sourceFile.forEachChildrenFile(
            getApplicationContext(),
            isRootExplorer,
            file -> {
              // iterating each file inside source files which were copied to find instance of
              // any copied / moved encrypted file
              findAndReplaceEncryptedEntry(file);
            });
      } else {

        if (sourceFile.getName(c).endsWith(CryptUtil.CRYPT_EXTENSION)) {
          try {

            CryptHandler cryptHandler = CryptHandler.getInstance();
            EncryptedEntry oldEntry = cryptHandler.findEntry(sourceFile.getPath());
            EncryptedEntry newEntry = new EncryptedEntry();

            newEntry.setPassword(oldEntry.getPassword());
            newEntry.setPath(targetPath + "/" + sourceFile.getName(c));

            if (move) {

              // file was been moved, update the existing entry
              newEntry.setId(oldEntry.getId());
              cryptHandler.updateEntry(oldEntry, newEntry);
            } else {
              // file was copied, create a new entry with same data
              cryptHandler.addEntry(newEntry);
            }
          } catch (Exception e) {
            e.printStackTrace();
            // couldn't change the entry, leave it alone
          }
        }
      }
    }

    class Copy {

      ArrayList<HybridFile> failedFOps;
      ArrayList<HybridFileParcelable> toDelete;

      Copy() {
        failedFOps = new ArrayList<>();
        toDelete = new ArrayList<>();
      }

      /**
       * Method iterate through files to be copied
       *
       * @param mode target file open mode (current path's open mode)
       */
      public void execute(
          final ArrayList<HybridFileParcelable> sourceFiles,
          final String targetPath,
          final boolean move,
          OpenMode mode) {

        // initial start of copy, initiate the watcher
        watcherUtil.watch(CopyService.this);

        if (FileProperties.checkFolder((targetPath), c) == 1) {
          for (int i = 0; i < sourceFiles.size(); i++) {
            sourceProgress = i;
            HybridFileParcelable f1 = (sourceFiles.get(i));

            try {

              HybridFile hFile;
              if (targetPath.contains(getExternalCacheDir().getPath())) {
                // the target open mode is not the one we're currently in!
                // we're processing the file for cache
                hFile =
                    new HybridFile(
                        OpenMode.FILE, targetPath, sourceFiles.get(i).getName(c), f1.isDirectory());
              } else {

                // the target open mode is where we're currently at
                hFile =
                    new HybridFile(
                        mode, targetPath, sourceFiles.get(i).getName(c), f1.isDirectory());
              }

              if (!progressHandler.getCancelled()) {

                if ((f1.getMode() == OpenMode.ROOT || mode == OpenMode.ROOT) && isRootExplorer) {
                  // either source or target are in root
                  Log.d(getClass().getSimpleName(), "either source or target are in root");
                  progressHandler.setSourceFilesProcessed(++sourceProgress);
                  copyRoot(f1, hFile, move);
                  continue;
                }
                progressHandler.setSourceFilesProcessed(++sourceProgress);
                copyFiles((f1), hFile, progressHandler);
              } else {
                break;
              }
            } catch (Exception e) {
              e.printStackTrace();
              Log.e("CopyService", "Got exception checkout: " + f1.getPath());

              failedFOps.add(sourceFiles.get(i));
              for (int j = i + 1; j < sourceFiles.size(); j++) failedFOps.add(sourceFiles.get(j));
              break;
            }
          }

        } else if (isRootExplorer) {
          for (int i = 0; i < sourceFiles.size(); i++) {
            if (!progressHandler.getCancelled()) {
              HybridFile hFile =
                  new HybridFile(
                      mode,
                      targetPath,
                      sourceFiles.get(i).getName(c),
                      sourceFiles.get(i).isDirectory());
              progressHandler.setSourceFilesProcessed(++sourceProgress);
              progressHandler.setFileName(sourceFiles.get(i).getName(c));
              copyRoot(sourceFiles.get(i), hFile, move);
              /*if(checkFiles(new HybridFile(sourceFiles.get(i).getMode(),path),
              new HybridFile(OpenMode.ROOT,targetPath+"/"+name))){
                  failedFOps.add(sourceFiles.get(i));
              }*/
            }
          }
        } else {
          for (HybridFileParcelable f : sourceFiles) failedFOps.add(f);
          return;
        }

        // making sure to delete files after copy operation is done
        // and not if the copy was cancelled
        if (move && !progressHandler.getCancelled()) {
          ArrayList<HybridFileParcelable> toDelete = new ArrayList<>();
          for (HybridFileParcelable a : sourceFiles) {
            if (!failedFOps.contains(a)) toDelete.add(a);
          }
          new DeleteTask(c).execute((toDelete));
        }
      }

      void copyRoot(HybridFileParcelable sourceFile, HybridFile targetFile, boolean move) {

        try {
          if (!move) {
            CopyFilesCommand.INSTANCE.copyFiles(sourceFile.getPath(), targetFile.getPath());
          } else if (move) {
            MoveFileCommand.INSTANCE.moveFile(sourceFile.getPath(), targetFile.getPath());
          }
          ServiceWatcherUtil.position += sourceFile.getSize();
        } catch (ShellNotRunningException e) {
          e.printStackTrace();
          failedFOps.add(sourceFile);
        }
        FileUtils.scanFile(c, new HybridFile[] {targetFile});
      }

      private void copyFiles(
          final HybridFileParcelable sourceFile,
          final HybridFile targetFile,
          final ProgressHandler progressHandler)
          throws IOException {

        if (progressHandler.getCancelled()) return;
        if (sourceFile.isDirectory()) {

          if (!targetFile.exists()) targetFile.mkdir(c);

          // various checks
          // 1. source file and target file doesn't end up in loop
          // 2. source file has a valid name or not
          if (!Operations.isFileNameValid(sourceFile.getName(c))
              || Operations.isCopyLoopPossible(sourceFile, targetFile)) {
            failedFOps.add(sourceFile);
            return;
          }
          targetFile.setLastModified(sourceFile.lastModified());

          if (progressHandler.getCancelled()) return;
          sourceFile.forEachChildrenFile(
              c,
              false,
              file -> {
                HybridFile destFile =
                    new HybridFile(
                        targetFile.getMode(),
                        targetFile.getPath(),
                        file.getName(c),
                        file.isDirectory());
                try {
                  copyFiles(file, destFile, progressHandler);
                } catch (IOException e) {
                  throw new IllegalStateException(e); // throw unchecked exception, no throws needed
                }
              });
        } else {
          if (!Operations.isFileNameValid(sourceFile.getName(c))) {
            failedFOps.add(sourceFile);
            return;
          }

          GenericCopyUtil copyUtil = new GenericCopyUtil(c, progressHandler);

          progressHandler.setFileName(sourceFile.getName(c));
          copyUtil.copy(
              sourceFile,
              targetFile,
              () -> {
                // we ran out of memory to map the whole channel, let's switch to streams
                AppConfig.toast(c, c.getString(R.string.copy_low_memory));
              },
              ServiceWatcherUtil.UPDATE_POSITION);
        }
      }
    }
  }

  private BroadcastReceiver receiver3 =
      new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
          // cancel operation
          progressHandler.setCancelled(true);
        }
      };

  @Override
  public IBinder onBind(Intent arg0) {
    // TODO Auto-generated method stub
    return mBinder;
  }
}
