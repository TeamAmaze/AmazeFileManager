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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MediaFile;
import com.amaze.filemanager.utils.RootHelper;
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
    boolean rootmode;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    @Override
    public void onCreate() {
        notification = new Notification(R.drawable.ic_content_copy_white_36dp, getResources().getString(R.string.copying_fles), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getResources().getString(R.string.copying_fles), "", pendingIntent);
        startForeground(001, notification);
        SharedPreferences Sp=PreferenceManager.getDefaultSharedPreferences(this);
        rootmode=Sp.getBoolean("rootmode",false);
        registerReceiver(receiver3, new IntentFilter("copycancel"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        ArrayList<String> files = intent.getStringArrayListExtra("FILE_PATHS");
        String FILE2 = intent.getStringExtra("COPY_DIRECTORY");
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        b.putInt("id", startId);
        b.putBoolean("move",intent.getBooleanExtra("move",false));
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
        boolean move;
        public Doback() {
        }

        protected Integer doInBackground(Bundle... p1) {
            String FILE2 = p1[0].getString("FILE2");
            int id = p1[0].getInt("id");
            files = p1[0].getStringArrayList("files");
            move=p1[0].getBoolean("move");
            new copy().execute(id, files, FILE2,move);

            // TODO: Implement this method
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults(false);
            publishResults("", 0, 0, b, 0, 0, true,move);
            stopSelf(b);

        }

    }

    private void publishResults(String a, int p1, int p2, int id, long total, long done, boolean b,boolean move) {
        if(hash.get(id)) {
            mBuilder.setProgress(100, p1, false);
            mBuilder.setOngoing(true);
            int title=R.string.copying;
            if(move)title=R.string.moving;
            mBuilder.setContentTitle(utils.getString(c,title));
            mBuilder.setContentText(new File(a).getName()+" "+utils.readableFileSize(done)+"/"+utils.readableFileSize(total));
            int id1=Integer.parseInt("456"+id);
            mNotifyManager.notify(id1,mBuilder.build());
            if(p1==100 || total==0){
                mBuilder.setContentTitle("Copy completed");
                if(move)
                mBuilder.setContentTitle("Move Completed");
                mBuilder.setContentText("");
                mBuilder.setProgress(0,0,false);
                mBuilder.setOngoing(false);
                mNotifyManager.notify(id1,mBuilder.build());
            publishCompletedResult(a,id1);
            }
            Intent intent = new Intent("copy");
            intent.putExtra("name", a);
            intent.putExtra("total", total);
            intent.putExtra("done", done);
            intent.putExtra("id", id);
            intent.putExtra("p1", p1);
            intent.putExtra("p2", p2);
            intent.putExtra("move", move);
            intent.putExtra("COPY_COMPLETED", b);
            sendBroadcast(intent);
        }else publishCompletedResult(a,Integer.parseInt("456"+id));}
    public void publishCompletedResult(String a,int id1){
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        public void execute(int id, final ArrayList<String> files,final String FILE2,final boolean move) {
            mBuilder = new NotificationCompat.Builder(c);
            mBuilder.setContentTitle(getResources().getString(R.string.copying))

                    .setSmallIcon(R.drawable.ic_content_copy_white_36dp);
            if (new File(FILE2).canWrite() && new File(files.get(0)).canRead()) {
                try{
                    for (int i = 0; i < files.size(); i++) {
                        File f1 = new File(files.get(i));
                        if (f1.isDirectory()) {
                            totalBytes = totalBytes + new Futils().folderSize(f1);
                        } else {
                            totalBytes = totalBytes + f1.length();
                        }
                    }}catch(Exception e){}
                for (int i = 0; i < files.size(); i++) {
                    File f1 = new File(files.get(i));
                    try {

                        copyFiles((f1), new File(FILE2, f1.getName()), id, move);
                    } catch (IOException e) {
                        System.out.println("amaze " + e);
                        publishResults("" + e, 0, 0, id, 0, 0, false, move);
                    }

                }
                if (move) {
                    boolean b = hash.get(id);
                    if (b) new DeleteTask(getContentResolver(),  c).execute(utils.toFileArray(files));
                }
                Intent intent = new Intent("loadlist");
                sendBroadcast(intent);
            } else if (rootmode) {
                RootTools.remount(FILE2, "rw");
                for (int i = 0; i < files.size(); i++) {
                    RootHelper.runAndWait("cp -R \""+getCommandLineString(files.get(i)) +"\" \""+getCommandLineString(FILE2) + "\"",true);
                    utils.scanFile(FILE2+"/"+new File(files.get(i)).getName(), c);
                }
                if(move){new DeleteTask(getContentResolver(),c).execute(utils.toFileArray(files));}

                Intent intent = new Intent("loadlist");
                sendBroadcast(intent);

            }else {
                    System.out.println("Not Allowed");
                }
            }
        private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";

        private  String getCommandLineString(String input) {
            return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
        }

        private void copyFiles(File sourceFile, File targetFile, int id,boolean move) throws IOException {
            if (sourceFile.isDirectory()) {
                if (!targetFile.exists()) targetFile.mkdirs();

                String[] filePaths = sourceFile.list();

                for (String filePath : filePaths) {
                    File srcFile = new File(sourceFile, filePath);
                    File destFile = new File(targetFile, filePath);

                    copyFiles(srcFile, destFile, id,move);
                }
            } else {
                long size = sourceFile.length(), fileBytes = 0l;
                // txtDetails.append("Copying " + sourceFile.getAbsolutePath() + " ... ");
                InputStream in = new FileInputStream(sourceFile);
                OutputStream out;
                try {
                out= new FileOutputStream(targetFile);
}catch (Exception e){out=new MediaFile(c,targetFile).write(size);}

                byte[] buffer = new byte[20480];

                int length;
                //copy the file content in bytes
                while ((length = in.read(buffer)) > 0) {
                    boolean b = hash.get(id);
                    if (b) {
                        out.write(buffer, 0, length);
                        copiedBytes += length;
                        fileBytes += length;
                        publishResults(sourceFile.getName(), Math.round(copiedBytes * 100 / totalBytes), Math.round(fileBytes * 100 / size), id, totalBytes, copiedBytes, false,move);
                        publishResults(true);
                    }
                    //	System.out.println(sourceFile.getName()+" "+id+" " +Math.round(copiedBytes*100/totalBytes)+"  "+Math.round(fileBytes*100/size));
                }


                in.close();
                out.close();

                utils.scanFile(targetFile.getPath(), c);
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
