package com.amaze.filemanager.utils;

/**
 * Created by vishal on 4/1/17.
 * Inner class that watches over the copy progress and publish progress after certain
 * interval. We wouldn't want to interrupt copy loops by writing too much callbacks, and thus
 * possibly delaying the copy process.
 */


import android.os.Handler;
import android.os.HandlerThread;

import com.amaze.filemanager.services.ProgressHandler;

public class CopyWatcherUtil {

    Handler handler;
    HandlerThread handlerThread;
    ProgressHandler progressHandler;
    Long totalSize;

    // position of byte in total byte size to be copied
    public static long POSITION = 0L;

    /**
     *
     * @param progressHandler to publish progress after certain delay
     * @param totalSize total size of copy operation on files, so we know when to halt the watcher
     */
    public CopyWatcherUtil(ProgressHandler progressHandler, long totalSize) {
        this.progressHandler = progressHandler;
        this.totalSize = totalSize;
        POSITION= 0l;

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
                if (POSITION == totalSize) {
                    // copy complete, free up resources
                    handler.removeCallbacks(this);
                    handlerThread.quit();
                    return;
                }
                handler.postDelayed(this, 1000);
            }
        };

        handler.postDelayed(runnable, 1000);
    }
}
