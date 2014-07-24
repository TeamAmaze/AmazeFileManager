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
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.Futils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExtractService extends Service {
    public final String EXTRACT_CONDITION = "EXTRACT_CONDITION";
    public final String EXTRACT_PROGRESS = "EXTRACT_PROGRESS";
    public final String EXTRACT_COMPLETED = "EXTRACT_COMPLETED";

    Futils utils = new Futils();
    // Binder given to clients
    HashMap<Integer, Boolean> hash = new HashMap<Integer, Boolean>();

    @Override
    public void onCreate() {
        Notification notification = new Notification(R.drawable.ic_action_search_light, "Extracting Files", System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Extracting Files", "", pendingIntent);
        startForeground(002, notification);
        registerReceiver(receiver1, new IntentFilter("excancel"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        b.putInt("id", startId);
        String file = intent.getStringExtra("zip");
        b.putString("file", file);
        new Doback().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, b);
        hash.put(startId, true);
        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
    }

    private void createDir(File dir) {
        if (dir.exists()) {
            return;
        }
        Log.i("Amaze", "Creating dir " + dir.getName());
        if (!dir.mkdirs()) {
            throw new RuntimeException("Can not create dir " + dir);
        }
    }

    private void unzipEntry(int id, ZipFile zipfile, ZipEntry entry, String outputDir)
            throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }
        //	Log.i("Amaze", "Extracting: " + entry);
        BufferedInputStream inputStream = new BufferedInputStream(
                zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(outputFile));
        try {
            int len;
            byte buf[] = new byte[1024];
            while ((len = inputStream.read(buf)) > 0) {
                System.out.println(id + " " + hash.get(id));
                if (hash.get(id)) {
                    publishResults(true);
                    outputStream.write(buf, 0, len);

                } else {
                    stopSelf(id);
                    publishResults(false);
                }
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    public boolean extract(int id, File archive, String destinationPath) {
        int i = 0;
        try {
            ZipFile zipfile = new ZipFile(archive);
            int fileCount = zipfile.size();
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                Log.i("Amaze", id + " " + hash.get(id));
                if (hash.get(id)) {
                    publishResults(true);
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    unzipEntry(id, zipfile, entry, destinationPath);
                    i++;
                    publishResults(id, archive.getName(), i * 100 / fileCount, false);
                } else {
                    stopSelf(id);
                    publishResults(false);
                }
            }
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            return true;
        } catch (Exception e) {
            Log.e("amaze", "Error while extracting file " + archive, e);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            return false;
        }

    }

    public class Doback extends AsyncTask<Bundle, Void, Integer> {
        protected Integer doInBackground(Bundle... p1) {
            String file = p1[0].getString("file");
            File f = new File(file);
            extract(p1[0].getInt("id"), f, f.getParent() + "/" + f.getName().substring(0, f.getName().lastIndexOf(".")));


            Log.i("Amaze", "Almost Completed");
            // TODO: Implement this method
            return p1[0].getInt("id");
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults(false);
            publishResults(b, "", 0, true);
            Log.i("Amaze", "Completed");
            stopSelf(b);
        }

    }

    private void publishResults(int id, String name, int i, boolean b) {
        Intent intent = new Intent(EXTRACT_CONDITION);
        intent.putExtra("p1", i);
        intent.putExtra("id", id);
        intent.putExtra("name", name);
        intent.putExtra("extract_completed", b);
        sendBroadcast(intent);

    }

    private void publishResults(boolean b) {
        Intent intent = new Intent("run");
        intent.putExtra("run", b);
        sendBroadcast(intent);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver1);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    Context c = this;
    private BroadcastReceiver receiver1 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Amaze", "" + intent.getIntExtra("id", 1));
            hash.put(intent.getIntExtra("id", 1), false);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}

