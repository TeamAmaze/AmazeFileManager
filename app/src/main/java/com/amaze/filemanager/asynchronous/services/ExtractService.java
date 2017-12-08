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

package com.amaze.filemanager.asynchronous.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.CopyDataParcelable;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.files.GenericCopyUtil;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
public class ExtractService extends Service {

    Context context;

    // list of data packages,// to initiate chart in process viewer fragment
    private ArrayList<CopyDataParcelable> dataPackages = new ArrayList<>();

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private ProgressHandler progressHandler;

    public static final String KEY_PATH_ZIP = "zip";
    public static final String KEY_ENTRIES_ZIP = "entries";
    public static final String TAG_BROADCAST_EXTRACT_CANCEL = "excancel";
    public static final String KEY_PATH_EXTRACT = "extractpath";

    @Override
    public void onCreate() {
        registerReceiver(receiver1, new IntentFilter(TAG_BROADCAST_EXTRACT_CANCEL));
        context = getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        String file = intent.getStringExtra(KEY_PATH_ZIP);
        String extractPath = intent.getStringExtra(KEY_PATH_EXTRACT);
        String[] entries = intent.getStringArrayExtra(KEY_ENTRIES_ZIP);

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        long totalSize = getTotalSize(file);
        progressHandler = new ProgressHandler(1, totalSize);

        progressHandler.setProgressListener((fileName, sourceFiles, sourceProgress, totalSize1, writtenSize, speed) -> {
            publishResults(startId, fileName, sourceFiles, sourceProgress, totalSize1, writtenSize, speed, false);
        });

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        mBuilder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(getResources().getString(R.string.extracting))
                .setContentText(new File(file).getName())
                .setSmallIcon(R.drawable.ic_zip_box_grey600_36dp);
        NotificationConstants.setMetadata(getApplicationContext(), mBuilder);
        startForeground(Integer.parseInt("123" + startId), mBuilder.build());

        new DoWork(this, progressHandler, file, extractPath, entries).execute();
        return START_STICKY;
    }

    /**
     * Method calculates zip file size to initiate progress
     * Supporting local file extraction progress for now
     *
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
        void onUpdate(CopyDataParcelable dataPackage);

        void refresh();
    }

    private void publishResults(int id, String fileName, int sourceFiles, int sourceProgress,
                                long total, long done, int speed, boolean isCompleted) {
        if (!progressHandler.getCancelled()) {
            mBuilder.setContentTitle(getResources().getString(R.string.extracting));
            float progressPercent = ((float) done / total) * 100;
            mBuilder.setProgress(100, Math.round(progressPercent), false);
            mBuilder.setOngoing(true);
            mBuilder.setContentText(fileName + " " + Formatter.formatFileSize(context, done) + "/"
                    + Formatter.formatFileSize(context, total));
            int id1 = Integer.parseInt("123" + id);
            mNotifyManager.notify(id1, mBuilder.build());
            if (progressPercent == 100 || total == 0) {
                mBuilder.setContentTitle(getString(R.string.extract_complete));
                mBuilder.setContentText(fileName + " " + Formatter.formatFileSize(context, total));
                mBuilder.setProgress(100, 100, false);
                mBuilder.setOngoing(false);
                mNotifyManager.notify(id1, mBuilder.build());
                publishCompletedResult("", id1);
            }

            CopyDataParcelable intent = new CopyDataParcelable();
            intent.setName(fileName);
            intent.setSourceFiles(sourceFiles);
            intent.setSourceProgress(sourceProgress);
            intent.setTotal(total);
            intent.setByteProgress(done);
            intent.setSpeedRaw(speed);
            intent.setMove(false);
            intent.setCompleted(isCompleted);
            putDataPackage(intent);

            if (progressListener != null) {
                progressListener.onUpdate(intent);
                if (isCompleted) progressListener.refresh();
            }
        } else publishCompletedResult(fileName, Integer.parseInt("123" + id));
    }

    public void publishCompletedResult(String a, int id1) {
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class DoWork extends AsyncTask<Void, Void, Void> {

        private WeakReference<ExtractService> extractService;
        private String[] entriesToExtract;
        private String extractionPath, compressedPath;
        private ProgressHandler progressHandler;
        private long totalBytes = 0L;
        private ServiceWatcherUtil watcherUtil;


        private DoWork(ExtractService extractService, ProgressHandler progressHandler, String cpath, String epath,
                       String[] entries) {
            this.extractService = new WeakReference<>(extractService);
            this.progressHandler = progressHandler;
            compressedPath = cpath;
            extractionPath = epath;
            entriesToExtract = entries;
        }

        @Override
        protected Void doInBackground(Void... p) {
            final Context context = this.extractService.get();
            if(context == null) return null;

            File f = new File(compressedPath);

            if (!compressedPath.equals(extractionPath)) {// custom extraction path not set, extract at default path
                extractionPath = f.getParent() + "/" + f.getName().substring(0, f.getName().lastIndexOf("."));
            } else if (extractionPath.endsWith("/")) {
                extractionPath = extractionPath + f.getName().substring(0, f.getName().lastIndexOf("."));
            }

            try {
                String path = f.getPath().toLowerCase();
                boolean isZip = path.endsWith(".zip") || path.endsWith(".jar") || path.endsWith(".apk");
                boolean isTar = path.endsWith(".tar") || path.endsWith(".tar.gz");
                boolean isRar = path.endsWith(".rar");

                if (entriesToExtract != null && entriesToExtract.length != 0) {
                    if (isZip) extract(context, f, extractionPath, entriesToExtract);
                    else if (isRar) extractRar(context, f, extractionPath, entriesToExtract);
                } else {
                    if (isZip) extract(context, f, extractionPath);
                    else if (isRar) extractRar(context, f, extractionPath);
                    else if (isTar) extractTar(context, f, extractionPath);
                }
            } catch (IOException | RarException e) {
                Log.e("amaze", "Error while extracting file " + compressedPath, e);
                AppConfig.toast(context, context.getString(R.string.error));
            }
            return null;
        }

        /**
         * Method extracts {@link ZipEntry} from {@link ZipFile}
         *
         * @param zipFile   zip file from which entriesToExtract are to be extracted
         * @param entry     zip entry that is to be extracted
         * @param outputDir output directory
         */
        private void unzipEntry(@NonNull final Context context, ZipFile zipFile, ZipEntry entry, String outputDir)
                throws IOException {

            if (entry.isDirectory()) {
                // zip entry is a directory, return after creating new directory
                FileUtil.mkdir(new File(outputDir, entry.getName()), context);
                return;
            }

            final File outputFile = new File(outputDir, entry.getName());

            if (!outputFile.getParentFile().exists()) {
                // creating directory if not already exists

                FileUtil.mkdir(outputFile.getParentFile(), context);
            }

            BufferedInputStream inputStream = new BufferedInputStream(
                    zipFile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile, context, 0));
            try {
                int len;
                byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                while ((len = inputStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION += len;
                }
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        private void unzipRAREntry(@NonNull final Context context, Archive zipFile, FileHeader entry, String outputDir)
                throws RarException, IOException {
            String name = entry.getFileNameString();
            name = name.replaceAll("\\\\", "/");
            if (entry.isDirectory()) {
                FileUtil.mkdir(new File(outputDir, name), context);
                return;
            }
            File outputFile = new File(outputDir, name);
            if (!outputFile.getParentFile().exists()) {
                FileUtil.mkdir(outputFile.getParentFile(), context);
            }
            //	Log.i("Amaze", "Extracting: " + entry);
            BufferedInputStream inputStream = new BufferedInputStream(
                    zipFile.getInputStream(entry));
            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile, context, entry.getFullUnpackSize()));
            try {
                int len;
                byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                while ((len = inputStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION += len;
                }
            } finally {
                outputStream.close();
                inputStream.close();
            }
        }

        private void unzipTAREntry(@NonNull final Context context, TarArchiveInputStream zipFileStream, TarArchiveEntry entry,
                                   String outputDir) throws IOException {
            String name = entry.getName();
            if (entry.isDirectory()) {
                FileUtil.mkdir(new File(outputDir, name), context);
                return;
            }
            File outputFile = new File(outputDir, name);
            if (!outputFile.getParentFile().exists()) {
                FileUtil.mkdir(outputFile.getParentFile(), context);
            }

            BufferedOutputStream outputStream = new BufferedOutputStream(
                    FileUtil.getOutputStream(outputFile, context, entry.getRealSize()));
            try {
                int len;
                byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
                while ((len = zipFileStream.read(buf)) > 0) {

                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.POSITION += len;
                }
            } finally {
                outputStream.close();
            }
        }

        /**
         * Helper method to initiate extraction of zip/jar files.
         *
         * @param archive         the file pointing to archive
         * @param destinationPath the where to extract
         * @param entryNamesList  names of files to be extracted from the archive
         * @return
         */
        private void extract(@NonNull final Context context, File archive, String destinationPath,
                                String[] entryNamesList) throws IOException {

            ArrayList<ZipEntry> entry1 = new ArrayList<>();
            ZipFile zipfile = new ZipFile(archive);

            // iterating archive elements to find file names that are to be extracted
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {

                ZipEntry zipEntry = (ZipEntry) e.nextElement();

                for (String entry : entryNamesList) {

                    if (zipEntry.getName().contains(entry)) {
                        // header to be extracted is atleast the entry path (may be more, when it is a directory)
                        entry1.add(zipEntry);
                    }
                }
            }

            // get the total size of elements to be extracted
            for (ZipEntry entry : entry1) {
                totalBytes += entry.getSize();
            }

            // setting total bytes calculated from zip entries
            progressHandler.setTotalSize(totalBytes);

            setInitDataPackage(totalBytes, entry1.get(0).getName(), entryNamesList.length);

            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
            watcherUtil.watch();

            int i = 0;
            for (ZipEntry entry : entry1) {
                if (!progressHandler.getCancelled()) {

                    progressHandler.setFileName(entry.getName());
                    unzipEntry(context, zipfile, entry, destinationPath);
                    progressHandler.setSourceFilesProcessed(++i);
                }
            }
        }

        private void extract(@NonNull final Context context, File archive, String destinationPath) throws IOException {
            ArrayList<ZipEntry> arrayList = new ArrayList<>();
            ZipFile zipfile = new ZipFile(archive);
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {

                // adding all the elements to be extracted to an array list
                ZipEntry entry = (ZipEntry) e.nextElement();
                arrayList.add(entry);
            }

            for (ZipEntry entry : arrayList) {
                // calculating size of compressed items
                totalBytes += entry.getSize();
            }

            // setting total bytes calculated from zip entries
            progressHandler.setTotalSize(totalBytes);

            setInitDataPackage(totalBytes, arrayList.get(0).getName(), 1);

            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
            watcherUtil.watch();

            for (ZipEntry entry : arrayList) {
                if (!progressHandler.getCancelled()) {

                    progressHandler.setFileName(entry.getName());
                    unzipEntry(context, zipfile, entry, destinationPath);
                }
            }
            progressHandler.setSourceFilesProcessed(1);
        }

        private void extractTar(@NonNull final Context context, File archive, String destinationPath) throws IOException {
            ArrayList<TarArchiveEntry> archiveEntries = new ArrayList<>();

            TarArchiveInputStream inputStream = createTarInputStream(archive);

            TarArchiveEntry tarArchiveEntry = inputStream.getNextTarEntry();

            while (tarArchiveEntry != null) {
                archiveEntries.add(tarArchiveEntry);
                tarArchiveEntry = inputStream.getNextTarEntry();
            }

            for (TarArchiveEntry entry : archiveEntries) {
                totalBytes += entry.getSize();
            }

            // setting total bytes calculated from zip entries
            progressHandler.setTotalSize(totalBytes);

            setInitDataPackage(totalBytes, archiveEntries.get(0).getName(), 1);

            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
            watcherUtil.watch();

            inputStream = createTarInputStream(archive);

            for (TarArchiveEntry entry : archiveEntries) {

                if (!progressHandler.getCancelled()) {

                    inputStream.getNextTarEntry();
                    progressHandler.setFileName(entry.getName());
                    unzipTAREntry(context, inputStream, entry, destinationPath);
                }
            }
            progressHandler.setSourceFilesProcessed(1);

            // operating finished
            inputStream.close();
        }

        private TarArchiveInputStream createTarInputStream(File archive) throws IOException {
            if (archive.getName().endsWith(".tar")) {
                return new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(archive)));
            } else {
                return new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archive)));
            }
        }

        private void extractRar(@NonNull final Context context, File archive, String destinationPath) throws IOException, RarException {
            ArrayList<FileHeader> arrayList = new ArrayList<>();
            Archive zipFile = new Archive(archive);
            FileHeader fh = zipFile.nextFileHeader();

            while (fh != null) {
                arrayList.add(fh);
                fh = zipFile.nextFileHeader();

            }

            for (FileHeader header : arrayList) {
                totalBytes += header.getFullUnpackSize();
            }

            // setting total bytes calculated from zip entriesToExtract
            progressHandler.setTotalSize(totalBytes);

            setInitDataPackage(totalBytes, arrayList.get(0).getFileNameString(), 1);

            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
            watcherUtil.watch();

            for (FileHeader header : arrayList) {

                if (!progressHandler.getCancelled()) {

                    progressHandler.setFileName(header.getFileNameString());
                    unzipRAREntry(context, zipFile, header, destinationPath);
                }
            }
            progressHandler.setSourceFilesProcessed(1);
        }

        private void extractRar(@NonNull final Context context, File archive, String destinationPath,
                                String[] entriesToExtract) throws IOException, RarException {
            Archive rarFile = new Archive(archive);

            ArrayList<FileHeader> arrayList = new ArrayList<>();

            // iterating archive elements to find file names that are to be extracted
            for (FileHeader header : rarFile.getFileHeaders()) {
                for (String entry : entriesToExtract) {

                    if (header.getFileNameString().contains(entry)) {
                        // header to be extracted is atleast the entry path (may be more, when it is a directory)
                        arrayList.add(header);
                    }
                }
            }

            // get the total size of elements to be extracted
            for (FileHeader entry : arrayList) {
                totalBytes += entry.getFullUnpackSize();
            }

            // setting total bytes calculated from zip entries
            progressHandler.setTotalSize(totalBytes);

            setInitDataPackage(totalBytes, arrayList.get(0).getFileNameString(), arrayList.size());

            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
            watcherUtil.watch();

            int i = 0;
            for (FileHeader entry : arrayList) {
                if (!progressHandler.getCancelled()) {

                    progressHandler.setFileName(entry.getFileNameString());
                    unzipRAREntry(context, rarFile, entry, destinationPath);
                    progressHandler.setSourceFilesProcessed(++i);
                }
            }
        }

        @Override
        public void onPostExecute(Void b) {
            final ExtractService extractService = this.extractService.get();
            if(extractService == null) return;

            // check whether watcherutil was initialized. It was not initialized when we got exception
            // in extracting the file
            if (watcherUtil != null) watcherUtil.stopWatch();
            Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, extractionPath);
            extractService.sendBroadcast(intent);
            extractService.stopSelf();
        }

        /**
         * Setting initial package to initialize charts in process viewer properly
         */
        private void setInitDataPackage(long totalSize, String fileName, int sourceTotal) {
            final ExtractService extractService = this.extractService.get();
            if(extractService == null) return;

            CopyDataParcelable intent1 = new CopyDataParcelable();
            intent1.setName(fileName);
            intent1.setSourceFiles(sourceTotal);
            intent1.setSourceProgress(0);
            intent1.setTotal(totalSize);
            intent1.setByteProgress(0);
            intent1.setSpeedRaw(0);
            intent1.setMove(false);
            intent1.setCompleted(false);
            extractService.putDataPackage(intent1);
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
     * data to be transferred to {@link ProcessViewerFragment}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     *
     * @return
     */
    public synchronized CopyDataParcelable getDataPackage(int index) {
        return this.dataPackages.get(index);
    }

    public synchronized int getDataPackageSize() {
        return this.dataPackages.size();
    }

    /**
     * Puts a {@link CopyDataParcelable} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     *
     * @param dataPackage
     */
    private synchronized void putDataPackage(CopyDataParcelable dataPackage) {
        this.dataPackages.add(dataPackage);
    }

}

