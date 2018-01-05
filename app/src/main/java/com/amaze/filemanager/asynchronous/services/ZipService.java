/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.CopyDataParcelable;
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

public class ZipService extends Service {

    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    String mZipPath;
    Context c;
    ProgressListener progressListener;
    long totalBytes = 0L;
    private final IBinder mBinder = new LocalBinder();
    private ProgressHandler progressHandler;
    private ArrayList<CopyDataParcelable> dataPackages = new ArrayList<>();

    public static final String KEY_COMPRESS_PATH = "zip_path";
    public static final String KEY_COMPRESS_FILES = "zip_files";
    public static final String KEY_COMPRESS_BROADCAST_CANCEL = "zip_cancel";

    @Override
    public void onCreate() {
        c = getApplicationContext();
        registerReceiver(receiver1, new IntentFilter(KEY_COMPRESS_BROADCAST_CANCEL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        mZipPath = intent.getStringExtra(KEY_COMPRESS_PATH);

        File zipFile = new File(mZipPath);
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (!zipFile.exists()) {
            try {
                zipFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
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
        NotificationConstants.setMetadata(this, mBuilder);
        startForeground(Integer.parseInt("789" + startId), mBuilder.build());

        ArrayList<HybridFileParcelable> baseFiles = intent.getParcelableArrayListExtra(KEY_COMPRESS_FILES);

        new DoWork(startId, baseFiles, mZipPath).execute();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    public class LocalBinder extends Binder {
        public ZipService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ZipService.this;
        }
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public interface ProgressListener {
        void onUpdate(CopyDataParcelable dataPackage);

        void refresh();
    }

    public class DoWork extends AsyncTask<Void, Void, Void> {

        ZipOutputStream zos;

        String zipPath;
        ServiceWatcherUtil watcherUtil;

        private final int notificationId;
        private ArrayList<HybridFileParcelable> baseFiles;

        public DoWork(int id, ArrayList<HybridFileParcelable> baseFiles, String zipPath) {
            notificationId = id;
            this.baseFiles = baseFiles;
            this.zipPath = zipPath;
        }

        public ArrayList<File> toFileArray(ArrayList<HybridFileParcelable> a) {
            ArrayList<File> b = new ArrayList<>();
            for (int i = 0; i < a.size(); i++) {
                b.add(new File(a.get(i).getPath()));
            }
            return b;
        }

        protected Void doInBackground(Void... p) {
            // setting up service watchers and initial data packages
            // finding total size on background thread (this is necessary condition for SMB!)
            totalBytes = FileUtils.getTotalBytes(baseFiles, c);
            progressHandler = new ProgressHandler(baseFiles.size(), totalBytes);
            progressHandler.setProgressListener((fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed) -> {
                publishResults(notificationId, fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed, false);
            });

            CopyDataParcelable intent1 = new CopyDataParcelable(baseFiles.get(0).getName(),
                    baseFiles.size(), totalBytes, false);
            putDataPackage(intent1);

            execute(toFileArray(baseFiles), zipPath);
            return null;
        }

        @Override
        public void onPostExecute(Void b) {

            watcherUtil.stopWatch();
            Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, mZipPath);
            sendBroadcast(intent);
            stopSelf();
        }

        public void execute(ArrayList<File> baseFiles, String zipPath) {

            OutputStream out;
            File zipDirectory = new File(zipPath);
            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
            watcherUtil.watch();

            try {
                out = FileUtil.getOutputStream(zipDirectory, c, totalBytes);
                zos = new ZipOutputStream(new BufferedOutputStream(out));

                int fileProgress = 0;
                for (File file : baseFiles) {
                    if (!progressHandler.getCancelled()) {

                        progressHandler.setFileName(file.getName());
                        progressHandler.setSourceFilesProcessed(++fileProgress);
                        compressFile(file, "");
                    } else return;
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
            if (file.list() == null) {
                return;
            }
            for (File currentFile : file.listFiles()) {

                compressFile(currentFile, path + File.separator + file.getName());

            }
        }
    }

    private void publishResults(int id, String fileName, int sourceFiles, int sourceProgress,
                                long total, long done, int speed, boolean isCompleted) {
        if (!progressHandler.getCancelled()) {
            float progressPercent = ((float) done / total) * 100;
            mBuilder.setProgress(100, Math.round(progressPercent), false);
            mBuilder.setOngoing(true);
            int title = R.string.compressing;
            mBuilder.setContentTitle(c.getResources().getString(title));
            mBuilder.setContentText(new File(fileName).getName() + " " +
                    Formatter.formatFileSize(c, done) + "/" + Formatter.formatFileSize(c, total));
            int id1 = Integer.parseInt("789" + id);
            mNotifyManager.notify(id1, mBuilder.build());
            if (done == total || total == 0) {
                mBuilder.setContentTitle(getString(R.string.compression_complete));
                mBuilder.setContentText("");
                mBuilder.setProgress(100, 100, false);
                mBuilder.setOngoing(false);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult(id1);
                isCompleted = true;
            }

            CopyDataParcelable intent = new CopyDataParcelable(fileName, sourceFiles, sourceProgress,
                    total, done, speed, false, isCompleted);

            putDataPackage(intent);
            if (progressListener != null) {
                progressListener.onUpdate(intent);
                if (isCompleted) progressListener.refresh();
            }
        } else {
            publishCompletedResult(Integer.parseInt("789" + id));
        }
    }

    public void publishCompletedResult(int id1) {
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
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
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(receiver1);
    }

    /**
     * Returns the {@link #dataPackages} list which contains
     * data to be transferred to {@link ProcessViewerFragment}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     *
     * @return
     */
    public synchronized CopyDataParcelable getDataPackage(int index) {
        return this.dataPackages.get(index);
    }

    public synchronized int getDataPackageSize() {
        return this.dataPackages.size();
    }

    /**
     * Puts a {@link CopyDataParcelable} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     *
     * @param dataPackage
     */
    private synchronized void putDataPackage(CopyDataParcelable dataPackage) {
        this.dataPackages.add(dataPackage);
    }

}
