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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amaze.filemanager.ProgressListener;
import com.amaze.filemanager.R;
import com.amaze.filemanager.RegisterCallback;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.GenericCopyThread;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class CopyService extends Service {
    HashMap<Integer, Boolean> hash = new HashMap<Integer, Boolean>();
    public HashMap<Integer, DataPackage> hash1 = new HashMap<Integer, DataPackage>();
    boolean rootmode;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    Context c;
    @Override
    public void onCreate() {
        c = getApplicationContext();
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
        ArrayList<BaseFile> sourceFiles;
        boolean move;
        Copy copy;
        public DoInBackground() {
        }

        protected Integer doInBackground(Bundle... p1) {
            String targetPath = p1[0].getString("FILE2");
            int id = p1[0].getInt("id");
            sourceFiles = p1[0].getParcelableArrayList("files");
            move=p1[0].getBoolean("move");
            copy=new Copy();
            copy.execute(id, sourceFiles, targetPath,move,OpenMode.getOpenMode(p1[0].getInt("MODE")));

            // TODO: Implement this method
            return id;
        }

        @Override
        public void onPostExecute(Integer b) {
            publishResults("", 0, 0, b, 0, 0, true, move);
            generateNotification(copy.failedFOps,move);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            hash.put(b,false);
            boolean stop=true;
            for(int a:hash.keySet()){
                if(hash.get(a))stop=false;
            }
            if(!stop) stopSelf(b);
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
                failedFOps=new ArrayList<>();
                toDelete=new ArrayList<>();
            }

            long getTotalBytes(final ArrayList<BaseFile> files, final ProgressHandler progressHandler) {
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
                            progressHandler.setTotalSize(totalBytes);
                        } catch (Exception e) {
                        }
                        Copy.this.totalBytes=totalBytes;
                        calculatingTotalSize=false;
                    }
                }).run();

                return totalBytes;
            }

            public int checkFolder(final String f,Context context) {
                if(f==null)return 0;
                if(f.startsWith("smb://") || f.startsWith("otg:"))return 1;
                File folder=new File(f);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(folder, context)) {
                    if (!folder.exists() || !folder.isDirectory()) {
                        return 0;
                    }

                    // On Android 5, trigger storage access framework.
                    if (FileUtil.isWritableNormalOrSaf(folder, context)) {
                        return 1;

                    }
                } else if (Build.VERSION.SDK_INT == 19 && FileUtil.isOnExtSdCard(folder, context)) {
                    // Assume that Kitkat workaround works
                    return 1;
                } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
                    return 1;
                } else {
                    return 0;
                }
                return 0;
            }

            public void execute(final int id, final ArrayList<BaseFile> sourceFiles, final String targetPath,
                                final boolean move,OpenMode mode) {
                if (checkFolder((targetPath), c) == 1) {
                    final ProgressHandler progressHandler=new ProgressHandler(-1);
                    GenericCopyThread copyThread = new GenericCopyThread(c);
                    progressHandler.setProgressListener(new ProgressHandler.ProgressListener() {
                        @Override
                        public void onProgressed(String fileName, float p1, float p2, float speed, float avg) {
                            publishResults(fileName, (int) p1, (int) p2, id, progressHandler.totalSize, progressHandler.writtenSize, false, move);
                            System.out.println(new File(fileName).getName() + " Progress " + p1 + " Secondary Progress " + p2 + " Speed " + speed + " Avg Speed " + avg);
                        }
                    });
                    getTotalBytes(sourceFiles, progressHandler);
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        BaseFile f1 = (sourceFiles.get(i));
                        Log.e("Copy","basefile\t"+f1.getPath());
                        try {


                            HFile hFile=new HFile(mode,targetPath, sourceFiles.get(i).getName(),f1.isDirectory());
                            if (hash.get(id)){
                                if(!f1.isSmb() && !new File(sourceFiles.get(i).getPath()).canRead() && rootmode){
                                    copyRoot(f1, hFile, move);
                                    continue;
                                }
                                copyFiles((f1),hFile, copyThread,progressHandler, id, move);
                            }
                            else{
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("Copy","Got exception checkout");

                            failedFOps.add(sourceFiles.get(i));
                            for(int j=i+1;j<sourceFiles.size();j++)failedFOps.add(sourceFiles.get(j));
                            break;
                        }
                    }
                    // waiting for generic copy thread to finish before returning from this point
                    try {

                        if (copyThread.thread!=null) {
                            copyThread.thread.join();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else if (rootmode) {
                    for (int i = 0; i < sourceFiles.size(); i++) {
                        HFile hFile=new HFile(mode,targetPath, sourceFiles.get(i).getName(),sourceFiles.get(i).isDirectory());
                        copyRoot(sourceFiles.get(i), hFile, move);
                        /*if(checkFiles(new HFile(sourceFiles.get(i).getMode(),path),new HFile(OpenMode.ROOT,targetPath+"/"+name))){
                            failedFOps.add(sourceFiles.get(i));
                        }*/
                    }


                } else {
                    for(BaseFile f:sourceFiles) failedFOps.add(f);
                    return;
                }

                // making sure to delete files after copy operation is done
                if (move) {
                    ArrayList<BaseFile> toDelete=new ArrayList<>();
                    for(BaseFile a:sourceFiles){
                        if(!failedFOps.contains(a))
                            toDelete.add(a);
                    }
                    new DeleteTask(getContentResolver(), c).execute((toDelete));
                }
            }
            void copyRoot(BaseFile sourceFile, HFile targetFile, boolean move){

                try {
                    RootUtils.mountOwnerRW(targetFile.getParent());
                    if (!move) RootUtils.copy(sourceFile.getPath(), targetFile.getPath());
                    else if (move) RootUtils.move(sourceFile.getPath(), targetFile.getPath());
                } catch (RootNotPermittedException e) {
                    failedFOps.add(sourceFile);
                    e.printStackTrace();
                }
                Futils.scanFile(targetFile.getPath(), c);
            }

            private void copyFiles(final BaseFile sourceFile,final HFile targetFile,
                                   GenericCopyThread copyThread,
                                   ProgressHandler progressHandler,
                                   final int id,final boolean move) throws IOException {
                Log.e("Copy",sourceFile.getPath());
                if (sourceFile.isDirectory()) {
                    if(!hash.get(id))return;

                    if (!targetFile.exists()) targetFile.mkdir(c);

                    // various checks
                    // 1. source file and target file doesn't end up in loop
                    // 2. source file has a valid name or not
                    if(!Operations.isFileNameValid(sourceFile.getName())
                            || Operations.isCopyLoopPossible(sourceFile, targetFile)){
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
                        copyFiles(file, destFile,copyThread,progressHandler, id, move);
                    }
                    if(!hash.get(id))return;
                } else {
                    if (!hash.get(id)) return;
                    if(!Operations.isFileNameValid(sourceFile.getName())){
                        failedFOps.add(sourceFile);
                        copy_successful=false;
                        return;
                    }

                    System.out.println("Copy start for "+targetFile.getName());

                    // start a new thread only after previous work is done
                    try {
                        if (copyThread.thread!=null) copyThread.thread.join();
                        copyThread.startThread(sourceFile, targetFile, progressHandler);
                    } catch (InterruptedException e) {
                        // thread interrupted due to some problem. we must return
                        failedFOps.add(sourceFile);
                        copy_successful = false;
                    }
                }
            }
        }
    }

    void generateNotification(ArrayList<HFile> failedOps,boolean move) {
        if(failedOps.size()==0)return;
        mNotifyManager.cancelAll();
        NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(c);
        mBuilder.setContentTitle(c.getString(R.string.operationunsuccesful));
        mBuilder.setContentText("Some files weren't %s successfully".replace("%s",move?"moved":"copied"));
        mBuilder.setAutoCancel(true);

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
            mBuilder.setContentTitle(c.getResources().getString(title));
            mBuilder.setContentText(new File(a).getName() + " " + Futils.readableFileSize(done) + "/" + Futils.readableFileSize(total));
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
            intent.setName(new File(a).getName());
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
    // avoid using the method as there is no way to know when we would be returning from command callbacks
    // rather confirm from the command result itself, inside it's callback
    boolean checkFiles(HFile hFile1,HFile hFile2) throws RootNotPermittedException {
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
