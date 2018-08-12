/*
 * ExtractService.java
 *
 * Copyright (C) 2014-2018 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam <emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt@gmail.com> and Contributors.
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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.application.AppConfig;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ExtractService extends AbstractProgressiveService {

    Context context;

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
        accentColor = ((AppConfig) getApplication()).getUtilsProvider()
                .getColorPreference()
                .getCurrentUserColorPreferences(this, sharedPreferences).accent;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        customSmallContentViews = new RemoteViews(getPackageName(), R.layout.notification_service_small);
        customBigContentViews = new RemoteViews(getPackageName(), R.layout.notification_service_big);

        Intent stopIntent = new Intent(TAG_BROADCAST_EXTRACT_CANCEL);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 1234, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_zip_box_grey600_36dp,
                getString(R.string.stop_ftp), stopPendingIntent);

        mBuilder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID);
        mBuilder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_zip_box_grey600_36dp)
                .setContentIntent(pendingIntent)
                .setCustomContentView(customSmallContentViews)
                .setCustomBigContentView(customBigContentViews)
                .setCustomHeadsUpContentView(customSmallContentViews)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .addAction(action)
                .setOngoing(true)
                .setColor(accentColor);

        NotificationConstants.setMetadata(getApplicationContext(), mBuilder, NotificationConstants.TYPE_NORMAL);
        startForeground(NotificationConstants.EXTRACT_ID, mBuilder.build());
        initNotificationViews();

        long totalSize = getTotalSize(file);

        progressHandler.setSourceSize(1);
        progressHandler.setTotalSize(totalSize);
        progressHandler.setProgressListener((speed) ->
            publishResults(speed, false, false));

        super.onStartCommand(intent, flags, startId);
        super.progressHalted();
        new DoWork(this, progressHandler, file, extractPath, entries).execute();

        return START_STICKY;
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
    public void onDestroy() {
        unregisterReceiver(receiver1);
    }

    /**
     * Method calculates zip file size to initiate progress
     * Supporting local file extraction progress for now
     */
    private long getTotalSize(String filePath) {
        return new File(filePath).length();
    }

    public class DoWork extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<ExtractService> extractService;
        private String[] entriesToExtract;
        private String extractionPath, compressedPath;
        private ProgressHandler progressHandler;
        private ServiceWatcherUtil watcherUtil;


        private DoWork(ExtractService extractService, ProgressHandler progressHandler, String cpath, String epath,
                       String[] entries) {
            this.extractService = new WeakReference<>(extractService);
            this.progressHandler = progressHandler;
            compressedPath = cpath;
            extractionPath = epath;
            entriesToExtract = entries;
        }

        @Override
        protected Boolean doInBackground(Void... p) {
            final ExtractService extractService = this.extractService.get();
            if(extractService == null) return null;

            File f = new File(compressedPath);
            String extractDirName = CompressedHelper.getFileName(f.getName());

            if (compressedPath.equals(extractionPath)) {
                // custom extraction path not set, extract at default path
                extractionPath = f.getParent() + "/" + extractDirName;
            } else {
                if (extractionPath.endsWith("/")) {
                    extractionPath = extractionPath + extractDirName;
                } else {
                    extractionPath = extractionPath + "/" + extractDirName;
                }
            }

            try {
                if(entriesToExtract.length == 0) entriesToExtract = null;

                final Extractor extractor =
                    CompressedHelper.getExtractorInstance(extractService.getApplicationContext(),
                        f, extractionPath, new Extractor.OnUpdate() {
                            private int sourceFilesProcessed = 0;

                            @Override
                            public void onStart(long totalBytes, String firstEntryName) {
                                // setting total bytes calculated from zip entries
                                progressHandler.setTotalSize(totalBytes);

                                extractService.addFirstDatapoint(firstEntryName,
                                        1, totalBytes, false);

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
                                if (entriesToExtract == null){
                                    progressHandler.setSourceFilesProcessed(1);
                                }
                            }

                            @Override
                            public boolean isCancelled() {
                                return progressHandler.getCancelled();
                            }
                        });

                if (entriesToExtract != null) {
                    extractor.extractFiles(entriesToExtract);
                } else {
                    extractor.extractEverything();
                }
                return (extractor.getInvalidArchiveEntries().size() == 0);
            } catch (IOException e) {
                Log.e("amaze", "Error while extracting file " + compressedPath, e);
                AppConfig.toast(extractService, extractService.getString(R.string.error));
                return false;
            }
        }

        @Override
        public void onPostExecute(Boolean hasInvalidEntries) {
            final ExtractService extractService = this.extractService.get();
            if(extractService == null) return;

            // check whether watcherutil was initialized. It was not initialized when we got exception
            // in extracting the file
            if (watcherUtil != null) watcherUtil.stopWatch();
            Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, extractionPath);
            extractService.sendBroadcast(intent);
            extractService.stopSelf();

            if(!hasInvalidEntries)
                AppConfig.toast(extractService, getString(R.string.multiple_invalid_archive_entries));
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    private BroadcastReceiver receiver1 = new BroadcastReceiver() {
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

