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
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.utils.Futils;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
public class ExtractService extends Service {
    public final String EXTRACT_CONDITION = "EXTRACT_CONDITION";
    Futils utils = new Futils();

    Context cd;
    // Binder given to clients
    HashMap<Integer, Boolean> hash = new HashMap<Integer, Boolean>();
    public HashMap<Integer, DataPackage> hash1 = new HashMap<Integer, DataPackage>();
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    ArrayList<String> entries=new ArrayList<String>();
    boolean eentries;
    String epath;
    @Override
    public void onCreate() {
        registerReceiver(receiver1, new IntentFilter("excancel"));
        cd=getApplicationContext();
    }
    boolean foreground=true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle b = new Bundle();
        b.putInt("id", startId);
        epath= PreferenceManager.getDefaultSharedPreferences(this).getString("extractpath", "");
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String file = intent.getStringExtra("zip");
        eentries=intent.getBooleanExtra("entries1",false);
        if(eentries){
            entries=intent.getStringArrayListExtra("entries");
        }
        b.putString("file", file);
        DataPackage intent1 = new DataPackage();
        intent1.setName(file);
        intent1.setTotal(0);
        intent1.setDone(0);
        intent1.setId(startId);
        intent1.setP1(0);
        intent1.setCompleted(false);
        hash1.put(startId, intent1);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.putExtra("openprocesses", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder = new NotificationCompat.Builder(cd);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(getResources().getString(R.string.extracting))
                .setContentText(new File(file).getName())
                .setSmallIcon(R.drawable.ic_doc_compressed);
        hash.put(startId, true);
        new Doback().execute(b);
        return START_STICKY;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ExtractService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ExtractService.this;
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

    private void publishResults(String a, int p1,  int id, long total, long done, boolean b) {
        if(hash.get(id)){
            mBuilder.setContentTitle(getResources().getString(R.string.extracting));
            mBuilder.setProgress(100, p1, false);
            mBuilder.setOngoing(true);
            mBuilder.setContentText(new File(a).getName() + " " + utils.readableFileSize(done) + "/" + utils.readableFileSize(total));
            int id1=Integer.parseInt("123"+id);
            mNotifyManager.notify(id1, mBuilder.build());
            if(p1==100){
                mBuilder.setContentTitle("Extract completed");
                mBuilder.setContentText(new File(a).getName() + " " + utils.readableFileSize(total));
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult("", id1);
            }
                DataPackage intent = new DataPackage();
                intent.setName(a);
                intent.setTotal(total);
                intent.setDone(done);
                intent.setId(id);
                intent.setP1(p1);
                intent.setCompleted(b);
                hash1.put(id,intent);
            if(progressListener!=null){
                progressListener.onUpdate(intent);
                if(b)progressListener.refresh();
            }
        }else publishCompletedResult(a,Integer.parseInt("123"+id));}
    public void publishCompletedResult(String a,int id1){
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public class Doback extends AsyncTask<Bundle, Void, Integer> {
    long copiedbytes=0,totalbytes=0;
    int lastpercent=0;
        private void createDir(File dir) {
            FileUtil.mkdir(dir,cd);
    }
        AsyncTask asyncTask;
        void calculateProgress(final String name,final int id, final boolean completed){
            calculateProgress(name,id,completed,copiedbytes,totalbytes);
        }
        void calculateProgress(final String name,final int id, final boolean completed,final long
                copiedbytes,final long totalbytes)
        {
            if(asyncTask!=null && asyncTask.getStatus()==Status.RUNNING)asyncTask.cancel(true);
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
                    publishResults(name, p1, id, totalbytes, copiedbytes,completed);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        }
        long time=System.nanoTime()/500000000;
        void stop(int b){
            hash.put(b,false);
            boolean stop=true;
            for(int a:hash.keySet()){
                if(hash.get(a))stop=false;
            }
            if(stop)
                stopSelf(b);
        }
    private void unzipEntry(int id, ZipFile zipfile, ZipEntry entry, String outputDir)
            throws Exception {
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
                FileUtil.getOutputStream(outputFile,cd,0));
        try {
            int len;
            byte buf[] = new byte[20480];
            while ((len = inputStream.read(buf)) > 0) {
                //System.out.println(id + " " + hash.get(id));
                if (hash.get(id)) {
                    outputStream.write(buf, 0, len);
                    copiedbytes=copiedbytes+len;

                    long time1=System.nanoTime()/500000000;
                    if(((int)time1)>((int)(time))){
                        calculateProgress(zipfile.getName(),id,false);
                        time=System.nanoTime()/500000000;
                    }
                } else {

                    calculateProgress(zipfile.getName(), id,true, copiedbytes, totalbytes);
                    cancel(true);
                }
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }
    private void unzipRAREntry(int id,String a, Archive zipfile, FileHeader entry, String outputDir)
            throws Exception {
        String name=entry.getFileNameString();
        name=name.replaceAll("\\\\","/");
        if (entry.isDirectory()) {
            createDir(new File(outputDir, name));
            return;
        }
        File outputFile = new File(outputDir, name);
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }
        //	Log.i("Amaze", "Extracting: " + entry);
        BufferedInputStream inputStream = new BufferedInputStream(
                zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, cd,entry.getFullUnpackSize()));
        try {
            int len;
            byte buf[] = new byte[20480];
            while ((len = inputStream.read(buf)) > 0) {
                //System.out.println(id + " " + hash.get(id));
                if (hash.get(id)) {
                    outputStream.write(buf, 0, len);
                    copiedbytes=copiedbytes+len;
                    long time1=System.nanoTime()/500000000;
                    if(((int)time1)>((int)(time))){
                        calculateProgress(a,id,false);
                        time=System.nanoTime()/500000000;
                    }
                } else {
                    calculateProgress(a,id,true);
                    cancel(true);
                    stop(id);
                }
            }
        }finally {
            outputStream.close();
            inputStream.close();
        }
    }
    private void unzipTAREntry(int id, TarArchiveInputStream zipfile, TarArchiveEntry entry, String outputDir,String string)
            throws Exception {
        String name=entry.getName();
        if (entry.isDirectory()) {
            createDir(new File(outputDir, name));
            return;
        }
        File outputFile = new File(outputDir, name);
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }
        //	Log.i("Amaze", "Extracting: " + entry);

        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile,cd,entry.getRealSize()));
        try {
            int len;
            byte buf[] = new byte[20480];
            while ((len = zipfile.read(buf)) > 0) {
                //System.out.println(id + " " + hash.get(id));
                if (hash.get(id)) {
                    outputStream.write(buf, 0, len);
                    copiedbytes=copiedbytes+len;
                    long time1=System.nanoTime()/500000000;
                    if(((int)time1)>((int)(time))){
                        calculateProgress(string,id,false);
                        time=System.nanoTime()/500000000;
                    }
                } else {
                    calculateProgress(string, id, true);
                    cancel(true);
                    stop(id);
                }
            }
        }finally {
            outputStream.close();

        }
    }
    public boolean extract(int id, File archive, String destinationPath,ArrayList<String> x) {
        int i = 0;
        ArrayList<ZipEntry> entry1=new ArrayList<ZipEntry>();
        try {
            ZipFile zipfile = new ZipFile(archive);
            calculateProgress(archive.getName(), id,false, copiedbytes, totalbytes);
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                if (hash.get(id)) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    for(String y:x){
                        if(y.endsWith("/")){
                        if(entry.getName().contains(y))entry1.add(entry);}
                        else {if(entry.getName().equals(y) || ("/"+entry.getName()).equals(y)){entry1.add(entry);}}
                    }
                    i++;
                } else {
                    cancel(true);
                    stop(id);
                     
                }
            }
            for (ZipEntry entry:entry1){totalbytes=totalbytes+entry.getSize();}
            for(ZipEntry entry:entry1){
                    unzipEntry(id, zipfile, entry, destinationPath);}
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            calculateProgress(archive.getName(), id,true, copiedbytes, totalbytes);
            return true;
        } catch (Exception e) {
            Log.e("amaze", "Error while extracting file " + archive, e);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            calculateProgress(archive.getName(), id,true, copiedbytes, totalbytes);
            return false;
        }

    }
    public boolean extract(int id, File archive, String destinationPath) {
        int i = 0;
        try {ArrayList<ZipEntry> arrayList=new ArrayList<ZipEntry>();
            ZipFile zipfile = new ZipFile(archive);
            calculateProgress(archive.getName(), id, false, copiedbytes, totalbytes);
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                //Log.i("Amaze", id + " " + hash.get(id));
                if (hash.get(id)) {
                     
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    arrayList.add(entry);
                } else {
                    stop(id);
                     
                }
            }for(ZipEntry entry:arrayList){totalbytes=totalbytes+entry.getSize();}
            for (ZipEntry entry : arrayList) {
                if (hash.get(id)) {
                    unzipEntry(id, zipfile, entry, destinationPath);

                } else {
                    stop(id);
                     
                }
            }
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            calculateProgress(archive.getName(), id, true, copiedbytes, totalbytes);
            return true;
        } catch (Exception e) {
            Log.e("amaze", "Error while extracting file " + archive, e);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            publishResults(archive.getName(),100,id,totalbytes,copiedbytes,true);
            return false;
        }

    }
    public boolean extractTar(int id, File archive, String destinationPath) {
        int i = 0;
        try {ArrayList<TarArchiveEntry> archiveEntries=new ArrayList<TarArchiveEntry>();
           TarArchiveInputStream inputStream;
            if(archive.getName().endsWith(".tar"))
            inputStream=new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)));
            else inputStream=new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archive)));
            publishResults(archive.getName(),0,id,totalbytes,copiedbytes,false);
            TarArchiveEntry tarArchiveEntry=inputStream.getNextTarEntry();
            while(tarArchiveEntry != null){
                if (hash.get(id)) {
                     
                    archiveEntries.add(tarArchiveEntry);
                    tarArchiveEntry=inputStream.getNextTarEntry();
                } else {
                    stop(id);
                     
                }
            }for(TarArchiveEntry entry:archiveEntries){totalbytes=totalbytes+entry.getSize();}
            for(TarArchiveEntry entry:archiveEntries){
                if (hash.get(id)) {
                    unzipTAREntry(id, inputStream, entry, destinationPath, archive.getName());
                } else {
                    stop(id);
                     
                }}

            inputStream.close();

            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            publishResults(archive.getName(),100,id,totalbytes,copiedbytes,true);
            return true;
        } catch (Exception e) {
            Log.e("amaze", "Error while extracting file " + archive, e);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            publishResults(archive.getName(),100,id,totalbytes,copiedbytes,true);
            return false;
        }

    }
    public boolean extractRar(int id, File archive, String destinationPath) {
        int i = 0;
        try {ArrayList<FileHeader> arrayList=new ArrayList<FileHeader>();
            Archive zipfile = new Archive(archive);
            FileHeader fh = zipfile.nextFileHeader();
            publishResults(archive.getName(),0,id,totalbytes,copiedbytes,false);
            while(fh != null){
                if (hash.get(id)) {
                     
                    arrayList.add(fh);
                fh=zipfile.nextFileHeader();
                } else {
                    stop(id);
                     
                }
            }
            for (FileHeader header:arrayList){totalbytes=totalbytes+header.getFullUnpackSize();}
            for (FileHeader header:arrayList){
                if (hash.get(id)) {
                    unzipRAREntry(id, archive.getName(), zipfile, header, destinationPath);
                } else {
                    stop(id);
                     
                }
            }
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            calculateProgress(archive.getName(), id, true, copiedbytes, totalbytes);
            return true;
        } catch (Exception e) {
            Log.e("amaze", "Error while extracting file " + archive, e);
            Intent intent = new Intent("loadlist");
            sendBroadcast(intent);
            calculateProgress(archive.getName(), id,true, copiedbytes, totalbytes);
            return false;
        }
    }
    protected Integer doInBackground(Bundle... p1) {
            String file = p1[0].getString("file");


        File f = new File(file);
        String path;
        if(epath.length()==0){
            path=f.getParent()+"/"+f.getName().substring(0,f.getName().lastIndexOf("."));
        }else{
            if(epath.endsWith("/")){path=epath+f.getName().substring(0,f.getName().lastIndexOf("."));}
            else {path=epath+"/"+f.getName().substring(0,f.getName().lastIndexOf("."));}
        }
            if(eentries) {
                extract(p1[0].getInt("id"), f,path, entries);
            }else if(f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".jar") || f.getName().toLowerCase().endsWith(".apk"))
            extract(p1[0].getInt("id"), f,path);
            else if(f.getName().toLowerCase().endsWith(".rar"))
                extractRar(p1[0].getInt("id"), f, path);
            else if(f.getName().toLowerCase().endsWith(".tar") || f.getName().toLowerCase().endsWith(".tar.gz"))
                extractTar(p1[0].getInt("id"), f, path);
            Log.i("Amaze", "Almost Completed");
            // TODO: Implement this method
            return p1[0].getInt("id");
        }

        @Override
        public void onPostExecute(Integer b) {
        stop(b);
        }

       
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(receiver1);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
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
        return mBinder;
    }}

