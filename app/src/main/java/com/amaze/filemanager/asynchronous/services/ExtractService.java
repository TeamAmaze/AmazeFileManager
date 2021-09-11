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

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.apache.commons.compress.PasswordRequiredException;
import org.tukaani.xz.CorruptedInputException;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.ProgressHandler;
import com.github.junrar.exception.UnsupportedRarV5Exception;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import net.lingala.zip4j.exception.ZipException;

public class ExtractService extends AbstractProgressiveService {

  Context context;

  private static final String TAG = ExtractService.class.getSimpleName();

  private final IBinder mBinder = new ObtainableServiceBinder<>(this);

  // list of data packages,// to initiate chart in process viewer fragment
  private ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();

  private NotificationManager mNotifyManager;
  private NotificationCompat.Builder mBuilder;
  private ProgressHandler progressHandler = new ProgressHandler();
  private ProgressListener progressListener;
  private int accentColor;
  private SharedPreferences sharedPreferences;
  private RemoteViews customSmallContentViews, customBigContentViews;

  public static final String KEY_PATH_ZIP = "zip";
  public static final String KEY_ENTRIES_ZIP = "entries";
  public static final String TAG_BROADCAST_EXTRACT_CANCEL = "excancel";
  public static final String KEY_PATH_EXTRACT = "extractpath";

  @Override
  public void onCreate() {
    registerReceiver(receiver1, new IntentFilter(TAG_BROADCAST_EXTRACT_CANCEL));
    context = getApplicationContext();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, final int startId) {
    String file = intent.getStringExtra(KEY_PATH_ZIP);
    String extractPath = intent.getStringExtra(KEY_PATH_EXTRACT);
    String[] entries = intent.getStringArrayExtra(KEY_ENTRIES_ZIP);

    mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    accentColor =
        ((AppConfig) getApplication())
            .getUtilsProvider()
            .getColorPreference()
            .getCurrentUserColorPreferences(this, sharedPreferences)
            .getAccent();

    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(Intent.ACTION_MAIN);
    notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    customSmallContentViews =
        new RemoteViews(getPackageName(), R.layout.notification_service_small);
    customBigContentViews = new RemoteViews(getPackageName(), R.layout.notification_service_big);

    Intent stopIntent = new Intent(TAG_BROADCAST_EXTRACT_CANCEL);
    PendingIntent stopPendingIntent =
        PendingIntent.getBroadcast(context, 1234, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    NotificationCompat.Action action =
        new NotificationCompat.Action(
            R.drawable.ic_zip_box_grey, getString(R.string.stop_ftp), stopPendingIntent);

    mBuilder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID);
    mBuilder
        .setContentIntent(pendingIntent)
        .setSmallIcon(R.drawable.ic_zip_box_grey)
        .setContentIntent(pendingIntent)
        .setCustomContentView(customSmallContentViews)
        .setCustomBigContentView(customBigContentViews)
        .setCustomHeadsUpContentView(customSmallContentViews)
        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
        .addAction(action)
        .setAutoCancel(true)
        .setOngoing(true)
        .setColor(accentColor);

    NotificationConstants.setMetadata(
        getApplicationContext(), mBuilder, NotificationConstants.TYPE_NORMAL);
    startForeground(NotificationConstants.EXTRACT_ID, mBuilder.build());
    initNotificationViews();

    long totalSize = getTotalSize(file);

    progressHandler.setSourceSize(1);
    progressHandler.setTotalSize(totalSize);
    progressHandler.setProgressListener((speed) -> publishResults(speed, false, false));

    super.onStartCommand(intent, flags, startId);
    super.progressHalted();
    new DoWork(this, progressHandler, file, extractPath, entries).execute();

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
    return NotificationConstants.EXTRACT_ID;
  }

  @Override
  @StringRes
  protected int getTitle(boolean move) {
    return R.string.extracting;
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

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(receiver1);
  }

  /**
   * Method calculates zip file size to initiate progress Supporting local file extraction progress
   * for now
   */
  private long getTotalSize(String filePath) {
    return new File(filePath).length();
  }

  public class DoWork extends AsyncTask<Void, IOException, Boolean> {
    private WeakReference<ExtractService> extractService;
    private String[] entriesToExtract;
    private String extractionPath, compressedPath;
    private ProgressHandler progressHandler;
    private ServiceWatcherUtil watcherUtil;
    private boolean paused = false;
    private boolean passwordProtected = false;

    private DoWork(
        ExtractService extractService,
        ProgressHandler progressHandler,
        String cpath,
        String epath,
        String[] entries) {
      this.extractService = new WeakReference<>(extractService);
      this.progressHandler = progressHandler;
      compressedPath = cpath;
      extractionPath = epath;
      entriesToExtract = entries;
    }

    @Override
    protected Boolean doInBackground(Void... p) {
      while (!isCancelled()) {
        if (paused) continue;

        final ExtractService extractService = this.extractService.get();
        if (extractService == null) return null;

        File f = new File(compressedPath);
        String extractDirName = CompressedHelper.getFileName(f.getName());

        if (compressedPath.equals(extractionPath)) {
          // custom extraction path not set, extract at default path
          extractionPath = f.getParent() + "/" + extractDirName;
        } else {
          if (extractionPath.endsWith("/")) {
            extractionPath = extractionPath + extractDirName;
          } else if (!passwordProtected) {
            extractionPath = extractionPath + "/" + extractDirName;
          }
        }

        if (entriesToExtract != null && entriesToExtract.length == 0) entriesToExtract = null;

        final Extractor extractor =
            CompressedHelper.getExtractorInstance(
                extractService.getApplicationContext(),
                f,
                extractionPath,
                new Extractor.OnUpdate() {
                  private int sourceFilesProcessed = 0;

                  @Override
                  public void onStart(long totalBytes, String firstEntryName) {
                    // setting total bytes calculated from zip entries
                    progressHandler.setTotalSize(totalBytes);

                    extractService.addFirstDatapoint(firstEntryName, 1, totalBytes, false);

                    watcherUtil = new ServiceWatcherUtil(progressHandler);
                    watcherUtil.watch(ExtractService.this);
                  }

                  @Override
                  public void onUpdate(String entryPath) {
                    progressHandler.setFileName(entryPath);
                    if (entriesToExtract != null) {
                      progressHandler.setSourceFilesProcessed(sourceFilesProcessed++);
                    }
                  }

                  @Override
                  public void onFinish() {
                    if (entriesToExtract == null) {
                      progressHandler.setSourceFilesProcessed(1);
                    }
                  }

                  @Override
                  public boolean isCancelled() {
                    return progressHandler.getCancelled();
                  }
                },
                ServiceWatcherUtil.UPDATE_POSITION);

        try {
          if (entriesToExtract != null) {
            extractor.extractFiles(entriesToExtract);
          } else {
            extractor.extractEverything();
          }
          return (extractor.getInvalidArchiveEntries().size() == 0);
        } catch (Extractor.EmptyArchiveNotice e) {
          Log.e(TAG, "Archive " + compressedPath + " is an empty archive");
          AppConfig.toast(
              extractService,
              extractService.getString(R.string.error_empty_archive, compressedPath));
          return true;
        } catch (CorruptedInputException e) {
          Log.d(TAG, "Corrupted LZMA input", e);
          return false;
        } catch (IOException e) {
          if (PasswordRequiredException.class.isAssignableFrom(e.getClass())
              || e.getCause() != null
                  && ZipException.class.isAssignableFrom(e.getCause().getClass())) {
            Log.d(TAG, "Archive is password protected.", e);
            if (ArchivePasswordCache.getInstance().containsKey(compressedPath)) {
              ArchivePasswordCache.getInstance().remove(compressedPath);
              AppConfig.toast(
                  extractService,
                  extractService.getString(R.string.error_archive_password_incorrect));
            }
            passwordProtected = true;
            paused = true;
            publishProgress(e);
          } else if (e.getCause() != null
              && UnsupportedRarV5Exception.class.isAssignableFrom(e.getCause().getClass())) {
            Log.e(TAG, "RAR " + compressedPath + " is unsupported V5 archive", e);
            AppConfig.toast(
                extractService,
                extractService.getString(R.string.error_unsupported_v5_rar, compressedPath));
            return false;
          } else {
            Log.e(TAG, "Error while extracting file " + compressedPath, e);
            AppConfig.toast(extractService, extractService.getString(R.string.error));
            paused = true;
            publishProgress(e);
          }
        }
      }
      return false;
    }

    @Override
    protected void onProgressUpdate(IOException... values) {
      super.onProgressUpdate(values);
      if (values.length < 1 || !passwordProtected) return;

      IOException result = values[0];
      ArchivePasswordCache.getInstance().remove(compressedPath);
      GeneralDialogCreation.showPasswordDialog(
          AppConfig.getInstance().getMainActivityContext(),
          (MainActivity) AppConfig.getInstance().getMainActivityContext(),
          AppConfig.getInstance().getUtilsProvider().getAppTheme(),
          R.string.archive_password_prompt,
          R.string.authenticate_password,
          (dialog, which) -> {
            EditText editText = dialog.getView().findViewById(R.id.singleedittext_input);
            ArchivePasswordCache.getInstance().put(compressedPath, editText.getText().toString());
            this.extractService.get().getDataPackages().clear();
            this.paused = false;
            dialog.dismiss();
          },
          ((dialog, which) -> {
            dialog.dismiss();
            toastOnParseError(result);
            cancel(true); // This cancels the AsyncTask...
            progressHandler.setCancelled(true);
            stopSelf(); // and this stops the ExtractService altogether.
            this.paused = false;
          }));
    }

    @Override
    public void onPostExecute(Boolean hasInvalidEntries) {
      ArchivePasswordCache.getInstance().remove(compressedPath);
      final ExtractService extractService = this.extractService.get();
      if (extractService == null) return;

      // check whether watcherutil was initialized. It was not initialized when we got exception
      // in extracting the file
      if (watcherUtil != null) watcherUtil.stopWatch();
      Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
      intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, extractionPath);
      extractService.sendBroadcast(intent);
      extractService.stopSelf();

      if (!hasInvalidEntries)
        AppConfig.toast(extractService, getString(R.string.multiple_invalid_archive_entries));
    }

    @Override
    protected void onCancelled() {
      super.onCancelled();
      ArchivePasswordCache.getInstance().remove(compressedPath);
    }

    private void toastOnParseError(IOException result) {
      Toast.makeText(
              AppConfig.getInstance().getMainActivityContext(),
              AppConfig.getInstance()
                  .getResources()
                  .getString(
                      R.string.cannot_extract_archive,
                      compressedPath,
                      result.getLocalizedMessage()),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * Class used for the client Binder. Because we know this service always runs in the same process
   * as its clients, we don't need to deal with IPC.
   */
  private BroadcastReceiver receiver1 =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          progressHandler.setCancelled(true);
        }
      };

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
}
