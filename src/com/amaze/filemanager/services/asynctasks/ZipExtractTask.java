package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.utils.Futils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Vishal on 11/27/2014.
 */
public class ZipExtractTask extends AsyncTask<ZipEntry, Void, Void> {
    private String outputDir;
    private ZipFile zipFile;
    private ZipViewer zipViewer;
    private String fileName;

    public ZipExtractTask(ZipFile zipFile, String outputDir, ZipViewer zipViewer, String fileName) {

        this.outputDir = outputDir;
        this.zipFile = zipFile;
        this.zipViewer = zipViewer;
        this.fileName = fileName;
    }

    @Override
    protected Void doInBackground(ZipEntry... zipEntries) {

        ZipEntry zipEntry = zipEntries[0];
        try {
            unzipEntry(zipFile, zipEntry, outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        Futils futils = new Futils();
        futils.openFile(new File(outputDir + "/" + fileName), (MainActivity) zipViewer.getActivity());
    }

    private void unzipEntry(ZipFile zipfile, ZipEntry entry, String outputDir)
            throws IOException {

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
