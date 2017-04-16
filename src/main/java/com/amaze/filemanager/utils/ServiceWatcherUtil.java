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

import com.amaze.filemanager.R;

import java.util.ArrayList;

public class ServiceWatcherUtil {

    private Handler handler;
    private static HandlerThread handlerThread;
    private ProgressHandler progressHandler;
    long totalSize;
    private Runnable runnable;

    private static ArrayList<Intent> pendingIntents = new ArrayList<>();

    // position of byte in total byte size to be copied
    public static long POSITION = 0L;

    private static int HAULT_COUNTER = -1;

    public static final int ID_NOTIFICATION_WAIT =  9248;

    /**
     *
     * @param progressHandler to publish progress after certain delay
     * @param totalSize total size of files to be performed, so we know when to halt the watcher
     */
    public ServiceWatcherUtil(ProgressHandler progressHandler, long totalSize) {
        this.progressHandler = progressHandler;
        this.totalSize = totalSize;
        POSITION = 0L;
        HAULT_COUNTER = -1;

        handlerThread = new HandlerThread("service_progress_watcher");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    /**
     * Watches over the service progress without interrupting the worker thread in respective services
     * Method frees up all the resources and handlers after operation completes.
     */
    public void watch() {
        runnable = new Runnable() {
            @Override
            public void run() {

                // we don't have a file name yet, wait for service to set
                if (progressHandler.getFileName()==null) handler.postDelayed(this, 1000);

                progressHandler.addWrittenLength(POSITION);

                if (POSITION == totalSize || progressHandler.getCancelled()) {
                    // process complete, free up resources
                    // we've finished the work or process cancelled
                    handler.removeCallbacks(this);
                    handlerThread.quit();
                    return;
                }

                if (POSITION == progressHandler.getWrittenSize()) {
                    HAULT_COUNTER++;

                    if (HAULT_COUNTER>10) {
                        // we suspect the progress has been haulted for some reason, stop the watcher

                        // workaround for decryption when we have a length retreived by
                        // CipherInputStream less than the orginal stream, and hence the total size
                        // we passed at the beginning is never reached
                        progressHandler.addWrittenLength(totalSize);
                        handler.removeCallbacks(this);
                        handlerThread.quit();
                        return;
                    }
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
     *
     * @param context
     * @param intent
     */
    public static synchronized void runService(final Context context, final Intent intent) {

        /*if (handlerThread==null || !handlerThread.isAlive()) {
            // we're not bound, no need to proceed further and waste up resources
            // start the service directly

            *//**
             * We can actually end up racing at this point with the {@link HandlerThread} started
             * in {@link #init(Context)}. If older service has returned, we already have the runnable
             * waiting to execute in #init, and user is in app, and starts another service, and
             * as this block executes the {@link android.app.Service#onStartCommand(Intent, int, int)}
             * we end up with a context switch to 'service_startup_watcher' in #init, it also starts
             * a new service (as {@link #progressHandler} is not alive yet).
             * Though chances are very slim, but even if this condition occurs, only the progress will
             * be flawed, but the actual operation will go fine, due to android's native serial service
             * execution. #nough' said!
             *//*
            context.startService(intent);
            return;
        }*/

        if (pendingIntents.size()==0) {
            init(context);
        }
        pendingIntents.add(intent);

    }

    /**
     * Helper method to {@link #runService(Context, Intent)}
     * Starts the wait watcher thread if not already started.
     * Halting condition depends on the state of {@link #handlerThread}
     * @param context
     */
    private static synchronized void init(final Context context) {

        final HandlerThread waitingThread = new HandlerThread("service_startup_watcher");
        waitingThread.start();
        final Handler handler = new Handler(waitingThread.getLooper());
        final NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.waiting_title));
        mBuilder.setContentText(context.getString(R.string.waiting_content));
        mBuilder.setAutoCancel(false);
        mBuilder.setSmallIcon(R.drawable.ic_all_inclusive_white_36dp);
        mBuilder.setProgress(0, 0, true);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (handlerThread==null || !handlerThread.isAlive()) {

                    // service is been finished, let's start this one

                    // pop recent intent from pendingIntents
                    context.startService(pendingIntents.remove(pendingIntents.size()-1));

                    if (pendingIntents.size()==0) {
                        // we've done all the work, free up resources (if not already killed by system)
                        notificationManager.cancel(ID_NOTIFICATION_WAIT);
                        handler.removeCallbacks(this);
                        waitingThread.quit();
                        return;
                    } else {

                        notificationManager.notify(ID_NOTIFICATION_WAIT, mBuilder.build());
                    }
                }
                handler.postDelayed(this, 5000);
            }
        };

        handler.postDelayed(runnable, 0);
    }
}
