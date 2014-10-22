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
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

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

        b.putInt("id", startId);

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

            new copy().execute(id, files, FILE2);

            // TODO: Implement this method
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults(false);
            publishResults("", 0, 0, b, 0, 0, true);
            stopSelf(b);

        }

    }

    private void publishResults(String a, int p1, int p2, int id, long total, long done, boolean b) {
        Intent intent = new Intent("copy");
        intent.putExtra("name", a);
        intent.putExtra("total", total);
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

        public void execute(int id, final ArrayList<String> files,final String FILE2) {
            if(new File(FILE2).canWrite() && new File(files.get(0)).canRead()){for (int i = 0; i < files.size(); i++) {

                File f1 = new File(files.get(i));
                if (f1.isDirectory()) {
                    totalBytes = totalBytes + new Futils().folderSize(f1,false);
                } else {
                    totalBytes = totalBytes + f1.length();
                }
            }
            for (int i = 0; i < files.size(); i++) {
                File f1 = new File(files.get(i));
                try {

                    copyFiles((f1), new File(FILE2, f1.getName()), id);
                } catch (IOException e) {
                    System.out.println("amaze " + e);
                    publishResults("" + e, 0, 0, id, 0, 0, false);
                }

            }}else{
                RootTools.remount(FILE2,"rw");
                for (int i = 0; i < files.size(); i++) {
                Command a=new Command(0,"cp "+files.get(i) +" "+FILE2) {
                    @Override
                    public void commandOutput(int i, String s) {

                    }

                    @Override
                    public void commandTerminated(int i, String s) {

                    }

                    @Override
                    public void commandCompleted(int i, int i2) {
                        utils.scanFile(FILE2+"/"+new File(files.get(i)).getName(), c);
                    }
                };
                try {
                    RootTools.getShell(true).add(a);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (RootDeniedException e) {
                    e.printStackTrace();
                }}
            }
            utils.scanFile(new File(files.get(0)).getParent(),c);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);

        }

        private void copyFiles(File sourceFile, File targetFile, int id) throws IOException {
            if (sourceFile.isDirectory()) {
                if (!targetFile.exists()) targetFile.mkdirs();

                String[] filePaths = sourceFile.list();

                for (String filePath : filePaths) {
                    File srcFile = new File(sourceFile, filePath);
                    File destFile = new File(targetFile, filePath);

                    copyFiles(srcFile, destFile, id);
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
                        publishResults(sourceFile.getName(), Math.round(copiedBytes * 100 / totalBytes), Math.round(fileBytes * 100 / size), id, totalBytes, copiedBytes, false);
                        publishResults(true);
                    }
                    //	System.out.println(sourceFile.getName()+" "+id+" " +Math.round(copiedBytes*100/totalBytes)+"  "+Math.round(fileBytes*100/size));
                }


                in.close();
                out.close();

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
