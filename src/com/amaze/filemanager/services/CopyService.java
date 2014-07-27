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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class CopyService extends Service {
    HashMap<Integer, Boolean> hash = new HashMap<Integer, Boolean>();
    Notification notification;

    @Override
    public void onCreate() {
        notification = new Notification(R.drawable.ic_action_copy, "Copying Files", System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Copying Files", "", pendingIntent);
        startForeground(001, notification);

        registerReceiver(receiver3, new IntentFilter("copycancel"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        ArrayList<String> files = intent.getStringArrayListExtra("FILE_PATHS");
        String FILE2 = intent.getStringExtra("COPY_DIRECTORY");
        boolean move = intent.getBooleanExtra("move", false);
        if (move) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notification.setLatestEventInfo(this, "Moving Files", "", pendingIntent);
        }
        b.putInt("id", startId);
        b.putBoolean("move", move);
        b.putString("FILE2", FILE2);
        b.putStringArrayList("files", files);
        new Doback().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, b);
        hash.put(startId, true);
        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
    }
    // Binder given to clients

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */


    public void onDestroy() {
        this.unregisterReceiver(receiver3);
    }

    public class Doback extends AsyncTask<Bundle, Void, Integer> {
        ArrayList<String> files;

        public Doback() {
        }

        protected Integer doInBackground(Bundle... p1) {
            String FILE2 = p1[0].getString("FILE2");
            int id = p1[0].getInt("id");
            files = p1[0].getStringArrayList("files");
            boolean move = p1[0].getBoolean("move", false);
            new copy().execute(id, files, FILE2, move);

            // TODO: Implement this method
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults(false);
            publishResults("", 0, 0, b, 0, 0, true, false);
            stopSelf(b);

        }

    }

    private void publishResults(String a, int p1, int p2, int id, long total, long done, boolean b, boolean move) {
        Intent intent = new Intent("copy");
        intent.putExtra("name", a);
        intent.putExtra("total", total);
        intent.putExtra("move", move);
        intent.putExtra("done", done);
        intent.putExtra("id", id);
        intent.putExtra("p1", p1);
        intent.putExtra("p2", p2);
        intent.putExtra("COPY_COMPLETED", b);
        sendBroadcast(intent);

    }

    Context c = this;

    private void publishResults(boolean b) {
        Intent intent = new Intent("run");
        intent.putExtra("run", b);
        sendBroadcast(intent);

    }

    class copy {
        public copy() {
        }

        long totalBytes = 0L, copiedBytes = 0L;

        public void execute(int id, ArrayList<String> files, String FILE2, boolean move) {
            for (int i = 0; i < files.size(); i++) {

                File f1 = new File(files.get(i));
                if (f1.isDirectory()) {
                    totalBytes = totalBytes + new Futils().folderSize(f1);
                } else {
                    totalBytes = totalBytes + f1.length();
                }
            }
            for (int i = 0; i < files.size(); i++) {
                File f1 = new File(files.get(i));
                try {

                    copyFiles((f1), new File(FILE2, f1.getName()), id, move);
                } catch (IOException e) {
                    System.out.println("amaze " + e);
                    publishResults("" + e, 0, 0, id, 0, 0, false, move);
                }
                Intent intent = new Intent("loadlist");
                sendBroadcast(intent);
            }


        }

        private void copyFiles(File sourceFile, File targetFile, int id, boolean move) throws IOException {
            if (sourceFile.isDirectory()) {
                if (!targetFile.exists()) targetFile.mkdirs();

                String[] filePaths = sourceFile.list();

                for (String filePath : filePaths) {
                    File srcFile = new File(sourceFile, filePath);
                    File destFile = new File(targetFile, filePath);

                    copyFiles(srcFile, destFile, id, move);
                }
            } else {
                long size = sourceFile.length(), fileBytes = 0l;
                // txtDetails.append("Copying " + sourceFile.getAbsolutePath() + " ... ");
                InputStream in = new FileInputStream(sourceFile);
                OutputStream out = new FileOutputStream(targetFile);

                byte[] buffer = new byte[8192];

                int length;
                //copy the file content in bytes
                while ((length = in.read(buffer)) > 0) {
                    boolean b = hash.get(id);
                    if (b) {
                        out.write(buffer, 0, length);
                        copiedBytes += length;
                        fileBytes += length;
                        publishResults(sourceFile.getName(), Math.round(copiedBytes * 100 / totalBytes), Math.round(fileBytes * 100 / size), id, totalBytes, copiedBytes, false, move);
                        publishResults(true);
                    }
                    //	System.out.println(sourceFile.getName()+" "+id+" " +Math.round(copiedBytes*100/totalBytes)+"  "+Math.round(fileBytes*100/size));
                }


                in.close();
                out.close();
                if (move) {
                    if (hash.get(id)) {
                        sourceFile.delete();
                    }
                }
                utils.scanFile(sourceFile.getPath(), c);
            }
        }
    }

    Futils utils = new Futils();
    private BroadcastReceiver receiver3 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            hash.put(intent.getIntExtra("id", 1), false);

        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
