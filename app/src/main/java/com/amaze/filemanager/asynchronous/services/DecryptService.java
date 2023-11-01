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

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.amaze.filemanager.asynchronous.services.EncryptService.TAG_PASSWORD;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.Task;
import com.amaze.filemanager.asynchronous.asynctasks.TaskKt;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.AESCrypt;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.ProgressHandler;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 28/11/2017, at 20:59.
 */
public class DecryptService extends AbstractProgressiveService {

  public static final String TAG_SOURCE = "crypt_source"; // source file to encrypt or decrypt
  public static final String TAG_DECRYPT_PATH = "decrypt_path";
  public static final String TAG_OPEN_MODE = "open_mode";

  public static final String TAG_BROADCAST_CRYPT_CANCEL = "crypt_cancel";
  private final Logger LOG = LoggerFactory.getLogger(DecryptService.class);

  private NotificationManagerCompat notificationManager;
  private NotificationCompat.Builder notificationBuilder;
  private Context context;
  private final IBinder mBinder = new ObtainableServiceBinder<>(this);
  private final ProgressHandler progressHandler = new ProgressHandler();
  private ProgressListener progressListener;
  // list of data packages, to initiate chart in process viewer fragment
  private final ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();
  private ServiceWatcherUtil serviceWatcherUtil;
  private long totalSize = 0L;
  private String decryptPath;
  private HybridFileParcelable baseFile;
  private final ArrayList<HybridFile> failedOps = new ArrayList<>();
  private String password;
  private RemoteViews customSmallContentViews, customBigContentViews;

  @Override
  public void onCreate() {
    super.onCreate();

    context = getApplicationContext();
    registerReceiver(cancelReceiver, new IntentFilter(TAG_BROADCAST_CRYPT_CANCEL));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    baseFile = intent.getParcelableExtra(TAG_SOURCE);
    password = intent.getStringExtra(TAG_PASSWORD);
    decryptPath = intent.getStringExtra(TAG_DECRYPT_PATH);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    int accentColor =
        ((AppConfig) getApplication())
            .getUtilsProvider()
            .getColorPreference()
            .getCurrentUserColorPreferences(this, sharedPreferences)
            .getAccent();

    notificationManager = NotificationManagerCompat.from(getApplicationContext());
    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(Intent.ACTION_MAIN);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(this, 0, notificationIntent, getPendingIntentFlag(0));

    customSmallContentViews =
        new RemoteViews(getPackageName(), R.layout.notification_service_small);
    customBigContentViews = new RemoteViews(getPackageName(), R.layout.notification_service_big);

    Intent stopIntent = new Intent(TAG_BROADCAST_CRYPT_CANCEL);
    PendingIntent stopPendingIntent =
        PendingIntent.getBroadcast(
            context, 1234, stopIntent, getPendingIntentFlag(FLAG_UPDATE_CURRENT));
    NotificationCompat.Action action =
        new NotificationCompat.Action(
            R.drawable.ic_folder_lock_open_white_36dp,
            getString(R.string.stop_ftp),
            stopPendingIntent);

    notificationBuilder =
        new NotificationCompat.Builder(this, NotificationConstants.CHANNEL_NORMAL_ID);
    notificationBuilder
        .setContentIntent(pendingIntent)
        .setCustomContentView(customSmallContentViews)
        .setCustomBigContentView(customBigContentViews)
        .setCustomHeadsUpContentView(customSmallContentViews)
        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
        .addAction(action)
        .setColor(accentColor)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);

    NotificationConstants.setMetadata(
        getApplicationContext(), notificationBuilder, NotificationConstants.TYPE_NORMAL);

    startForeground(getNotificationId(), notificationBuilder.build());
    initNotificationViews();

    super.onStartCommand(intent, flags, startId);
    super.progressHalted();
    TaskKt.fromTask(new BackgroundTask());

    return START_NOT_STICKY;
  }

  @Override
  protected NotificationManagerCompat getNotificationManager() {
    return notificationManager;
  }

  @Override
  protected NotificationCompat.Builder getNotificationBuilder() {
    return notificationBuilder;
  }

  @Override
  protected int getNotificationId() {
    return NotificationConstants.DECRYPT_ID;
  }

  @Override
  @StringRes
  protected int getTitle(boolean move) {
    return R.string.crypt_decrypting;
  }

  @Override
  protected RemoteViews getNotificationCustomViewSmall() {
    return customSmallContentViews;
  }

  @Override
  protected RemoteViews getNotificationCustomViewBig() {
    return customBigContentViews;
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

  class BackgroundTask implements Task<Long, Callable<Long>> {

    @Override
    public void onError(@NonNull Throwable error) {
      LOG.warn("failed to decrypt", error);
    }

    @Override
    public void onFinish(Long value) {
      serviceWatcherUtil.stopWatch();
      finalizeNotification(failedOps, false);

      Intent intent = new Intent(EncryptDecryptUtils.DECRYPT_BROADCAST);
      intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, "");
      sendBroadcast(intent);
      stopSelf();
    }

    @NonNull
    @Override
    public Callable<Long> getTask() {
      return () -> {
        String baseFileFolder =
            baseFile.isDirectory()
                ? baseFile.getPath()
                : baseFile.getPath().substring(0, baseFile.getPath().lastIndexOf('/'));

        if (baseFile.isDirectory()) totalSize = baseFile.folderSize(context);
        else totalSize = baseFile.length(context);

        progressHandler.setSourceSize(1);
        progressHandler.setTotalSize(totalSize);
        progressHandler.setProgressListener((speed) -> publishResults(speed, false, false));
        serviceWatcherUtil = new ServiceWatcherUtil(progressHandler);

        addFirstDatapoint(
            baseFile.getName(context),
            1,
            totalSize,
            false); // we're using encrypt as move flag false

        if (FileProperties.checkFolder(baseFileFolder, context) == 1) {
          serviceWatcherUtil.watch(DecryptService.this);

          // we're here to decrypt, we'll decrypt at a custom path.
          // the path is to the same directory as in encrypted one in normal case
          // and the cache directory in case we're here because of the viewer
          try {
            new CryptUtil(context, baseFile, decryptPath, progressHandler, failedOps, password);
          } catch (AESCrypt.DecryptFailureException ignored) {

          } catch (Exception e) {
            LOG.error("Error decrypting " + baseFile.getPath(), e);
            failedOps.add(baseFile);
          }
        }
        return totalSize;
      };
    }
  }

  @Override
  public boolean isDecryptService() {
    return true;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    this.unregisterReceiver(cancelReceiver);
  }

  private final BroadcastReceiver cancelReceiver =
      new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
          // cancel operation
          progressHandler.setCancelled(true);
        }
      };
}
