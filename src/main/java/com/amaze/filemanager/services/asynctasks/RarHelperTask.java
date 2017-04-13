package com.amaze.filemanager.services.asynctasks;

/**
 * Created by Arpit on 25-01-2015.
 */

import android.os.AsyncTask;

import com.amaze.filemanager.fragments.ZipViewer;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Vishal on 11/23/2014.
 */
public class RarHelperTask extends AsyncTask<File, Void, ArrayList<FileHeader>> {

    ZipViewer zipViewer;
    String dir;

    /**
     * AsyncTask to load RAR file items.
     * @param zipViewer the zipViewer fragment instance
     * @param dir
     */
    public RarHelperTask(ZipViewer zipViewer, String dir) {
        this.zipViewer = zipViewer;
        this.dir = dir;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        zipViewer.swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected ArrayList<FileHeader> doInBackground(File... params) {
        ArrayList<FileHeader> elements = new ArrayList<>();
        try {
            Archive zipfile = new Archive(params[0]);
            zipViewer.archive = zipfile;
            if (zipViewer.wholelistRar.size() == 0) {

                FileHeader fh = zipfile.nextFileHeader();
                while (fh != null) {
                    zipViewer.wholelistRar.add(fh);
                    fh = zipfile.nextFileHeader();
                }
            }
            if (dir == null || dir.trim().length() == 0 || dir.equals("")) {

                for (FileHeader header : zipViewer.wholelistRar) {
                    String name = header.getFileNameString();

                    if (!name.contains("\\")) {
                        elements.add(header);

                    }
                }
            } else {
                for (FileHeader header : zipViewer.wholelistRar) {
                    String name = header.getFileNameString();
                    if (name.substring(0, name.lastIndexOf("\\")).equals(dir)) {
                        elements.add(header);
                    }
                }
            }
        } catch (Exception e) {

        }
        Collections.sort(elements, new FileListSorter());
        return elements;
    }

    @Override
    protected void onPostExecute(ArrayList<FileHeader> zipEntries) {
        super.onPostExecute(zipEntries);
        zipViewer.swipeRefreshLayout.setRefreshing(false);
        zipViewer.createRarviews(zipEntries, dir);
    }

    private class FileListSorter implements Comparator<FileHeader> {
        @Override
        public int compare(FileHeader file1, FileHeader file2) {

            if (file1.isDirectory() && !file2.isDirectory()) {
                return -1;


            } else if (file2.isDirectory() && !(file1).isDirectory()) {
                return 1;
            }
            return file1.getFileNameString().compareToIgnoreCase(file2.getFileNameString());
        }
    }
}

