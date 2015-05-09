package com.amaze.filemanager.services.asynctasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.RootHelper;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.stericson.RootTools.RootTools;

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
public class ZipExtractTask extends AsyncTask<Void, Void, Void> {
    private String outputDir;
    private ZipFile zipFile;
    private Activity zipViewer;
    private String fileName;
    boolean zip;ZipEntry entry;
    Archive rar;
    FileHeader header;
    File output;    
    public ZipExtractTask(ZipFile zipFile, String outputDir, Activity zipViewer, String fileName,boolean zip,ZipEntry zipEntry) {
        this.zip=zip;
        this.outputDir = outputDir;
        this.zipFile = zipFile;
        this.zipViewer = zipViewer;
        this.fileName = fileName;
        this.entry=zipEntry;
    }
    public ZipExtractTask(Archive rar, String outputDir, Activity zipViewer, String fileName,boolean zip,FileHeader fileHeader) {
        this.zip=zip;
        this.outputDir = outputDir;
        this.rar = rar;
        this.zipViewer = zipViewer;
        this.fileName = fileName;
        this.header = fileHeader;
    }
    @Override
    protected Void doInBackground(Void... zipEntries) {
        
        try {
            if (zip) unzipEntry1(zipFile, entry, outputDir);
            else unzipRAREntry(rar,header,outputDir);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        String cmd = "chmod 777 " + output.getPath();
        Log.d("change permissions", cmd);
        RootHelper.runAndWait(cmd, false);
        Futils futils = new Futils();
        futils.openFile(output, (MainActivity) zipViewer);
}
    
private void unzipEntry1(ZipFile zipfile, ZipEntry entry, String outputDir)
            throws IOException {

        output = new File(outputDir, fileName);
        BufferedInputStream inputStream = new BufferedInputStream(
                zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(output));
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
    
 private void unzipRAREntry(Archive zipfile, FileHeader header, String outputDir)
            throws IOException, RarException {

         output = new File(outputDir + "/" + header.getFileNameString().trim());
        FileOutputStream fileOutputStream = new FileOutputStream(output);
        zipfile.extractFile(header, fileOutputStream);
    }

}
