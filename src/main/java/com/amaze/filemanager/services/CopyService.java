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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amaze.filemanager.ProgressListener;
import com.amaze.filemanager.R;
import com.amaze.filemanager.RegisterCallback;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.services.FileVerifier.FileVerifierInterface;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.stericson.RootTools.RootTools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jcifs.smb.SmbException;

public class CopyService extends Service {
    HashMap<Integer, Boolean> hash = new HashMap<Integer, Boolean>();
    public HashMap<Integer, DataPackage> hash1 = new HashMap<Integer, DataPackage>();
    boolean rootmode;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    Context c;
    Futils utils ;
    @Override
    public void onCreate() {
        c = getApplicationContext();
        utils=new Futils();
        SharedPreferences Sp=PreferenceManager.getDefaultSharedPreferences(this);
        rootmode=Sp.getBoolean("rootmode",false);
        registerReceiver(receiver3, new IntentFilter("copycancel"));
    }


    boolean foreground=true;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        ArrayList<BaseFile> files = intent.getParcelableArrayListExtra("FILE_PATHS");
        String FILE2 = intent.getStringExtra("COPY_DIRECTORY");
        int mode=intent.getIntExtra("MODE",0);
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        b.putInt("id", startId);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra("openprocesses",true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder = new NotificationCompat.Builder(c);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(getResources().getString(R.string.copying))

                .setSmallIcon(R.drawable.ic_content_copy_white_36dp);
        if(foreground){
            startForeground(Integer.parseInt("456"+startId),mBuilder.build());
            foreground=false;
        }
        b.putBoolean("move", intent.getBooleanExtra("move", false));
        b.putString("FILE2", FILE2);
        b.putInt("MODE",mode);
        b.putParcelableArrayList("files", files);
        hash.put(startId, true);
        DataPackage intent1 = new DataPackage();
        intent1.setName(files.get(0).getName());
        intent1.setTotal(0);
        intent1.setDone(0);
        intent1.setId(startId);
        intent1.setP1(0);
        intent1.setP2(0);
        intent1.setMove(intent.getBooleanExtra("move", false));
        intent1.setCompleted(false);
        hash1.put(startId,intent1);
        //going async
        new DoInBackground().execute(b);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }
    ProgressListener progressListener;


    public void onDestroy() {
        this.unregisterReceiver(receiver3);
    }

    public class DoInBackground extends AsyncTask<Bundle, Void, Integer> {
        ArrayList<BaseFile> files;
        boolean move;
        FileVerifier fileVerifier;
        Copy copy;
        public DoInBackground() {
        }

        protected Integer doInBackground(Bundle... p1) {
            String FILE2 = p1[0].getString("FILE2");
            int id = p1[0].getInt("id");
            files = p1[0].getParcelableArrayList("files");
            move=p1[0].getBoolean("move");
            copy=new Copy();
            copy.execute(id, files, FILE2,move,p1[0].getInt("MODE"));

            // TODO: Implement this method
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults("", 0, 0, b, 0, 0, true, move);
            if(fileVerifier!=null && fileVerifier.isRunning()){
                while (fileVerifier.isRunning()){
                    try {
                        Thread.sleep(2000);
                     } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            generateNotification(copy.failedFOps,move);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            hash.put(b,false);
            boolean stop=true;
            for(int a:hash.keySet()){
            if(hash.get(a))stop=false;
            }
            if(!stop)
            stopSelf(b);
            else stopSelf();

        }

    class Copy {

        long totalBytes = 0L, copiedBytes = 0L;
        boolean calculatingTotalSize=false;
        ArrayList<HFile> failedFOps;
        ArrayList<BaseFile> toDelete;
        boolean copy_successful;
        public Copy() {
            copy_successful=true;
            fileVerifier = new FileVerifier(c,rootmode, new FileVerifierInterface() {
                @Override
                public void addFailedFile(HFile a) {
                    failedFOps.add(a);
                }

                @Override
                public boolean contains(String path) {
                    for (HFile a : failedFOps)
                        if (a.getPath().equals(path)) return true;
                    return false;
                }

                @Override
                public boolean containsDirectory(String path) {
                    for (HFile a : failedFOps)
                        if (a.getPath().contains(path))
                            return true;
                    return false;
                }

                @Override
                public void setCopySuccessful(boolean b) {
                    copy_successful = b;
                }
            });
            failedFOps=new ArrayList<>();
            toDelete=new ArrayList<>();
        }

        long getTotalBytes(final ArrayList<BaseFile> files) {
            calculatingTotalSize=true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long totalBytes = 0l;
                    try {
                        for (int i = 0; i < files.size(); i++) {
                            HFile f1 = (files.get(i));
                            if (f1.isDirectory()) {
                                totalBytes = totalBytes + f1.folderSize();
                            } else {
                                totalBytes = totalBytes + f1.length();
                            }
                        }
                    } catch (Exception e) {
                    }
                    Copy.this.totalBytes=totalBytes;
                calculatingTotalSize=false;
                }
            }).run();

            return totalBytes;
        }
        public void execute(int id, final ArrayList<BaseFile> files, final String FILE2, final boolean move,int mode) {
            if (utils.checkFolder((FILE2), c) == 1) {
                getTotalBytes(files);
                for (int i = 0; i < files.size(); i++) {
                    BaseFile f1 = (files.get(i));
                    Log.e("Copy","basefile\t"+f1.getPath());
                    try {

                        if (hash.get(id)){
                            if(!f1.isSmb() && !new File(files.get(i).getPath()).canRead() && rootmode){
                                copyRoot(files.get(i).getPath(),files.get(i).getName(),FILE2,move);
                                continue;
                            }
                            HFile hFile=new HFile(mode,FILE2, files.get(i).getName(),f1.isDirectory());
                            copyFiles((f1),hFile , id, move);
                        }
                        else{
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("Copy","Got exception checkout");

                        failedFOps.add(files.get(i));
                        for(int j=i+1;j<files.size();j++)failedFOps.add(files.get(j));
                        break;
                    }
                }

            } else if (rootmode) {
                boolean m = true;
                for (int i = 0; i < files.size(); i++) {
                    String path=files.get(i).getPath();
                    String name=files.get(i).getName();
                    copyRoot(path,name,FILE2,move);
                    if(checkFiles(new HFile(files.get(i).getMode(),path),new HFile(HFile.ROOT_MODE,FILE2+"/"+name))){
                        failedFOps.add(files.get(i));
                    }
                }
                if (move && m) {
                    ArrayList<BaseFile> toDelete=new ArrayList<>();
                    for(BaseFile a:files){
                        if(!failedFOps.contains(a))
                            toDelete.add(a);
                    }
                    new DeleteTask(getContentResolver(), c).execute((toDelete));
                }


            } else {
                for(BaseFile f:files)
                    failedFOps.add(f);
            }
        }
        boolean copyRoot(String path,String name,String FILE2,boolean move){
            boolean b = RootTools.copyFile(RootHelper.getCommandLineString(path), RootHelper.getCommandLineString(FILE2)+"/"+name, true, true);
            if (!b && path.contains("/0/"))
                b = RootTools.copyFile(RootHelper.getCommandLineString(path.replace("/0/", "/legacy/")), RootHelper.getCommandLineString(FILE2)+"/"+name, true, true);
            utils.scanFile(FILE2 + "/" + name, c);
            return b;
        }

        private void copyFiles(final BaseFile sourceFile,final HFile targetFile,final int id,final boolean move) throws IOException {
            Log.e("Copy",sourceFile.getPath());
            if (sourceFile.isDirectory()) {
                if(!hash.get(id))return;
                if (!targetFile.exists()) targetFile.mkdir(c);
                if(!targetFile.exists()){
                    Log.e("Copy","cant make dir");
                    failedFOps.add(sourceFile);
                    copy_successful=false;
                    return;
                }
                    targetFile.setLastModified(sourceFile.lastModified());
                if(!hash.get(id))return;
                ArrayList<BaseFile> filePaths = sourceFile.listFiles(false);
                for (BaseFile file : filePaths) {
                    HFile destFile = new HFile(targetFile.getMode(),targetFile.getPath(), file.getName(),file.isDirectory());
                    copyFiles(file, destFile, id, move);
                }
                if(!hash.get(id))return;
                fileVerifier.add(new FileBundle(sourceFile,targetFile,move));
            } else {
                if (!hash.get(id)) return;
                long size = sourceFile.length();
                InputStream in = sourceFile.getInputStream();
                OutputStream out = targetFile.getOutputStream(c);
                if (in == null || out == null) {
                    Log.e("Copy","streams null");
                    failedFOps.add(sourceFile);
                    copy_successful = false;
                    return;
                }
                if (!hash.get(id)) return;
                copy(in, out, size, id, sourceFile.getName(), move);
                fileVerifier.add(new FileBundle(sourceFile,targetFile,move));
            }
        }
        long time=System.nanoTime()/500000000;
        AsyncTask asyncTask;

        void calculateProgress(final String name, final long fileBytes, final int id, final long
                size, final boolean move) {
            if (asyncTask != null && asyncTask.getStatus() == AsyncTask.Status.RUNNING)
                asyncTask.cancel(true);
                asyncTask = new AsyncTask<Void, Void, Void>() {
                int p1, p2;

                @Override
                protected Void doInBackground(Void... voids) {
                    p1 = (int) ((copiedBytes / (float) totalBytes) * 100);
                    p2 = (int) ((fileBytes / (float) size) * 100);
                    if(calculatingTotalSize)p1=0;
                    return null;
                }

                @Override
                public void onPostExecute(Void v) {
                    publishResults(name, p1, p2, id, totalBytes, copiedBytes, false, move);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        void copy(InputStream stream,OutputStream outputStream,long size,int id,String name,boolean move) throws IOException {
            long  fileBytes = 0l;
            BufferedInputStream in = new BufferedInputStream(stream);
            BufferedOutputStream out=new BufferedOutputStream(outputStream);
            byte[] buffer = new byte[1024*60];
            int length;
            //copy the file content in bytes
            while ((length = in.read(buffer)) > 0) {
                boolean b = hash.get(id);
                if (b) {
                    out.write(buffer, 0, length);
                    copiedBytes += length;
                    fileBytes += length;
                    long time1=System.nanoTime()/500000000;
                    if(((int)time1)>((int)(time))){
                        calculateProgress(name,fileBytes,id,size,move);
                        time=System.nanoTime()/500000000;
                    }

                } else {
                    break;
                }}
            in.close();
            out.close();
            stream.close();
            outputStream.close();
        }

    }    }


    void generateNotification(ArrayList<HFile> failedOps,boolean move) {
        if(failedOps.size()==0)return;
                mNotifyManager.cancelAll();
        NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(c);
        mBuilder.setContentTitle("Operation Unsuccessful");
        mBuilder.setContentText("Some files weren't %s successfully".replace("%s",move?"moved":"copied"));
        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra("failedOps",failedOps);
        intent.putExtra("move",move);
        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pIntent);
        mBuilder.setSmallIcon(R.drawable.ic_content_copy_white_36dp);
        mNotifyManager.notify(741,mBuilder.build());
        intent=new Intent("general_communications");
        intent.putExtra("failedOps",failedOps);
        intent.putExtra("move",move);
        sendBroadcast(intent);
    }

    private void publishResults(String a, int p1, int p2, int id, long total, long done, boolean b, boolean move) {
        if (hash.get(id)) {
            //notification
            mBuilder.setProgress(100, p1, false);
            mBuilder.setOngoing(true);
            int title = R.string.copying;
            if (move) title = R.string.moving;
            mBuilder.setContentTitle(utils.getString(c, title));
            mBuilder.setContentText(new File(a).getName() + " " + utils.readableFileSize(done) + "/" + utils.readableFileSize(total));
            int id1 = Integer.parseInt("456" + id);
            mNotifyManager.notify(id1, mBuilder.build());
            if (p1 == 100 || total == 0) {
                mBuilder.setContentTitle("Copy completed");
                if (move)
                    mBuilder.setContentTitle("Move Completed");
                mBuilder.setContentText("");
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setAutoCancel(true);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult(id, id1);
            }
            //for processviewer
            DataPackage intent = new DataPackage();
            intent.setName(a);
            intent.setTotal(total);
            intent.setDone(done);
            intent.setId(id);
            intent.setP1(p1);
            intent.setP2(p2);
            intent.setMove(move);
            intent.setCompleted(b);
            hash1.put(id,intent);
            try {
                if(progressListener!=null){
                    progressListener.onUpdate(intent);
                    if(b)progressListener.refresh();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else publishCompletedResult(id, Integer.parseInt("456" + id));
    }
    public void publishCompletedResult(int id,int id1){
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //check if copy is successful
    boolean checkFiles(HFile hFile1,HFile hFile2){
        if(RootHelper.isDirectory(hFile1.getPath(),rootmode,5))
        {
            if(RootHelper.fileExists(hFile2.getPath()))return false;
            ArrayList<BaseFile> baseFiles=RootHelper.getFilesList(hFile1.getPath(),true,true,null);
            if(baseFiles.size()>0){
                boolean b=true;
                for(BaseFile baseFile:baseFiles){
                  if(!checkFiles(new HFile(baseFile.getMode(),baseFile.getPath()),new HFile(hFile2.getMode(),hFile2.getPath()+"/"+(baseFile.getName()))))
                      b=false;
                }
                return b;
            }
            return RootHelper.fileExists(hFile2.getPath());
        }
        else{
          ArrayList<BaseFile>  baseFiles=RootHelper.getFilesList(hFile1.getParent(),true,true,null);
            int i=-1;
            int index=-1;
            for(BaseFile b:baseFiles){
                i++;
                if(b.getPath().equals(hFile1.getPath()))
                {   index=i;
                    break;
                }
            }
            ArrayList<BaseFile>  baseFiles1=RootHelper.getFilesList(hFile1.getParent(),true,true,null);
            int i1=-1;
            int index1=-1;
            for(BaseFile b:baseFiles1){
                i1++;
                if(b.getPath().equals(hFile1.getPath()))
                {   index1=i1;
                    break;
                }
            }
            if(baseFiles.get(index).getSize()==baseFiles1.get(index1).getSize())
                return true;
            else return false;
        }
    }
    private BroadcastReceiver receiver3 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //cancel operation
            hash.put(intent.getIntExtra("id", 1), false);
        }
    };
    //bind with processviewer
    RegisterCallback registerCallback= new RegisterCallback.Stub() {
        @Override
        public void registerCallBack(ProgressListener p) throws RemoteException {
            progressListener=p;
        }

        @Override
        public List<DataPackage> getCurrent() throws RemoteException {
            List<DataPackage> dataPackages=new ArrayList<>();
            for (int i : hash1.keySet()) {
               dataPackages.add(hash1.get(i));
            }
            return dataPackages;
        }
    };
    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return registerCallback.asBinder();
    }
}
