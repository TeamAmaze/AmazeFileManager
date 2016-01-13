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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.utils.Futils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    public HashMap<Integer, DataPackage> hash1 = new HashMap<Integer, DataPackage>();
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    String zpath;
    Context c;
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        c=getApplicationContext();
        registerReceiver(receiver1, new IntentFilter("zipcancel"));
    }
boolean foreground=true;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        zpath= PreferenceManager.getDefaultSharedPreferences(this).getString("zippath","");
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String name = intent.getStringExtra("name");
        if((zpath!=null && zpath.length()!=0)){
        if(zpath.endsWith("/"))name=zpath+new File(name).getName();
            else name=zpath+"/"+new File(name).getName();
        }
        File c = new File(name);
        if (!c.exists()) {
            try {
                c.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        DataPackage intent1 = new DataPackage();
        intent1.setName(name);
        intent1.setTotal(0);
        intent1.setDone(0);
        intent1.setId(startId);
        intent1.setP1(0);
        intent1.setCompleted(false);
        hash1.put(startId, intent1);
        mBuilder = new NotificationCompat.Builder(this);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("openprocesses",true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(getResources().getString(R.string.zipping))

                .setSmallIcon(R.drawable.ic_doc_compressed);
        if(foreground){
            startForeground(Integer.parseInt("789"+startId),mBuilder.build());
        }
        ArrayList<String> a = intent.getStringArrayListExtra("files");
        b.putInt("id", startId);
        b.putStringArrayList("files", a);
        b.putString("name", name);
        hash.put(startId, true);
        new Doback().execute(b);
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public ZipTask getService() {
            // Return this instance of LocalService so clients can call public methods
            return ZipTask.this;
        }
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    ProgressListener progressListener;

    public interface ProgressListener {
        void onUpdate(DataPackage dataPackage);
        void refresh();
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
            publishResults(b, name, 100, true,0,totalBytes);
            hash.put(b,false);
            boolean stop=true;
            for(int a:hash.keySet()){
                if(hash.get(a))stop=false;
            }
            if(stop)
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
            int id1 = Integer.parseInt("789" + id);
            mNotifyManager.notify(id1, mBuilder.build());
            if (i == 100 || total == 0) {
                mBuilder.setContentTitle("Zip completed");
                mBuilder.setContentText("");
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult(id1);
                b=true;
            }
            DataPackage intent = new DataPackage();
            intent.setName(fileName);
            intent.setTotal(total);
            intent.setDone(done);
            intent.setId(id);
            intent.setP1(i);
            intent.setCompleted(b);
            hash1.put(id, intent);
            if(progressListener!=null){
                progressListener.onUpdate(intent);
                if(b)progressListener.refresh();
            }
        } else {
            publishCompletedResult(Integer.parseInt("789" + id));
        }
    }public void publishCompletedResult(int id1){
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
            hash.put(intent.getIntExtra("id", 1), false);
        }
    };
    class zip {
        public zip() {
        }

        int count,lastpercent=0;
        long size, totalBytes=0;
        String fileName;

        public void execute(int id, ArrayList<File> a, String fileOut) {
            for (File f1 : a) {
                if (f1.isDirectory()) {
                    totalBytes = totalBytes + new Futils().folderSize(f1);
                } else {
                    totalBytes = totalBytes + f1.length();
                }
            }
            OutputStream out = null;
            count = a.size();
            fileName = fileOut;
            File zipDirectory = new File(fileOut);
             
            try {
                out = FileUtil.getOutputStream(zipDirectory,c,totalBytes);
                zos = new ZipOutputStream(new BufferedOutputStream(out));
            } catch (Exception e) {
            }
            for (File file : a) {
                try {
                     
                    compressFile(id, file, "");
                } catch (Exception e) {
                }
            }
            try {
                zos.flush();
                zos.close();

            } catch (Exception e) {
            }
        }

        ZipOutputStream zos;
        private int isCompressed = 0;
        AsyncTask asyncTask;
        void calculateProgress(final String name,final int id, final boolean completed,final long
                copiedbytes,final long totalbytes)
        {
            if(asyncTask!=null && asyncTask.getStatus()== AsyncTask.Status.RUNNING)asyncTask.cancel(true);
            asyncTask=new AsyncTask<Void,Void,Void>(){
                int p1,p2;
                @Override
                protected Void doInBackground(Void... voids) {
                    if(isCancelled())return null;
                    p1 = (int) ((copiedbytes / (float) totalbytes) * 100);
                    lastpercent = (int)copiedbytes;
                    if(isCancelled())return null;
                    return null;
                }@Override
                 public void onPostExecute(Void v){
                    publishResults(id, name, p1, completed, copiedbytes,totalbytes);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        }

        private void compressFile(int id, File file, String path) throws IOException,NullPointerException {

            if (!file.isDirectory()) {
                byte[] buf = new byte[20480];
                int len;
                BufferedInputStream in=new BufferedInputStream( new FileInputStream(file));
                    zos.putNextEntry(new ZipEntry(path + "/" + file.getName()));
                while ((len = in.read(buf)) > 0) {
                    if (hash.get(id)) {
                        zos.write(buf, 0, len);
                        size += len;
                        int p=(int) ((size / (float) totalBytes) * 100);
                        if(p!=lastpercent || lastpercent==0) {
                            calculateProgress(fileName,id,false,size,totalBytes);
                        }lastpercent=p;
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
        return mBinder;
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(receiver1);
    }
}
