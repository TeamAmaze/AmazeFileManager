package com.amaze.filemanager.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

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

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        Notification notification = new Notification(R.drawable.ic_action_search_light, "Zipping Files", System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Zipping Files", "", pendingIntent);
        startForeground(004, notification);
        registerReceiver(receiver1, new IntentFilter("zipcancel"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
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

    public class Doback extends AsyncTask<Bundle, Void, Integer> {
        ArrayList<String> files;

        public Doback() {
        }

        long totalBytes = 0L;
        String name;

        protected Integer doInBackground(Bundle... p1) {

            int id = p1[0].getInt("id");
            ArrayList<String> a = p1[0].getStringArrayList("files");
            name = p1[0].getString("name");
            new zip().execute(id, utils.toFileArray(a), name);
            // TODO: Implement this method
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults(b, name, 100, true);
            publishResult(false);
            stopSelf(b);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
        }

    }

    private void publishResults(int id, String fileName, int i, boolean b) {
        Intent intent = new Intent(EXTRACT_CONDITION);
        intent.putExtra(EXTRACT_PROGRESS, i);
        intent.putExtra("id", id);
        intent.putExtra("name", fileName);
        intent.putExtra(EXTRACT_COMPLETED, b);
        sendBroadcast(intent);

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
                    totalBytes = totalBytes + new Futils().folderSize(f1,false);
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
                byte[] buf = new byte[1024];
                int len;
                FileInputStream in = new FileInputStream(file);
                if (path.length() > 0)
                    zos.putNextEntry(new ZipEntry(path + "/" + file.getName()));
                else
                    zos.putNextEntry(new ZipEntry(file.getName()));
                while ((len = in.read(buf)) > 0) {
                    if (hash.get(id)) {
                        zos.write(buf, 0, len);
                        size += len;
                        publishResult(true);
                        int p = Math.round(size * 100 / totalBytes);
                        System.out.println(id + " " + p + " " + hash.get(id));
                        publishResults(id, fileName, p, false);
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
