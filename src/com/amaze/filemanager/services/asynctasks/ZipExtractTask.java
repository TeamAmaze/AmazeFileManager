package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.ZipObj;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
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
    boolean open;ZipEntry entry;
    public ZipExtractTask(ZipFile zipFile, String outputDir, ZipViewer zipViewer, String fileName,boolean open) {
        this.open=open;
        this.outputDir = outputDir;
        this.zipFile = zipFile;
        this.zipViewer = zipViewer;
        this.fileName = fileName;
    }

    @Override
    protected Void doInBackground(ZipEntry... zipEntries) {
        entry=zipEntries[0];
        ZipEntry zipEntry = zipEntries[0];
        try {if(open)unzipEntry1(zipFile, zipEntry, outputDir);
            else
            unzipEntry(zipFile, zipEntry, outputDir);
        } catch (IOException e) {
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
    }else Toast.makeText(zipViewer.getActivity(),"Extracted to "+outputDir,Toast.LENGTH_LONG).show();}

    private void createDir(File dir) {
        if (dir.exists()) {
            return;
        }
        Log.i("Amaze", "Creating dir " + dir.getName());
        if (!dir.mkdirs()) {
            throw new RuntimeException("Can not create dir " + dir);
        }
    }
    private void unzipEntry(ZipFile zipfile, ZipEntry entry, String outputDir)
            throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            for (ZipObj entry1:zipViewer.wholelist ) {
                if(entry1.getName().contains(entry.getName())){unzipEntry(zipfile,entry1.getEntry(),outputDir);}
            }
                return;
        }
        File outputFile = new File(outputDir, entry.getName());
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
    }private void unzipEntry1(ZipFile zipfile, ZipEntry entry, String outputDir)
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
