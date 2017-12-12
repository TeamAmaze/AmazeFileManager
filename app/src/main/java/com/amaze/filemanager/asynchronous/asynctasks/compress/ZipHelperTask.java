package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by Vishal on 11/23/2014 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class ZipHelperTask extends AsyncTask<Void, Void, ArrayList<CompressedObjectParcelable>> {

    private WeakReference<Context> context;
    private Uri fileLocation;
    private String relativeDirectory;
    private boolean createBackItem;
    private OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> onFinish;

    /**
     * AsyncTask to load ZIP file items.
     * @param realFileDirectory the location of the zip file
     * @param dir relativeDirectory to access inside the zip file
     */
    public ZipHelperTask(Context c, String realFileDirectory, String dir, boolean goback,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        context = new WeakReference<>(c);
        fileLocation = Uri.parse(realFileDirectory);
        relativeDirectory = dir;
        createBackItem = goback;
        onFinish = l;
    }

    @Override
    protected ArrayList<CompressedObjectParcelable> doInBackground(Void... params) {
        ArrayList<CompressedObjectParcelable> elements = new ArrayList<>();

        if (createBackItem) {
            elements.add(0, new CompressedObjectParcelable());
        }

        try {
            ArrayList<CompressedObjectParcelable> wholelist = new ArrayList<>();
            if (new File(fileLocation.getPath()).canRead()) {
                ZipFile zipfile = new ZipFile(fileLocation.getPath());
                for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    wholelist.add(new CompressedObjectParcelable(entry.getName(), entry.getTime(), entry.getSize(), entry.isDirectory()));
                }
            } else {
                ZipInputStream zipfile1 = new ZipInputStream(context.get().getContentResolver().openInputStream(fileLocation));
                for (ZipEntry entry = zipfile1.getNextEntry(); entry != null; entry = zipfile1.getNextEntry()) {
                    wholelist.add(new CompressedObjectParcelable(entry.getName(), entry.getTime(), entry.getSize(), entry.isDirectory()));
                }
            }

            ArrayList<String> strings = new ArrayList<>();

            for (CompressedObjectParcelable entry : wholelist) {
                File file = new File(entry.name);
                if (relativeDirectory == null || relativeDirectory.trim().length() == 0) {
                    String y = entry.name;
                    if (y.startsWith("/"))
                        y = y.substring(1, y.length());
                    if (file.getParent() == null || file.getParent().length() == 0 || file.getParent().equals("/")) {
                        if (!strings.contains(y)) {
                            elements.add(new CompressedObjectParcelable(y, entry.date, entry.size, entry.directory));
                            strings.add(y);
                        }
                    } else {
                        String path = y.substring(0, y.indexOf("/") + 1);
                        if (!strings.contains(path)) {
                            CompressedObjectParcelable zipObj = new CompressedObjectParcelable(path, entry.date, entry.size, true);
                            strings.add(path);
                            elements.add(zipObj);
                        }
                    }
                } else {
                    String y = entry.name;
                    if (entry.name.startsWith("/"))
                        y = y.substring(1, y.length());

                    if (file.getParent() != null && (file.getParent().equals(relativeDirectory) || file.getParent().equals("/" + relativeDirectory))) {
                        if (!strings.contains(y)) {
                            elements.add(new CompressedObjectParcelable(y, entry.date, entry.size, entry.directory));
                            strings.add(y);
                        }
                    } else {
                        if (y.startsWith(relativeDirectory + "/") && y.length() > relativeDirectory.length() + 1) {
                            String path1 = y.substring(relativeDirectory.length() + 1, y.length());

                            int index = relativeDirectory.length() + 1 + path1.indexOf("/");
                            String path = y.substring(0, index + 1);
                            if (!strings.contains(path)) {
                                CompressedObjectParcelable zipObj = new CompressedObjectParcelable(y.substring(0, index + 1), entry.date, entry.size, true);
                                strings.add(path);
                                elements.add(zipObj);
                            }
                        }
                    }

                }
            }

            Collections.sort(elements, new CompressedObjectParcelable.Sorter());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return elements;
    }

    @Override
    protected void onPostExecute(ArrayList<CompressedObjectParcelable> zipEntries) {
        super.onPostExecute(zipEntries);
        onFinish.onAsyncTaskFinished(zipEntries);
    }

}
