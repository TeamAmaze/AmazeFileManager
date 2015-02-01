package com.amaze.filemanager.services.asynctasks;

/**
 * Created by Arpit on 01-02-2015.
 */

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.RarViewer;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.ZipObj;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

public class RarExtractTask extends AsyncTask<FileHeader, Void, Void> {
    private String outputDir;
    private Archive zipFile;
    private RarViewer zipViewer;
    private String fileName;
    boolean open;FileHeader entry;
    public RarExtractTask(Archive zipFile, String outputDir, RarViewer zipViewer, String fileName,boolean open) {
        this.open=open;
        this.outputDir = outputDir;
        this.zipFile = zipFile;
        this.zipViewer = zipViewer;
        this.fileName = fileName;
    }

    @Override
    protected Void doInBackground(FileHeader... zipEntries) {
        entry=zipEntries[0];
        FileHeader zipEntry = zipEntries[0];
        try {if(open)unzipEntry1(zipFile, zipEntry, outputDir);
        else
            unzipEntry(zipFile, zipEntry, outputDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(open){
            Futils futils = new Futils();
            futils.openFile(new File(outputDir,fileName), (MainActivity) zipViewer.getActivity());
        }else Toast.makeText(zipViewer.getActivity(), "Extracted to " + outputDir, Toast.LENGTH_LONG).show();}

    private void createDir(File dir) {
        if (dir.exists()) {
            return;
        }
        Log.i("Amaze", "Creating dir " + dir.getName());
        if (!dir.mkdirs()) {
            throw new RuntimeException("Can not create dir " + dir);
        }
    }
    private void unzipEntry(Archive zipfile, FileHeader entry, String outputDir)
            throws IOException,RarException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getFileNameString()));
            for (FileHeader entry1:zipViewer.wholelist ) {
                if(entry1.getFileNameString().contains(entry.getFileNameString())){unzipEntry(zipfile,entry1,outputDir);}
            }
            return;
        }
        File outputFile = new File(outputDir, entry.getFileNameString());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }
        BufferedInputStream inputStream = new BufferedInputStream(
                zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(outputFile));
        try {
            int len;
            byte buf[] = new byte[1024];
            while ((len = inputStream.read(buf)) > 0) {

                outputStream.write(buf, 0, len);
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }private void unzipEntry1(Archive zipfile,FileHeader entry, String outputDir)
            throws IOException,RarException {

        File outputFile = new File(outputDir, fileName);
        BufferedInputStream inputStream = new BufferedInputStream(
                zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(outputFile));
        try {
            int len;
            byte buf[] = new byte[1024];
            while ((len = inputStream.read(buf)) > 0) {

                outputStream.write(buf, 0, len);
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }
}

