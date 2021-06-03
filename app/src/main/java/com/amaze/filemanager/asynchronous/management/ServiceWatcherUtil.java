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

package com.amaze.filemanager.asynchronous.management;

/**
 * Created by vishal on 4/1/17.
 *
 * <p>Helper class providing helper methods to manage Service startup and it's progress Be advised -
 * this class can only handle progress with one object at a time. Hence, class also provides
 * convenience methods to serialize the service startup.
 */
import static com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil.ServiceStatusCallbacks.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.AbstractRepeatingRunnable;
import com.amaze.filemanager.asynchronous.services.AbstractProgressiveService;
import com.amaze.filemanager.file_operations.utils.UpdatePosition;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.ProgressHandler;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.text.format.Formatter;

import androidx.core.app.NotificationCompat;

public class ServiceWatcherUtil {

  public static final UpdatePosition UPDATE_POSITION =
      (toAdd -> ServiceWatcherUtil.position += toAdd);

  public static int state = STATE_UNSET;

  /**
   * Position of byte in total byte size to be copied. This variable CANNOT be updated from more
   * than one thread simultaneously. This variable should only be updated from an {@link
   * AbstractProgressiveService}'s background thread.
   *
   * @see #postWaiting(Context)
   */
  public static volatile long position = 0L;

  private ProgressHandler progressHandler;

  private static AbstractRepeatingRunnable watcherRepeatingRunnable;
  private static NotificationManager notificationManager;
  private static NotificationCompat.Builder builder;

  private static ConcurrentLinkedQueue<Intent> pendingIntents = new ConcurrentLinkedQueue<>();

  private static int haltCounter = -1;

  /** @param progressHandler to publish progress after certain delay */
  public ServiceWatcherUtil(ProgressHandler progressHandler) {
    this.progressHandler = progressHandler;
    position = 0L;
    haltCounter = -1;
  }

  /**
   * Watches over the service progress without interrupting the worker thread in respective services
   * Method frees up all the resources and handlers after operation completes.
   */
  public void watch(ServiceStatusCallbacks serviceStatusCallbacks) {
    watcherRepeatingRunnable =
        new ServiceWatcherRepeatingRunnable(true, serviceStatusCallbacks, progressHandler);
  }

  private static final class ServiceWatcherRepeatingRunnable extends AbstractRepeatingRunnable {
    private final WeakReference<ServiceStatusCallbacks> serviceStatusCallbacks;
    private final ProgressHandler progressHandler;

    public ServiceWatcherRepeatingRunnable(
        boolean startImmediately,
        ServiceStatusCallbacks serviceStatusCallbacks,
        ProgressHandler progressHandler) {
      super(1, 1, TimeUnit.SECONDS, startImmediately);

      this.serviceStatusCallbacks = new WeakReference<>(serviceStatusCallbacks);
      this.progressHandler = progressHandler;
    }

    @Override
    public void run() {
      final ServiceStatusCallbacks serviceStatusCallbacks = this.serviceStatusCallbacks.get();
      if (serviceStatusCallbacks == null) {
        // the service was destroyed, clean up
        cancel(false);
        return;
      }

      // we don't have a file name yet, wait for service to set
      if (progressHandler.getFileName() == null) {
        return;
      }

      if (position == progressHandler.getWrittenSize()
          && (state != STATE_HALTED && ++haltCounter > 5)) {
        // new position is same as the last second position, and halt counter is past threshold

        String writtenSize =
            Formatter.formatShortFileSize(
                serviceStatusCallbacks.getApplicationContext(), progressHandler.getWrittenSize());
        String totalSize =
            Formatter.formatShortFileSize(
                serviceStatusCallbacks.getApplicationContext(), progressHandler.getTotalSize());

        if (serviceStatusCallbacks.isDecryptService() && writtenSize.equals(totalSize)) {
          // workaround for decryption when we have a length retrieved by
          // CipherInputStream less than the original stream, and hence the total size
          // we passed at the beginning is never reached
          // we try to get a less precise size and make our decision based on that
          progressHandler.addWrittenLength(progressHandler.getTotalSize());
          if (!pendingIntents.isEmpty()) pendingIntents.remove();
          cancel(false);
          return;
        }

        haltCounter = 0;
        state = STATE_HALTED;
        serviceStatusCallbacks.progressHalted();
      } else if (position != progressHandler.getWrittenSize()) {

        if (state == STATE_HALTED) {

          state = STATE_RESUMED;
          haltCounter = 0;
          serviceStatusCallbacks.progressResumed();
        } else {

          // reset the halt counter everytime there is a progress
          // so that it increments only when
          // progress was halted for consecutive time period
          state = STATE_UNSET;
          haltCounter = 0;
        }
      }

      progressHandler.addWrittenLength(position);

      if (position == progressHandler.getTotalSize() || progressHandler.getCancelled()) {
        // process complete, free up resources
        // we've finished the work or process cancelled
        if (!pendingIntents.isEmpty()) pendingIntents.remove();
        cancel(false);
      }
    }
  }

  /**
   * Manually call runnable, before the delay. Fixes race condition which can arise when service has
   * finished execution and stopping self, but the runnable is yet scheduled to be posted. Thus
   * avoids posting any callback after service has stopped.
   */
  public void stopWatch() {
    if (watcherRepeatingRunnable != null && watcherRepeatingRunnable.isAlive()) {
      watcherRepeatingRunnable.cancel(true);
    }
  }

  /**
   * Convenience method to check whether another service is working in background If a service is
   * found working (by checking {@link #watcherRepeatingRunnable} for it's state) then we wait for
   * an interval of 5 secs, before checking on it again.
   *
   * <p>Be advised - this method is not sure to start a new service, especially when app has been
   * closed as there are higher chances for android system to GC the thread when it is running low
   * on memory
   */
  public static synchronized void runService(final Context context, final Intent intent) {
    switch (pendingIntents.size()) {
      case 0:
        context.startService(intent);
        break;
      case 1:
        // initialize waiting handlers
        pendingIntents.add(intent);
        postWaiting(context);
        break;
      case 2:
        // to avoid notifying repeatedly
        pendingIntents.add(intent);
        notificationManager.notify(NotificationConstants.WAIT_ID, builder.build());
        break;
      default:
        pendingIntents.add(intent);
        break;
    }
  }

  /**
   * Helper method to {@link #runService(Context, Intent)} Starts the wait watcher thread if not
   * already started. Halting condition depends on the state of {@link #watcherRepeatingRunnable}
   */
  private static synchronized void postWaiting(final Context context) {
    notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    builder =
        new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID)
            .setContentTitle(context.getString(R.string.waiting_title))
            .setContentText(context.getString(R.string.waiting_content))
            .setAutoCancel(false)
            .setSmallIcon(R.drawable.ic_all_inclusive_white_36dp)
            .setProgress(0, 0, true);

    NotificationConstants.setMetadata(context, builder, NotificationConstants.TYPE_NORMAL);

    new WaitNotificationThread(context, true);
  }

  private static final class WaitNotificationThread extends AbstractRepeatingRunnable {
    private final WeakReference<Context> context;

    private WaitNotificationThread(Context context, boolean startImmediately) {
      super(0, 1, TimeUnit.SECONDS, startImmediately);
      this.context = new WeakReference<>(context);
    }

    @Override
    public void run() {
      if (watcherRepeatingRunnable == null || !watcherRepeatingRunnable.isAlive()) {
        if (pendingIntents.size() == 0) {
          cancel(false);
          return;
        } else {
          if (pendingIntents.size() == 1) {
            notificationManager.cancel(NotificationConstants.WAIT_ID);
          }

          final Context context = this.context.get();
          if (context != null) {
            context.startService(pendingIntents.element());
            cancel(true);
            return;
          }
        }
      }
    }
  }

  public interface ServiceStatusCallbacks {

    int STATE_UNSET = -1;
    int STATE_HALTED = 0;
    int STATE_RESUMED = 1;

    /** Progress has been halted for some reason */
    void progressHalted();

    /** Future extension for possible implementation of pause/resume of services */
    void progressResumed();

    Context getApplicationContext();

    /** This is for a hack, read about it where it's used */
    boolean isDecryptService();
  }
}
