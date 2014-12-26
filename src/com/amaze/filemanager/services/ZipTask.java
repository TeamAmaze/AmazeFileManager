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

package com.amaze.filemanager.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.Futils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipTask extends Service {
    public final String EXTRACT_CONDITION = "ZIPPING";
    public final String EXTRACT_PROGRESS = "ZIP_PROGRESS";
    public final String EXTRACT_COMPLETED = "ZIP_COMPLETED";

    Futils utils = new Futils();
    // Binder given to clients
    HashMap<Integer, Boolean> hash = new HashMap<Integer, Boolean>();
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        Notification notification = new Notification(R.drawable.ic_doc_compressed,getResources().getString(R.string.Zipping_fles), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this,getResources().getString(R.string.Zipping_fles), "", pendingIntent);
        startForeground(004, notification);
        registerReceiver(receiver1, new IntentFilter("zipcancel"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String name = intent.getStringExtra("name");
        File c = new File(name);
        if (!c.exists()) {
            try {
                c.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        ArrayList<String> a = intent.getStringArrayListExtra("files");
        b.putInt("id", startId);
        b.putStringArrayList("files", a);
        b.putString("name", name);
        new Doback().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, b);
        hash.put(startId, true);
        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
    }
Context c=this;
    public class Doback extends AsyncTask<Bundle, Void, Integer> {
        ArrayList<String> files;

        public Doback() {
        }

        long totalBytes = 0L;
        String name;

        protected Integer doInBackground(Bundle... p1) {
            mBuilder = new NotificationCompat.Builder(c);
            mBuilder.setContentTitle(getResources().getString(R.string.zipping))

                    .setSmallIcon(R.drawable.ic_doc_compressed);
            int id = p1[0].getInt("id");
            ArrayList<String> a = p1[0].getStringArrayList("files");
            name = p1[0].getString("name");
            new zip().execute(id, utils.toFileArray(a), name);
            // TODO: Implement this method
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults(b, name, 100, true,0,totalBytes);
            publishResult(false);
            stopSelf(b);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
        }

    }

    private void publishResults(int id, String fileName, int i, boolean b,long done,long total) {
        if (hash.get(id)) {
            mBuilder.setProgress(100, i, false);
            mBuilder.setOngoing(true);
            int title = R.string.zipping;
            mBuilder.setContentTitle(utils.getString(c, title));
            mBuilder.setContentText(new File(fileName).getName() + " " + utils.readableFileSize(done) + "/" + utils.readableFileSize(total));
            int id1 = Integer.parseInt("456" + id);
            mNotifyManager.notify(id1, mBuilder.build());
            if (i == 100 || total == 0) {
                mBuilder.setContentTitle("Zip completed");
                mBuilder.setContentText("");
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult(id1);
            }
            Intent intent = new Intent(EXTRACT_CONDITION);
            intent.putExtra(EXTRACT_PROGRESS, i);
            intent.putExtra("id", id);
            intent.putExtra("name", fileName);
            intent.putExtra(EXTRACT_COMPLETED, b);
            sendBroadcast(intent);

        } else {
            publishCompletedResult(Integer.parseInt("456" + id));
        }
    }public void publishCompletedResult(int id1){
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void publishResult(boolean b) {
        Intent intent = new Intent("run");
        intent.putExtra("run", b);
        sendBroadcast(intent);

    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */

    private BroadcastReceiver receiver1 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            hash.put(intent.getIntExtra("id", 1), false);
        }
    };

    class zip {
        public zip() {
        }

        int count;
        long size, totalBytes;
        String fileName;

        public void execute(int id, ArrayList<File> a, String fileOut) {
            for (File f1 : a) {
                if (f1.isDirectory()) {
                    totalBytes = totalBytes + new Futils().folderSize(f1);
                } else {
                    totalBytes = totalBytes + f1.length();
                }
            }
            FileOutputStream out = null;
            count = a.size();
            fileName = fileOut;
            File zipDirectory = new File(fileOut);
            publishResult(true);
            try {
                out = new FileOutputStream(zipDirectory);
                zos = new ZipOutputStream(new BufferedOutputStream(out));
            } catch (Exception e) {
            }
            for (File file : a) {
                try {
                    publishResult(true);
                    compressFile(id, file, "");
                } catch (IOException e) {
                }
            }
            try {
                zos.flush();
                zos.close();

            } catch (IOException e) {
            }
        }

        ZipOutputStream zos;
        private int isCompressed = 0;

        private void compressFile(int id, File file, String path) throws IOException {

            if (!file.isDirectory()) {
                byte[] buf = new byte[20480];
                int len;
                FileInputStream in = new FileInputStream(file);
                    zos.putNextEntry(new ZipEntry(path + "/" + file.getName()));
                while ((len = in.read(buf)) > 0) {
                    if (hash.get(id)) {
                        zos.write(buf, 0, len);
                        size += len;
                        publishResult(true);
                        int p = Math.round(size * 100 / totalBytes);
                        System.out.println(id + " " + p + " " + hash.get(id));
                        publishResults(id, fileName, p, false,size,totalBytes);
                    }
                }
                in.close();
                return;
            }
            if (file.list() == null) {
                return;
            }
            for (String fileName : file.list()) {

                File f = new File(file.getAbsolutePath() + File.separator
                        + fileName);
                compressFile(id, f, path + File.separator + file.getName());

            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(receiver1);
    }
}
