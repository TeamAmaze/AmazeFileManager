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
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.GenericCopyThread;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
public class ExtractService extends Service {

    Context cd;

    // list of data packages, to initiate chart in process viewer fragment
    private ArrayList<DataPackage> dataPackages = new ArrayList<>();
    long totalSize = 0l;    // total size of file, can change later

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private ArrayList<String> entries=new ArrayList<String>();  // names of entries to be extracted
    private String epath;
    private ProgressHandler progressHandler;


    public static final String KEY_PATH_ZIP = "zip";
    public static final String KEY_PATH_EXTRACT = "extractpath";
    public static final String TAG_BROADCAST_EXTRACT_CANCEL = "excancel";
    public static final String KEY_ENTRIES_ZIP = "entries";

    @Override
    public void onCreate() {
        registerReceiver(receiver1, new IntentFilter(TAG_BROADCAST_EXTRACT_CANCEL));
        cd=getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Bundle b = new Bundle();
        String file = intent.getStringExtra(KEY_PATH_ZIP);
        epath = PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_PATH_EXTRACT, file);
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        entries=intent.getStringArrayListExtra(KEY_ENTRIES_ZIP);

        b.putString(KEY_PATH_ZIP, file);

        totalSize = getTotalSize(file);
        progressHandler = new ProgressHandler(1, totalSize);

        progressHandler.setProgressListener(new ProgressHandler.ProgressListener() {
            @Override
            public void onProgressed(String fileName, int sourceFiles, int sourceProgress, long totalSize,
                                     long writtenSize, int speed) {
                publishResults(startId, fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed, false);
            }
        });

        /*DataPackage intent1 = new DataPackage();
        intent1.setName(file);
        intent1.setSourceFiles(entries==null ? 1 : entries.size());
        intent1.setSourceProgress(0);
        intent1.setTotal(totalSize);
        intent1.setByteProgress(0);
        intent1.setSpeedRaw(0);
        intent1.setMove(false);
        intent1.setCompleted(false);
        putDataPackage(intent1);*/

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder = new NotificationCompat.Builder(cd);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(getResources().getString(R.string.extracting))
                .setContentText(new File(file).getName())
                .setSmallIcon(R.drawable.ic_doc_compressed);
        startForeground(Integer.parseInt("123" + startId), mBuilder.build());

        new DoWork().execute(b);
        return START_STICKY;
    }

    /**
     * Method calculates zip file size to initiate progress
     * Supporting local file extraction progress for now
     * @param filePath
     * @return
     */
    private long getTotalSize(String filePath) {

        return new File(filePath).length();
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

    private void publishResults(int id, String fileName, int sourceFiles, int sourceProgress, long total, long done,
                                int speed, boolean isCompleted) {
        if(!progressHandler.getCancelled()) {
            mBuilder.setContentTitle(getResources().getString(R.string.extracting));

            float progressPercent = ((float) (done / total) * 100);
            mBuilder.setProgress(100, Math.round(progressPercent), false);
            mBuilder.setOngoing(true);
            mBuilder.setContentText(fileName + " " + Futils.readableFileSize(done) + "/"
                    + Futils.readableFileSize(total));
            int id1=Integer.parseInt("123"+id);
            mNotifyManager.notify(id1, mBuilder.build());
            if(progressPercent==100 || total == 0){
                mBuilder.setContentTitle(getString(R.string.extract_complete));
                mBuilder.setContentText(fileName + " " + Futils.readableFileSize(total));
                mBuilder.setProgress(100, 100, false);
                mBuilder.setOngoing(false);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult("", id1);
            }

            DataPackage intent = new DataPackage();
            intent.setName(fileName);
            intent.setSourceFiles(sourceFiles);
            intent.setSourceProgress(sourceProgress);
            intent.setTotal(total);
            intent.setByteProgress(done);
            intent.setSpeedRaw(speed);
            intent.setMove(false);
            intent.setCompleted(isCompleted);
            putDataPackage(intent);

            if(progressListener!=null){
                progressListener.onUpdate(intent);
                if(isCompleted) progressListener.refresh();
            }
        } else publishCompletedResult(fileName, Integer.parseInt("123"+id));
    }

    public void publishCompletedResult(String a,int id1){
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class DoWork extends AsyncTask<Bundle, Void, Integer> {

        long totalBytes = 0l;
        private ServiceWatcherUtil watcherUtil;

        private void createDir(File dir) {
            FileUtil.mkdir(dir, cd);
        }

        /**
         * Method extracts {@link ZipEntry} from {@link ZipFile}
         * @param zipFile zip file from which entries are to be extracted
         * @param entry zip entry that is to be extracted
         * @param outputDir output directory
         * @throws Exception
         */
        private void unzipEntry(ZipFile zipFile, ZipEntry entry, String outputDir)
                throws Exception {

            if (entry.isDirectory()) {
                // zip entry is a directory, return after creating new directory
                createDir(new File(outputDir, entry.getName()));
                return;
            }

            final File outputFile = new File(outputDir, entry.getName());

            if (!outputFile.getParentFile().exists()) {
                // creating directory if not already exists

                createDir(outputFile.getParentFile());
            }

            BufferedInputStream inputStream = new BufferedInputStream(
                    zipFile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile, cd, 0));
            try {
                int len;
                byte buf[] = new byte[GenericCopyThread.DEFAULT_BUFFER_SIZE];
                while ((len = inputStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION+=len;
                }
            } catch (Exception e) {

                throw new Exception();
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        private void unzipRAREntry(Archive zipFile, FileHeader entry, String outputDir)
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
                    zipFile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile, cd,entry.getFullUnpackSize()));
            try {
                int len;
                byte buf[] = new byte[GenericCopyThread.DEFAULT_BUFFER_SIZE];
                while ((len = inputStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION+=len;
                }
            } catch (Exception e) {

                throw new Exception();
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        private void unzipTAREntry(TarArchiveInputStream zipFileStream, TarArchiveEntry entry,
                                   String outputDir) throws Exception {
            String name=entry.getName();
            if (entry.isDirectory()) {
                createDir(new File(outputDir, name));
                return;
            }
            File outputFile = new File(outputDir, name);
            if (!outputFile.getParentFile().exists()) {
                createDir(outputFile.getParentFile());
            }

            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile,cd,entry.getRealSize()));
            try {
                int len;
                byte buf[] = new byte[20480];
                while ((len = zipFileStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION += len;
                }
            } catch (Exception e) {

                throw new Exception();
            } finally {
                outputStream.close();
            }
        }

        /**
         * Helper method to initiate extraction of zip/jar files.
         * @param archive the file pointing to archive
         * @param destinationPath the where to extract
         * @param entryNamesList names of files to be extracted from the archive
         * @return
         */
        public boolean extract(File archive, String destinationPath,
                               ArrayList<String> entryNamesList) {

            ArrayList<ZipEntry> entry1=new ArrayList<ZipEntry>();
            try {
                ZipFile zipfile = new ZipFile(archive);

                // iterating archive elements to find file names that are to be extracted
                for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {

                    ZipEntry entry = (ZipEntry) e.nextElement();
                    for(String y:entryNamesList){
                        if(y.endsWith("/")){
                            if(entry.getName().contains(y))
                                entry1.add(entry);
                        } else {
                            if(entry.getName().equals(y) || ("/"+entry.getName()).equals(y)) {
                                entry1.add(entry);
                            }
                        }
                    }
                }

                // get the total size of elements to be extracted
                for (ZipEntry entry:entry1) {
                    totalBytes+=entry.getSize();
                }

                // setting total bytes calculated from zip entries
                progressHandler.setTotalSize(totalBytes);

                watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
                watcherUtil.watch();

                int sourceProgress = 0;
                for(ZipEntry entry:entry1) {
                    progressHandler.setFileName(entry.getName());
                    unzipEntry(zipfile, entry, destinationPath);
                    progressHandler.setSourceFilesCopied(++sourceProgress);
                }

                Intent intent = new Intent("loadlist");
                sendBroadcast(intent);
                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(getApplicationContext(), getString(R.string.error));
                return false;
            }

        }

        public boolean extract(File archive, String destinationPath) {

            try {
                ArrayList<ZipEntry> arrayList=new ArrayList<ZipEntry>();
                ZipFile zipfile = new ZipFile(archive);
                for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {

                    // adding all the elements to be extracted to an array list
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    arrayList.add(entry);
                }

                for(ZipEntry entry:arrayList) {
                    // calculating size of compressed items
                    totalBytes += entry.getSize();
                }

                // setting total bytes calculated from zip entries
                progressHandler.setTotalSize(totalBytes);

                watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
                watcherUtil.watch();

                for (ZipEntry entry : arrayList) {
                    progressHandler.setFileName(entry.getName());
                    unzipEntry(zipfile, entry, destinationPath);
                }

                // operating finished
                progressHandler.setSourceFilesCopied(1);
                Intent intent = new Intent("loadlist");
                sendBroadcast(intent);
                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(getApplicationContext(), getString(R.string.error));
                return false;
            }

        }

        public boolean extractTar(File archive, String destinationPath) {

            try {

                ArrayList<TarArchiveEntry> archiveEntries=new ArrayList<TarArchiveEntry>();

                TarArchiveInputStream inputStream;

                if(archive.getName().endsWith(".tar"))
                    inputStream =new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)));
                else inputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archive)));

                TarArchiveEntry tarArchiveEntry=inputStream.getNextTarEntry();

                while(tarArchiveEntry != null){
                    archiveEntries.add(tarArchiveEntry);
                    tarArchiveEntry=inputStream.getNextTarEntry();
                }

                for(TarArchiveEntry entry : archiveEntries) {
                    totalBytes += entry.getSize();
                }

                // setting total bytes calculated from zip entries
                progressHandler.setTotalSize(totalBytes);

                watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
                watcherUtil.watch();

                for(TarArchiveEntry entry : archiveEntries){

                    progressHandler.setFileName(entry.getName());
                    unzipTAREntry(inputStream, entry, destinationPath);
                }

                // operating finished
                progressHandler.setSourceFilesCopied(1);
                inputStream.close();

                Intent intent = new Intent("loadlist");
                sendBroadcast(intent);
                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(getApplicationContext(), getString(R.string.error));
                return false;
            }

        }
        public boolean extractRar(File archive, String destinationPath) {

            try {
                ArrayList<FileHeader> arrayList=new ArrayList<FileHeader>();
                Archive zipFile = new Archive(archive);
                FileHeader fh = zipFile.nextFileHeader();

                while(fh != null){
                    arrayList.add(fh);
                    fh=zipFile.nextFileHeader();

                }

                for (FileHeader header:arrayList) {
                    totalBytes += header.getFullUnpackSize();
                }

                // setting total bytes calculated from zip entries
                progressHandler.setTotalSize(totalBytes);

                watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
                watcherUtil.watch();

                for (FileHeader header:arrayList){

                    progressHandler.setFileName(header.getFileNameString());
                    unzipRAREntry(zipFile, header, destinationPath);
                }

                progressHandler.setSourceFilesCopied(1);
                Intent intent = new Intent("loadlist");
                sendBroadcast(intent);
                return true;
            } catch (Exception e) {
                Log.e("amaze", "Error while extracting file " + archive, e);
                AppConfig.toast(getApplicationContext(), getString(R.string.error));
                return false;
            }
        }

        protected Integer doInBackground(Bundle... p1) {

            String file = p1[0].getString(KEY_PATH_ZIP);

            File f = new File(file);

            String path;
            if(epath.equals(file)){

                // custom extraction path not set, extract at default path
                path=f.getParent()+"/"+f.getName().substring(0,f.getName().lastIndexOf("."));
            } else{

                if(epath.endsWith("/")) {
                    path=epath+f.getName().substring(0,f.getName().lastIndexOf("."));
                } else {
                    path=epath+"/"+f.getName().substring(0,f.getName().lastIndexOf("."));
                }
            }

            if(entries!=null && entries.size()!=0) {
                extract(f, path, entries);
            } else if(f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".jar") || f.getName().toLowerCase().endsWith(".apk"))
                extract(f, path);
            else if(f.getName().toLowerCase().endsWith(".rar"))
                extractRar(f, path);
            else if(f.getName().toLowerCase().endsWith(".tar") || f.getName().toLowerCase().endsWith(".tar.gz"))
                extractTar(f, path);
            Log.i("Amaze", "Almost Completed");
            // TODO: Implement this method
            return p1[0].getInt("id");
        }

        @Override
        public void onPostExecute(Integer b) {
            stopSelf();
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
            progressHandler.setCancelled(true);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    /**
     * Returns the {@link #dataPackages} list which contains
     * data to be transferred to {@link com.amaze.filemanager.fragments.ProcessViewer}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
     * @return
     */
    public synchronized ArrayList<DataPackage> getDataPackageList() {
        return this.dataPackages;
    }

    /**
     * Puts a {@link DataPackage} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
     * @param dataPackage
     */
    private synchronized void putDataPackage(DataPackage dataPackage) {
        this.dataPackages.add(dataPackage);
    }
}

