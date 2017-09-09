package com.amaze.filemanager.asynchronous.asynctasks;

/**
 * Created by Arpit on 25-01-2015.
 */

import android.os.AsyncTask;

import com.amaze.filemanager.fragments.ZipExplorerFragment;
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

    ZipExplorerFragment zipExplorerFragment;
    String dir;

    /**
     * AsyncTask to load RAR file items.
     * @param zipExplorerFragment the zipExplorerFragment fragment instance
     * @param dir
     */
    public RarHelperTask(ZipExplorerFragment zipExplorerFragment, String dir) {
        this.zipExplorerFragment = zipExplorerFragment;
        this.dir = dir;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        zipExplorerFragment.swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected ArrayList<FileHeader> doInBackground(File... params) {
        ArrayList<FileHeader> elements = new ArrayList<>();
        try {
            Archive zipfile = new Archive(params[0]);
            zipExplorerFragment.archive = zipfile;
            if (zipExplorerFragment.wholelistRar.size() == 0) {

                FileHeader fh = zipfile.nextFileHeader();
                while (fh != null) {
                    zipExplorerFragment.wholelistRar.add(fh);
                    fh = zipfile.nextFileHeader();
                }
            }
            if (dir == null || dir.trim().length() == 0 || dir.equals("")) {

                for (FileHeader header : zipExplorerFragment.wholelistRar) {
                    String name = header.getFileNameString();

                    if (!name.contains("\\")) {
                        elements.add(header);

                    }
                }
            } else {
                for (FileHeader header : zipExplorerFragment.wholelistRar) {
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
        zipExplorerFragment.swipeRefreshLayout.setRefreshing(false);
        zipExplorerFragment.createRarViews(zipEntries, dir);
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

