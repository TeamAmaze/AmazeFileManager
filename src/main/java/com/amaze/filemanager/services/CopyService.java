/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 *                      Emmanuel Messulam<emmanuelbendavid@gmail.com>
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

package com.amaze.filemanager.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.GenericCopyUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CopyService extends Service {

    public static final String TAG_COPY_TARGET = "COPY_DIRECTORY";
    public static final String TAG_COPY_SOURCES = "FILE_PATHS";
    public static final String TAG_COPY_OPEN_MODE = "MODE"; // target open mode
    public static final String TAG_COPY_MOVE = "move";
    private static final String TAG_COPY_START_ID = "id";

    public static final String TAG_BROADCAST_COPY_CANCEL = "copycancel";

    // list of data packages, to initiate chart in process viewer fragment
    private ArrayList<DataPackage> dataPackages = new ArrayList<>();
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private Context c;

    private ProgressListener progressListener;
    private final IBinder mBinder = new LocalBinder();
    private ProgressHandler progressHandler;
    private ServiceWatcherUtil watcherUtil;

    private long totalSize = 0L;
    private int totalSourceFiles = 0;
    private int sourceProgress = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        c = getApplicationContext();
        registerReceiver(receiver3, new IntentFilter(TAG_BROADCAST_COPY_CANCEL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Bundle b = new Bundle();
        ArrayList<BaseFile> files = intent.getParcelableArrayListExtra(TAG_COPY_SOURCES);
        String targetPath = intent.getStringExtra(TAG_COPY_TARGET);
        int mode = intent.getIntExtra(TAG_COPY_OPEN_MODE, OpenMode.UNKNOWN.ordinal());
        final boolean move = intent.getBooleanExtra(TAG_COPY_MOVE, false);

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        b.putInt(TAG_COPY_START_ID, startId);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder = new NotificationCompat.Builder(c);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(getResources().getString(R.string.copying))
                .setSmallIcon(R.drawable.ic_content_copy_white_36dp);

        startForeground(Integer.parseInt("456" + startId), mBuilder.build());

        b.putBoolean(TAG_COPY_MOVE, move);
        b.putString(TAG_COPY_TARGET, targetPath);
        b.putInt(TAG_COPY_OPEN_MODE, mode);
        b.putParcelableArrayList(TAG_COPY_SOURCES, files);

        //going async
        new DoInBackground().execute(b);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    /**
     * Helper method to calculate source files size
     * @param files
     * @param context
     * @return
     */
    long getTotalBytes(ArrayList<BaseFile> files, Context context) {
        long totalBytes = 0;
        for (BaseFile file : files) {
            if (file.isDirectory()) totalBytes += file.folderSize(context);
            else totalBytes += file.length(context);
        }
        return totalBytes;
    }

    public void onDestroy() {
        this.unregisterReceiver(receiver3);
    }

    private class DoInBackground extends AsyncTask<Bundle, Void, Integer> {
        ArrayList<BaseFile> sourceFiles;
        boolean move;
        Copy copy;

        protected Integer doInBackground(Bundle... p1) {

            sourceFiles = p1[0].getParcelableArrayList(TAG_COPY_SOURCES);
            final int id = p1[0].getInt(TAG_COPY_START_ID);

            // setting up service watchers and initial data packages
            // finding total size on background thread (this is necessary condition for SMB!)
            totalSize = getTotalBytes(sourceFiles, c);
            totalSourceFiles = sourceFiles.size();
            progressHandler = new ProgressHandler(totalSourceFiles, totalSize);

            progressHandler.setProgressListener(new ProgressHandler.ProgressListener() {

                @Override
                public void onProgressed(String fileName, int sourceFiles, int sourceProgress,
                                         long totalSize, long writtenSize, int speed) {
                    publishResults(id, fileName, sourceFiles, sourceProgress, totalSize,
                            writtenSize, speed, false, move);
                }
            });

            watcherUtil = new ServiceWatcherUtil(progressHandler, totalSize);

            DataPackage intent1 = new DataPackage();
            intent1.setName(sourceFiles.get(0).getName());
            intent1.setSourceFiles(sourceFiles.size());
            intent1.setSourceProgress(0);
            intent1.setTotal(totalSize);
            intent1.setByteProgress(0);
            intent1.setSpeedRaw(0);
            intent1.setMove(move);
            intent1.setCompleted(false);
            putDataPackage(intent1);

            String targetPath = p1[0].getString(TAG_COPY_TARGET);
            move = p1[0].getBoolean(TAG_COPY_MOVE);
            copy = new Copy();
            copy.execute(sourceFiles, targetPath, move,
                    OpenMode.getOpenMode(p1[0].getInt(TAG_COPY_OPEN_MODE)));
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {

            super.onPostExecute(b);
            //  publishResults(b, "", totalSourceFiles, totalSourceFiles, totalSize, totalSize, 0, true, move);
            // stopping watcher if not yet finished
            watcherUtil.stopWatch();
            generateNotification(copy.failedFOps, move);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            stopSelf();
        }

        class Copy {

            ArrayList<HFile> failedFOps;
            ArrayList<BaseFile> toDelete;

            Copy() {
                failedFOps = new ArrayList<>();
                toDelete = new ArrayList<>();
            }

            /**
             * Method iterate through files to be copied
             *
             * @param sourceFiles
             * @param targetPath
             * @param move
             * @param mode        target file open mode (current path's open mode)
             */
            public void execute(final ArrayList<BaseFile> sourceFiles, final String targetPath,
                                final boolean move, OpenMode mode) {

                // initial start of copy, initiate the watcher
                watcherUtil.watch();

                if (FileUtil.checkFolder((targetPath), c) == 1) {
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        sourceProgress = i;
                        BaseFile f1 = (sourceFiles.get(i));
                        Log.e("Copy", "basefile\t" + f1.getPath());

                        try {

                            HFile hFile = new HFile(mode, targetPath, sourceFiles.get(i).getName(),
                                    f1.isDirectory());
                            if (!progressHandler.getCancelled()) {

                                if (!f1.isSmb()
                                        && (f1.getMode() == OpenMode.ROOT || mode == OpenMode.ROOT)
                                        && BaseActivity.rootMode) {
                                    // either source or target are in root
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
                            Log.e("Copy", "Got exception checkout");

                            failedFOps.add(sourceFiles.get(i));
                            for (int j = i + 1; j < sourceFiles.size(); j++)
                                failedFOps.add(sourceFiles.get(j));
                            break;
                        }
                    }

                } else if (BaseActivity.rootMode) {
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        if (!progressHandler.getCancelled()) {

                            HFile hFile = new HFile(mode, targetPath, sourceFiles.get(i).getName(),
                                    sourceFiles.get(i).isDirectory());
                            progressHandler.setSourceFilesProcessed(++sourceProgress);
                            progressHandler.setFileName(sourceFiles.get(i).getName());
                            copyRoot(sourceFiles.get(i), hFile, move);
                            /*if(checkFiles(new HFile(sourceFiles.get(i).getMode(),path),
                            new HFile(OpenMode.ROOT,targetPath+"/"+name))){
                                failedFOps.add(sourceFiles.get(i));
                            }*/
                        }
                    }


                } else {
                    for (BaseFile f : sourceFiles) failedFOps.add(f);
                    return;
                }

                // making sure to delete files after copy operation is done
                // and not if the copy was cancelled
                if (move && !progressHandler.getCancelled()) {
                    ArrayList<BaseFile> toDelete = new ArrayList<>();
                    for (BaseFile a : sourceFiles) {
                        if (!failedFOps.contains(a))
                            toDelete.add(a);
                    }
                    new DeleteTask(getContentResolver(), c).execute((toDelete));
                }
            }

            void copyRoot(BaseFile sourceFile, HFile targetFile, boolean move) {

                try {
                    if (!move) RootUtils.copy(sourceFile.getPath(), targetFile.getPath());
                    else if (move) RootUtils.move(sourceFile.getPath(), targetFile.getPath());
                    ServiceWatcherUtil.POSITION += sourceFile.getSize();
                } catch (RootNotPermittedException e) {
                    failedFOps.add(sourceFile);
                    e.printStackTrace();
                }
                Futils.scanFile(targetFile.getPath(), c);
            }

            private void copyFiles(final BaseFile sourceFile, final HFile targetFile,
                                   ProgressHandler progressHandler) throws IOException {

                if (sourceFile.isDirectory()) {
                    if (progressHandler.getCancelled()) return;

                    if (!targetFile.exists()) targetFile.mkdir(c);

                    // various checks
                    // 1. source file and target file doesn't end up in loop
                    // 2. source file has a valid name or not
                    if (!Operations.isFileNameValid(sourceFile.getName())
                            || Operations.isCopyLoopPossible(sourceFile, targetFile)) {
                        failedFOps.add(sourceFile);
                        return;
                    }
                    targetFile.setLastModified(sourceFile.lastModified());

                    if(progressHandler.getCancelled()) return;
                    ArrayList<BaseFile> filePaths = sourceFile.listFiles(c, false);
                    for (BaseFile file : filePaths) {
                        HFile destFile = new HFile(targetFile.getMode(), targetFile.getPath(),
                                file.getName(), file.isDirectory());
                        copyFiles(file, destFile, progressHandler);
                    }
                } else {
                    if (progressHandler.getCancelled()) return;
                    if (!Operations.isFileNameValid(sourceFile.getName())) {
                        failedFOps.add(sourceFile);
                        return;
                    }

                    GenericCopyUtil copyUtil = new GenericCopyUtil(c);

                    progressHandler.setFileName(sourceFile.getName());
                    copyUtil.copy(sourceFile, targetFile);
                }
            }
        }
    }

    /**
     * Displays a notification, sends intent and cancels progress if there were some failures
     * in copy progress
     *
     * @param failedOps
     * @param move
     */
    void generateNotification(ArrayList<HFile> failedOps, boolean move) {

        mNotifyManager.cancelAll();

        if(failedOps.size()==0) return;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c);
        mBuilder.setContentTitle(c.getString(R.string.operationunsuccesful));
        mBuilder.setContentText(c.getString(R.string.copy_error).replace("%s",
                move ? c.getString(R.string.moved) : c.getString(R.string.copied)));
        mBuilder.setAutoCancel(true);

        progressHandler.setCancelled(true);

        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra("move", move);

        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pIntent);
        mBuilder.setSmallIcon(R.drawable.ic_content_copy_white_36dp);

        mNotifyManager.notify(741, mBuilder.build());

        intent=new Intent(MainActivity.TAG_INTENT_FILTER_GENERAL);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra(TAG_COPY_MOVE, move);

        sendBroadcast(intent);
    }

    /**
     * Publish the results of the progress to notification and {@link DataPackage}
     * and eventually to {@link com.amaze.filemanager.fragments.ProcessViewer}
     *
     * @param id             id of current service
     * @param fileName       file name of current file being copied
     * @param sourceFiles    total number of files selected by user for copy
     * @param sourceProgress files been copied out of them
     * @param totalSize      total size of selected items to copy
     * @param writtenSize    bytes successfully copied
     * @param speed          number of bytes being copied per sec
     * @param isComplete     whether operation completed or ongoing (not supported at the moment)
     * @param move           if the files are to be moved
     */
    private void publishResults(int id, String fileName, int sourceFiles, int sourceProgress,
                                long totalSize, long writtenSize, int speed, boolean isComplete,
                                boolean move) {
        if (!progressHandler.getCancelled()) {

            //notification
            float progressPercent = ((float) writtenSize / totalSize) * 100;
            mBuilder.setProgress(100, Math.round(progressPercent), false);
            mBuilder.setOngoing(true);
            int title = R.string.copying;
            if (move) title = R.string.moving;
            mBuilder.setContentTitle(c.getResources().getString(title));
            mBuilder.setContentText(fileName + " " + Formatter.formatFileSize(c, writtenSize) + "/" +
                    Formatter.formatFileSize(c, totalSize));
            int id1 = Integer.parseInt("456" + id);
            mNotifyManager.notify(id1, mBuilder.build());
            if (writtenSize == totalSize || totalSize == 0) {
                if (move) {

                    //mBuilder.setContentTitle(getString(R.string.move_complete));
                    // set progress to indeterminate as deletion might still be going on from source
                    mBuilder.setProgress(0, 0, true);
                } else {

                    mBuilder.setContentTitle(getString(R.string.copy_complete));
                    mBuilder.setProgress(0, 0, false);
                }
                mBuilder.setContentText("");
                mBuilder.setOngoing(false);
                mBuilder.setAutoCancel(true);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult(id1);
            }

            //for processviewer
            DataPackage intent = new DataPackage();
            intent.setName(fileName);
            intent.setSourceFiles(sourceFiles);
            intent.setSourceProgress(sourceProgress);
            intent.setTotal(totalSize);
            intent.setByteProgress(writtenSize);
            intent.setSpeedRaw(speed);
            intent.setMove(move);
            intent.setCompleted(isComplete);
            putDataPackage(intent);
            if (progressListener != null) {
                progressListener.onUpdate(intent);
                if (isComplete) progressListener.refresh();
            }
        } else publishCompletedResult(Integer.parseInt("456" + id));
    }

    public void publishCompletedResult(int id1) {
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //check if copy is successful
    // avoid using the method as there is no way to know when we would be returning from command callbacks
    // rather confirm from the command result itself, inside it's callback
    boolean checkFiles(HFile hFile1, HFile hFile2) throws RootNotPermittedException {
        if (RootHelper.isDirectory(hFile1.getPath(), BaseActivity.rootMode, 5)) {
            if (RootHelper.fileExists(hFile2.getPath())) return false;
            ArrayList<BaseFile> baseFiles = RootHelper.getFilesList(hFile1.getPath(), true, true, null);
            if (baseFiles.size() > 0) {
                boolean b = true;
                for (BaseFile baseFile : baseFiles) {
                    if (!checkFiles(new HFile(baseFile.getMode(), baseFile.getPath()),
                            new HFile(hFile2.getMode(), hFile2.getPath() + "/" + (baseFile.getName()))))
                        b = false;
                }
                return b;
            }
            return RootHelper.fileExists(hFile2.getPath());
        } else {
            ArrayList<BaseFile> baseFiles = RootHelper.getFilesList(hFile1.getParent(), true, true, null);
            int i = -1;
            int index = -1;
            for (BaseFile b : baseFiles) {
                i++;
                if (b.getPath().equals(hFile1.getPath())) {
                    index = i;
                    break;
                }
            }
            ArrayList<BaseFile> baseFiles1 = RootHelper.getFilesList(hFile1.getParent(), true, true, null);
            int i1 = -1;
            int index1 = -1;
            for (BaseFile b : baseFiles1) {
                i1++;
                if (b.getPath().equals(hFile1.getPath())) {
                    index1 = i1;
                    break;
                }
            }
            return baseFiles.get(index).getSize() == baseFiles1.get(index1).getSize();
        }
    }

    private BroadcastReceiver receiver3 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //cancel operation
            progressHandler.setCancelled(true);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public CopyService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CopyService.this;
        }
    }

    public interface ProgressListener {
        void onUpdate(DataPackage dataPackage);

        void refresh();
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Returns the {@link #dataPackages} list which contains
     * data to be transferred to {@link com.amaze.filemanager.fragments.ProcessViewer}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
     *
     * @return
     */
    public synchronized DataPackage getDataPackage(int index) {
        return this.dataPackages.get(index);
    }

    public synchronized int getDataPackageSize() {
        return this.dataPackages.size();
    }

    /**
     * Puts a {@link DataPackage} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
     *
     * @param dataPackage
     */
    private synchronized void putDataPackage(DataPackage dataPackage) {
        this.dataPackages.add(dataPackage);
    }

}
