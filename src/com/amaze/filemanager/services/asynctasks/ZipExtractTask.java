package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;
import android.widget.Toast;

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
public class ZipExtractTask extends AsyncTask<ZipEntry, Void, String> {
    private String outputDir;
    private ZipFile zipFile;
    private BufferedOutputStream bufferedOutputStream;
    private BufferedInputStream bufferedInputStream;
    private ZipViewer zipViewer;

    public ZipExtractTask(ZipFile zipFile, String outputDir, ZipViewer zipViewer) {

        this.outputDir = outputDir;
        this.zipFile = zipFile;
        this.zipViewer = zipViewer;
    }

    @Override
    protected String doInBackground(ZipEntry... zipEntries) {

        ZipEntry zipEntry = zipEntries[0];
        File outputFile = new File(outputDir, zipEntry.getName());
        try {
            bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
            int len;
            byte buf[] = new byte[1024];
            while ((len = bufferedInputStream.read(buf)) > 0) {
                bufferedOutputStream.write(buf, 0, len);
            }

            try {
                bufferedOutputStream.close();
                bufferedInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            /*try {
                bufferedOutputStream.close();
                bufferedInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }

        return outputFile.getPath();
    }

    @Override
    protected void onPostExecute(String aVoid) {
        super.onPostExecute(aVoid);

        Futils futils = new Futils();
        futils.openFile(new File(aVoid), (MainActivity) zipViewer.getActivity());
        Toast.makeText(zipViewer.getActivity(), outputDir, Toast.LENGTH_LONG).show();
    }
}
