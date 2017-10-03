package com.amaze.filemanager.asynchronous.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.Pair;

import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Arpit on 25-01-2015 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class RarHelperTask extends AsyncTask<Void, Void, Pair<Archive, ArrayList<FileHeader>>> {

    private WeakReference<Context> context;
    private String fileLocation;
    private String relativeDirectory;
    private OnAsyncTaskFinished<Pair<Archive, ArrayList<FileHeader>>> onFinish;

    /**
     * AsyncTask to load RAR file items.
     * @param realFileDirectory the location of the zip file
     * @param dir relativeDirectory to access inside the zip file
     */
    public RarHelperTask(Context c, String realFileDirectory, String dir, OnAsyncTaskFinished<Pair<Archive, ArrayList<FileHeader>>> l) {
        context = new WeakReference<>(c);
        fileLocation = realFileDirectory;
        relativeDirectory = dir;
        onFinish = l;
    }

    @Override
    protected Pair<Archive, ArrayList<FileHeader>> doInBackground(Void... params) {
        try {
            ArrayList<FileHeader> elements = new ArrayList<>();
            Archive zipfile = new Archive(new File(fileLocation));
            String relativeDirDiffSeparator = relativeDirectory.replace("/", "\\");

            for (FileHeader header : zipfile.getFileHeaders()) {
                String name = header.getFileNameString();//This uses \ as separator, not /
                boolean isInBaseDir = (relativeDirDiffSeparator == null || relativeDirDiffSeparator.equals("")) && !name.contains("\\");
                boolean isInRelativeDir = relativeDirDiffSeparator != null && name.contains("\\")
                        && name.substring(0, name.lastIndexOf("\\")).equals(relativeDirDiffSeparator);

                if (isInBaseDir || isInRelativeDir) {
                    elements.add(header);
                }
            }
            Collections.sort(elements, new FileListSorter());
            return new Pair<>(zipfile, elements);
        } catch (RarException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(Pair<Archive, ArrayList<FileHeader>> ArchivePairZipEntries) {
        super.onPostExecute(ArchivePairZipEntries);
        onFinish.onAsyncTaskFinished(ArchivePairZipEntries);
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

