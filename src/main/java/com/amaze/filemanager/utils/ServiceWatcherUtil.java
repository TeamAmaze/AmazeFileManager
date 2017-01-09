package com.amaze.filemanager.utils;

/**
 * Created by vishal on 4/1/17.
 * Inner class that watches over the copy progress and publish progress after certain
 * interval. We wouldn't want to interrupt copy loops by writing too much callbacks, and thus
 * possibly delaying the copy process.
 */


import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.NotificationCompat;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.services.ProgressHandler;

public class ServiceWatcherUtil {

    Handler handler;
    HandlerThread handlerThread;
    ProgressHandler progressHandler;
    long totalSize;

    // position of byte in total byte size to be copied
    public static long POSITION = 0L;

    /**
     *
     * @param progressHandler to publish progress after certain delay
     * @param totalSize total size of copy operation on files, so we know when to halt the watcher
     */
    public ServiceWatcherUtil(ProgressHandler progressHandler, long totalSize) {
        this.progressHandler = progressHandler;
        this.totalSize = totalSize;
        POSITION = 0l;

        handlerThread = new HandlerThread("copy_watcher");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    /**
     * Watches over the copy progress without interrupting the worker
     * {@link GenericCopyThread#thread} thread.
     * Method frees up all the resources and worker threads after copy operation completes.
     */
    public void watch() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

                // we don't have a file name yet, wait for copy to start
                if (progressHandler.getFileName()==null) handler.postDelayed(this, 1000);

                progressHandler.addWrittenLength(POSITION);
                if (POSITION == totalSize || progressHandler.getCancelled()) {
                    // copy complete, free up resources
                    // we've finished the work or copy cancelled
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
     * Convenient method to check whether another service is working in background
     * If a service is found working (by checking and maintaining state of {@link BaseActivity#IS_BOUND}
     * which is further bound to service using {@link android.content.ServiceConnection} for it's state)
     * then we wait for an interval of 5 secs, before checking on it again
     *
     * @param context
     * @param intent
     */
    public static void runService(final Context context, final Intent intent) {

        if (!BaseActivity.IS_BOUND) {
            // we're not bound, no need to proceed further and waste up resources
            context.startService(intent);
            return;
        }

        final HandlerThread handlerThread = new HandlerThread("service_watcher");
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());
        final NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.waiting_title));
        mBuilder.setContentText(context.getString(R.string.waiting_content));
        mBuilder.setAutoCancel(false);
        mBuilder.setSmallIcon(R.drawable.ic_content_copy_white_36dp);
        mBuilder.setProgress(0, 0, true);
        notificationManager.notify(1, mBuilder.build());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!BaseActivity.IS_BOUND) {

                    // service is been finished, let's start this one
                    // and free up resources before returning from here
                    context.startService(intent);
                    notificationManager.cancel(1);
                    handler.removeCallbacks(this);
                    handlerThread.quit();
                    return;
                }
                handler.postDelayed(this, 5000);
            }
        };

        handler.postDelayed(runnable, 5000);
    }
}
