/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 * Copyright (C) 2017-2018 Emmanuel Messulam <emmanuelbendavid@gmail.com>
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

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipService extends ProgressiveServiceAbstract {

    public static final String KEY_COMPRESS_PATH = "zip_path";
    public static final String KEY_COMPRESS_FILES = "zip_files";
    public static final String KEY_COMPRESS_BROADCAST_CANCEL = "zip_cancel";

    private final IBinder mBinder = new ObtainableServiceBinder<>(this);

    private CompressAsyncTask asyncTask;


    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private ProgressHandler progressHandler = new ProgressHandler();
    private volatile float progressPercent = 0f;
    private ProgressListener progressListener;
    // list of data packages, to initiate chart in process viewer fragment
    private ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();

    @Override
    public void onCreate() {
        registerReceiver(receiver1, new IntentFilter(KEY_COMPRESS_BROADCAST_CANCEL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        String mZipPath = intent.getStringExtra(KEY_COMPRESS_PATH);

        ArrayList<HybridFileParcelable> baseFiles = intent.getParcelableArrayListExtra(KEY_COMPRESS_FILES);

        File zipFile = new File(mZipPath);

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (!zipFile.exists()) {
            try {
                zipFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder = new NotificationCompat.Builder(this, NotificationConstants.CHANNEL_NORMAL_ID)
                .setContentIntent(pendingIntent)
                .setContentTitle(getResources().getString(R.string.compressing))
                .setSmallIcon(R.drawable.ic_zip_box_grey600_36dp);

        NotificationConstants.setMetadata(this, mBuilder, NotificationConstants.TYPE_NORMAL);
        startForeground(NotificationConstants.ZIP_ID, mBuilder.build());

        super.onStartCommand(intent, flags, startId);

        asyncTask = new CompressAsyncTask(this, baseFiles, mZipPath);
        asyncTask.execute();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public ZipService getServiceType() {
        return this;
    }

    @Override
    public void initVariables() {

        super.mNotifyManager = mNotifyManager;
        super.mBuilder = mBuilder;
        super.notificationID = NotificationConstants.ZIP_ID;
        super.progressPercent = progressPercent;
        super.progressListener = progressListener;
        super.dataPackages = dataPackages;
        super.progressHandler = progressHandler;
    }

    public class CompressAsyncTask extends AsyncTask<Void, Void, Void> {

        @SuppressLint("StaticFieldLeak")
        private ZipService zipService;
        private ZipOutputStream zos;
        private String zipPath;
        private ServiceWatcherUtil watcherUtil;
        private long totalBytes = 0L;
        private ArrayList<HybridFileParcelable> baseFiles;

        public CompressAsyncTask(ZipService zipService, ArrayList<HybridFileParcelable> baseFiles, String zipPath) {
            this.zipService = zipService;
            this.baseFiles = baseFiles;
            this.zipPath = zipPath;
        }

        protected Void doInBackground(Void... p1) {
            // setting up service watchers and initial data packages
            // finding total size on background thread (this is necessary condition for SMB!)
            totalBytes = FileUtils.getTotalBytes(baseFiles, zipService.getApplicationContext());

            progressHandler.setSourceSize(baseFiles.size());
            progressHandler.setTotalSize(totalBytes);
            progressHandler.setProgressListener((fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed) -> {
                publishResults(fileName, sourceFiles, sourceProgress, totalSize,
                        writtenSize, speed, false, false);
            });


            zipService.addFirstDatapoint(baseFiles.get(0).getName(), baseFiles.size(), totalBytes, false);

            execute(zipService.getApplicationContext(), FileUtils.hybridListToFileArrayList(baseFiles), zipPath);
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressHandler.setCancelled(true);
            File zipFile = new File(zipPath);
            if (zipFile.exists()) zipFile.delete();
        }

        @Override
        public void onPostExecute(Void a) {
            watcherUtil.stopWatch();
            Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, zipPath);
            zipService.sendBroadcast(intent);
            zipService.stopSelf();
        }

        public void execute(final @NonNull Context context, ArrayList<File> baseFiles, String zipPath) {
            OutputStream out;
            File zipDirectory = new File(zipPath);
            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
            watcherUtil.watch(ZipService.this);

            try {
                out = FileUtil.getOutputStream(zipDirectory, context);
                zos = new ZipOutputStream(new BufferedOutputStream(out));

                int fileProgress = 0;
                for (File file : baseFiles) {
                    if (isCancelled()) return;

                    progressHandler.setFileName(file.getName());
                    progressHandler.setSourceFilesProcessed(++fileProgress);
                    compressFile(file, "");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    zos.flush();
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void compressFile(File file, String path) throws IOException, NullPointerException {
            if (!file.isDirectory()) {
                if (progressHandler.getCancelled()) return;

                byte[] buf = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                int len;
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                zos.putNextEntry(new ZipEntry(path + "/" + file.getName()));
                while ((len = in.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION += len;
                }
                in.close();
                return;
            }

            if (file.list() == null) return;

            for (File currentFile : file.listFiles()) {
                compressFile(currentFile, path + File.separator + file.getName());
            }
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */

    private BroadcastReceiver receiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            asyncTask.cancel(true);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(receiver1);
    }

}
