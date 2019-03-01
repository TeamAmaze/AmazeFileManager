package com.amaze.filemanager.utils;

/**
 * Created by vishal on 4/1/17.
 *
 * Helper class providing helper methods to manage Service startup and it's progress
 * Be advised - this class can only handle progress with one object at a time. Hence, class also provides
 * convenience methods to serialize the service startup.
 */

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.services.AbstractProgressiveService;
import com.amaze.filemanager.ui.notifications.NotificationConstants;

import java.util.concurrent.ConcurrentLinkedQueue;

import static com.amaze.filemanager.utils.ServiceWatcherUtil.ServiceWatcherInteractionInterface.*;
import static com.amaze.filemanager.utils.ServiceWatcherUtil.ServiceWatcherInteractionInterface.STATE_UNSET;

public class ServiceWatcherUtil {

    public static int state = STATE_UNSET;

    /**
     * Position of byte in total byte size to be copied.
     * This variable CANNOT be updated from more than one thread simultaneously.
     * This variable should only be updated from an {@link AbstractProgressiveService}'s background
     * thread.
     *
     * @see #postWaiting(Context)
     */
    public static volatile long position = 0L;

    private Handler handler;
    private static HandlerThread handlerThread;
    private ProgressHandler progressHandler;
    private Runnable runnable;

    private static Handler waitingHandler;
    private static HandlerThread waitingHandlerThread;
    private static NotificationManager notificationManager;
    private static NotificationCompat.Builder builder;

    private static ConcurrentLinkedQueue<Intent> pendingIntents = new ConcurrentLinkedQueue<>();

    private static int haltCounter = -1;

    /**
     * @param progressHandler to publish progress after certain delay
     */
    public ServiceWatcherUtil(ProgressHandler progressHandler) {
        this.progressHandler = progressHandler;
        position = 0L;
        haltCounter = -1;

        handlerThread = new HandlerThread("service_progress_watcher");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    /**
     * Watches over the service progress without interrupting the worker thread in respective services
     * Method frees up all the resources and handlers after operation completes.
     */
    public void watch(ServiceWatcherInteractionInterface interactionInterface) {
        runnable = new Runnable() {
            @Override
            public void run() {

                // we don't have a file name yet, wait for service to set
                if (progressHandler.getFileName()==null) handler.postDelayed(this, 1000);

                if (position == progressHandler.getWrittenSize() &&
                        (state != STATE_HALTED
                                && ++haltCounter >5)) {

                    // new position is same as the last second position, and halt counter is past threshold

                    String writtenSize = Formatter.formatShortFileSize(interactionInterface.getApplicationContext(),
                            progressHandler.getWrittenSize());
                    String totalSize = Formatter.formatShortFileSize(interactionInterface.getApplicationContext(),
                            progressHandler.getTotalSize());

                    if (interactionInterface.isDecryptService() && writtenSize.equals(totalSize)) {
                        // workaround for decryption when we have a length retrieved by
                        // CipherInputStream less than the original stream, and hence the total size
                        // we passed at the beginning is never reached
                        // we try to get a less precise size and make our decision based on that
                        progressHandler.addWrittenLength(progressHandler.getTotalSize());
                        if (!pendingIntents.isEmpty())
                            pendingIntents.remove();
                        handler.removeCallbacks(this);
                        handlerThread.quit();
                        return;
                    }

                    haltCounter = 0;
                    state = STATE_HALTED;
                    interactionInterface.progressHalted();
                } else if (position != progressHandler.getWrittenSize()) {

                    if (state == STATE_HALTED) {

                        state = STATE_RESUMED;
                        haltCounter = 0;
                        interactionInterface.progressResumed();
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
                    if (!pendingIntents.isEmpty())
                        pendingIntents.remove();
                    handler.removeCallbacks(this);
                    handlerThread.quit();
                    return;
                }

                handler.postDelayed(this, 1000);
            }
        };

        handler.postDelayed(runnable, 1000);
    }

    /**
     * Manually call runnable, before the delay. Fixes race condition which can arise when
     * service has finished execution and stopping self, but the runnable is yet scheduled to be posted.
     * Thus avoids posting any callback after service has stopped.
     */
    public void stopWatch() {
        if (handlerThread.isAlive()) handler.post(runnable);
    }

    /**
     * Convenience method to check whether another service is working in background
     * If a service is found working (by checking {@link #handlerThread} for it's state)
     * then we wait for an interval of 5 secs, before checking on it again.
     *
     * Be advised - this method is not sure to start a new service, especially when app has been closed
     * as there are higher chances for android system to GC the thread when it is running low on memory
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
     * Helper method to {@link #runService(Context, Intent)}
     * Starts the wait watcher thread if not already started.
     * Halting condition depends on the state of {@link #handlerThread}
     */
    private static synchronized void postWaiting(final Context context) {
        waitingHandlerThread = new HandlerThread("service_startup_watcher");
        waitingHandlerThread.start();
        waitingHandler = new Handler(waitingHandlerThread.getLooper());
        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID)
                .setContentTitle(context.getString(R.string.waiting_title))
                .setContentText(context.getString(R.string.waiting_content))
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_all_inclusive_white_36dp)
                .setProgress(0, 0, true);

        NotificationConstants.setMetadata(context, builder, NotificationConstants.TYPE_NORMAL);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (handlerThread==null || !handlerThread.isAlive()) {

                    if (pendingIntents.size()==0) {
                        // we've done all the work, free up resources (if not already killed by system)
                        waitingHandler.removeCallbacks(this);
                        waitingHandlerThread.quit();
                        return;
                    } else {
                        if (pendingIntents.size()==1) {
                            notificationManager.cancel(NotificationConstants.WAIT_ID);
                        }
                        context.startService(pendingIntents.element());
                    }
                }

                Log.d(getClass().getSimpleName(), "Processes in progress, delay the check");
                waitingHandler.postDelayed(this, 1000);
            }
        };

        waitingHandler.postDelayed(runnable, 0);
    }

    public interface ServiceWatcherInteractionInterface {

        int STATE_UNSET = -1;
        int STATE_HALTED = 0;
        int STATE_RESUMED = 1;

        /**
         * Progress has been halted for some reason
         */
        void progressHalted();

        /**
         * Future extension for possible implementation of pause/resume of services
         */
        void progressResumed();

        Context getApplicationContext();

        /**
         * This is for a hack, read about it where it's used
         */
        boolean isDecryptService();
    }
}