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

import java.util.ArrayList;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.fragments.ProcessViewerFragment;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.Utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.text.format.Formatter;
import android.widget.RemoteViews;

import androidx.annotation.CallSuper;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 28/11/2017, at 19:32. */
public abstract class AbstractProgressiveService extends Service
    implements ServiceWatcherUtil.ServiceStatusCallbacks {

  private boolean isNotificationTitleSet = false;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return super.onStartCommand(intent, flags, startId);
  }

  protected abstract NotificationManager getNotificationManager();

  protected abstract NotificationCompat.Builder getNotificationBuilder();

  protected abstract int getNotificationId();

  protected abstract @StringRes int getTitle(boolean move);

  protected abstract RemoteViews getNotificationCustomViewSmall();

  protected abstract RemoteViews getNotificationCustomViewBig();

  public abstract ProgressListener getProgressListener();

  public abstract void setProgressListener(ProgressListener progressListener);

  /** @return list of data packages, to initiate chart in process viewer fragment */
  protected abstract ArrayList<DatapointParcelable> getDataPackages();

  protected abstract ProgressHandler getProgressHandler();

  protected abstract void clearDataPackages();

  @Override
  public void progressHalted() {
    // set notification to indeterminate unless progress resumes
    getNotificationCustomViewSmall()
        .setProgressBar(R.id.notification_service_progressBar_small, 0, 0, true);
    getNotificationCustomViewBig()
        .setProgressBar(R.id.notification_service_progressBar_big, 0, 0, true);
    getNotificationCustomViewBig()
        .setTextViewText(
            R.id.notification_service_textView_timeRemaining_big, getString(R.string.unknown));
    getNotificationCustomViewBig()
        .setTextViewText(
            R.id.notification_service_textView_transferRate_big, getString(R.string.unknown));
    getNotificationManager().notify(getNotificationId(), getNotificationBuilder().build());
  }

  @Override
  public void progressResumed() {
    // set notification to indeterminate unless progress resumes
    getNotificationCustomViewSmall()
        .setProgressBar(
            R.id.notification_service_progressBar_small,
            100,
            Math.round(getProgressHandler().getPercentProgress()),
            false);
    getNotificationCustomViewBig()
        .setProgressBar(
            R.id.notification_service_progressBar_big,
            100,
            Math.round(getProgressHandler().getPercentProgress()),
            false);
    getNotificationManager().notify(getNotificationId(), getNotificationBuilder().build());
  }

  @Override
  @CallSuper
  public void onDestroy() {
    super.onDestroy();
    // remove the listener on destruction to prevent
    // implicit AbstractProgressiveService instance from leaking (as "this")
    getProgressHandler().setProgressListener(null);
  }

  /**
   * Publish the results of the progress to notification and {@link DatapointParcelable} and
   * eventually to {@link ProcessViewerFragment}
   *
   * @param speed number of bytes being copied per sec
   * @param isComplete whether operation completed or ongoing (not supported at the moment)
   * @param move if the files are to be moved
   */
  public final void publishResults(long speed, boolean isComplete, boolean move) {
    if (!getProgressHandler().getCancelled()) {
      String fileName = getProgressHandler().getFileName();
      long totalSize = getProgressHandler().getTotalSize();
      long writtenSize = getProgressHandler().getWrittenSize();

      if (!isNotificationTitleSet) {
        getNotificationBuilder().setSubText(getString(getTitle(move)));
        isNotificationTitleSet = true;
      }

      if (ServiceWatcherUtil.state != ServiceWatcherUtil.ServiceStatusCallbacks.STATE_HALTED) {

        String written =
            Formatter.formatFileSize(this, writtenSize)
                + "/"
                + Formatter.formatFileSize(this, totalSize);
        getNotificationCustomViewBig()
            .setTextViewText(R.id.notification_service_textView_filename_big, fileName);
        getNotificationCustomViewSmall()
            .setTextViewText(R.id.notification_service_textView_filename_small, fileName);
        getNotificationCustomViewBig()
            .setTextViewText(R.id.notification_service_textView_written_big, written);
        getNotificationCustomViewSmall()
            .setTextViewText(R.id.notification_service_textView_written_small, written);
        getNotificationCustomViewBig()
            .setTextViewText(
                R.id.notification_service_textView_transferRate_big,
                Formatter.formatFileSize(this, speed) + "/s");

        String remainingTime;
        if (speed != 0) {
          remainingTime = Utils.formatTimer(Math.round((totalSize - writtenSize) / speed));
        } else {
          remainingTime = getString(R.string.unknown);
        }
        getNotificationCustomViewBig()
            .setTextViewText(R.id.notification_service_textView_timeRemaining_big, remainingTime);
        getNotificationCustomViewSmall()
            .setProgressBar(
                R.id.notification_service_progressBar_small,
                100,
                Math.round(getProgressHandler().getPercentProgress()),
                false);
        getNotificationCustomViewBig()
            .setProgressBar(
                R.id.notification_service_progressBar_big,
                100,
                Math.round(getProgressHandler().getPercentProgress()),
                false);
        getNotificationManager().notify(getNotificationId(), getNotificationBuilder().build());
      }

      if (writtenSize == totalSize || totalSize == 0) {
        if (move && getNotificationId() == NotificationConstants.COPY_ID) {

          // mBuilder.setContentTitle(getString(R.string.move_complete));
          // set progress to indeterminate as deletion might still be going on from source
          // while moving the file
          getNotificationCustomViewSmall()
              .setProgressBar(R.id.notification_service_progressBar_small, 0, 0, true);
          getNotificationCustomViewBig()
              .setProgressBar(R.id.notification_service_progressBar_big, 0, 0, true);

          getNotificationCustomViewBig()
              .setTextViewText(
                  R.id.notification_service_textView_filename_big, getString(R.string.processing));
          getNotificationCustomViewSmall()
              .setTextViewText(
                  R.id.notification_service_textView_filename_small,
                  getString(R.string.processing));
          getNotificationCustomViewBig()
              .setTextViewText(
                  R.id.notification_service_textView_timeRemaining_big,
                  getString(R.string.unknown));
          getNotificationCustomViewBig()
              .setTextViewText(
                  R.id.notification_service_textView_transferRate_big, getString(R.string.unknown));

          getNotificationBuilder().setOngoing(false);
          getNotificationBuilder().setAutoCancel(true);
          getNotificationManager().notify(getNotificationId(), getNotificationBuilder().build());
        } else {
          publishCompletedResult(getNotificationId());
        }
      }

      // for processviewer
      DatapointParcelable intent =
          new DatapointParcelable(
              fileName,
              getProgressHandler().getSourceSize(),
              getProgressHandler().getSourceFilesProcessed(),
              totalSize,
              writtenSize,
              speed,
              move,
              isComplete);
      // putDataPackage(intent);
      addDatapoint(intent);
    } else publishCompletedResult(getNotificationId());
  }

  private void publishCompletedResult(int id1) {
    try {
      getNotificationManager().cancel(id1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void addFirstDatapoint(String name, int amountOfFiles, long totalBytes, boolean move) {
    if (!getDataPackages().isEmpty()) {
      throw new IllegalStateException("This is not the first datapoint!");
    }

    DatapointParcelable intent1 =
        DatapointParcelable.Companion.buildDatapointParcelable(
            name, amountOfFiles, totalBytes, move);
    putDataPackage(intent1);
  }

  protected void addDatapoint(DatapointParcelable datapoint) {
    if (getDataPackages().isEmpty()) {
      throw new IllegalStateException("This is the first datapoint!");
    }

    putDataPackage(datapoint);
    if (getProgressListener() != null) {
      getProgressListener().onUpdate(datapoint);
      if (datapoint.getCompleted()) getProgressListener().refresh();
    }
  }

  /**
   * Returns the {@link #getDataPackages()} list which contains data to be transferred to {@link
   * ProcessViewerFragment} Method call is synchronized so as to avoid modifying the list by {@link
   * ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)} is
   * executing the callbacks in {@link ProcessViewerFragment}
   */
  public final synchronized DatapointParcelable getDataPackage(int index) {
    return getDataPackages().get(index);
  }

  public final synchronized int getDataPackageSize() {
    return getDataPackages().size();
  }

  /**
   * Puts a {@link DatapointParcelable} into a list Method call is synchronized so as to avoid
   * modifying the list by {@link ServiceWatcherUtil#handlerThread} while {@link
   * MainActivity#runOnUiThread(Runnable)} is executing the callbacks in {@link
   * ProcessViewerFragment}
   */
  private synchronized void putDataPackage(DatapointParcelable dataPackage) {
    getDataPackages().add(dataPackage);
  }

  public interface ProgressListener {
    void onUpdate(DatapointParcelable dataPackage);

    void refresh();
  }

  @Override
  public boolean isDecryptService() {
    return false;
  }

  /** Displays a notification, sends intent and cancels progress if there were some failures */
  void finalizeNotification(ArrayList<HybridFile> failedOps, boolean move) {
    clearDataPackages();

    if (!move) getNotificationManager().cancelAll();

    if (failedOps.size() == 0) return;
    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(
            getApplicationContext(), NotificationConstants.CHANNEL_NORMAL_ID);
    mBuilder.setContentTitle(getString(R.string.operation_unsuccesful));

    mBuilder.setContentText(
        getString(R.string.copy_error, getString(getTitle(move)).toLowerCase()));
    mBuilder.setAutoCancel(true);

    getProgressHandler().setCancelled(true);

    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
    intent.putExtra("move", move);

    PendingIntent pIntent =
        PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    mBuilder.setContentIntent(pIntent);
    mBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);

    getNotificationManager().notify(NotificationConstants.FAILED_ID, mBuilder.build());

    intent = new Intent(MainActivity.TAG_INTENT_FILTER_GENERAL);
    intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);

    sendBroadcast(intent);
  }

  /** Initializes notification views to initial (processing..) state */
  public void initNotificationViews() {
    getNotificationCustomViewBig()
        .setTextViewText(
            R.id.notification_service_textView_filename_big, getString(R.string.processing));
    getNotificationCustomViewSmall()
        .setTextViewText(
            R.id.notification_service_textView_filename_small, getString(R.string.processing));

    String zeroBytesFormat = Formatter.formatFileSize(this, 0l);

    getNotificationCustomViewBig()
        .setTextViewText(R.id.notification_service_textView_written_big, zeroBytesFormat);
    getNotificationCustomViewSmall()
        .setTextViewText(R.id.notification_service_textView_written_small, zeroBytesFormat);
    getNotificationCustomViewBig()
        .setTextViewText(
            R.id.notification_service_textView_transferRate_big, zeroBytesFormat + "/s");

    getNotificationCustomViewBig()
        .setTextViewText(
            R.id.notification_service_textView_timeRemaining_big, getString(R.string.unknown));
    getNotificationCustomViewSmall()
        .setProgressBar(R.id.notification_service_progressBar_small, 0, 0, true);
    getNotificationCustomViewBig()
        .setProgressBar(R.id.notification_service_progressBar_big, 0, 0, true);
    getNotificationManager().notify(getNotificationId(), getNotificationBuilder().build());
  }
}
