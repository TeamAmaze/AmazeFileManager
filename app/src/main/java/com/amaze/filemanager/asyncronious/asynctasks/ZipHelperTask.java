package com.amaze.filemanager.asyncronious.asynctasks;

import android.net.Uri;
import android.os.AsyncTask;

import com.amaze.filemanager.fragments.ZipExplorerFragment;
import com.amaze.filemanager.ui.ZipObj;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by Vishal on 11/23/2014.
 */
public class ZipHelperTask extends AsyncTask<String, Void, ArrayList<ZipObj>> {

    ZipExplorerFragment zipExplorerFragment;
    String dir;

    /**
     * AsyncTask to load ZIP file items.
     * @param zipExplorerFragment the zipExplorerFragment fragment instance
     * @param dir
     */
    public ZipHelperTask(ZipExplorerFragment zipExplorerFragment, String dir) {
        this.zipExplorerFragment = zipExplorerFragment;
        this.dir = dir;
        zipExplorerFragment.swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        zipExplorerFragment.swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected ArrayList<ZipObj> doInBackground(String... params) {
        ArrayList<ZipObj> elements = new ArrayList<>();
        try {
            if (zipExplorerFragment.wholelist.size() == 0) {
                Uri uri = Uri.parse(params[0]);
                if (new File(uri.getPath()).canRead()) {
                    ZipFile zipfile = new ZipFile(uri.getPath());
                    for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                        ZipEntry entry = (ZipEntry) e.nextElement();
                        zipExplorerFragment.wholelist.add(new ZipObj(entry, entry.getTime(), entry.getSize(), entry.isDirectory()));
                    }
                } else {
                    ZipEntry entry1;
                    if (zipExplorerFragment.wholelist.size() == 0) {
                        ZipInputStream zipfile1 = new ZipInputStream(zipExplorerFragment.getActivity().getContentResolver().openInputStream(uri));
                        while ((entry1 = zipfile1.getNextEntry()) != null) {
                            zipExplorerFragment.wholelist.add(new ZipObj(entry1, entry1.getTime(), entry1.getSize(), entry1.isDirectory()));
                        }
                    }
                }
            }
            ArrayList<String> strings = new ArrayList<>();
            //  int fileCount = zipfile.size();

            for (ZipObj entry : zipExplorerFragment.wholelist) {

                String s = entry.getName();
                //  System.out.println(s);
                File file = new File(entry.getName());
                if (dir == null || dir.trim().length() == 0) {
                    String y = entry.getName();
                    if (y.startsWith("/"))
                        y = y.substring(1, y.length());
                    if (file.getParent() == null || file.getParent().length() == 0 || file.getParent().equals("/")) {
                        if (!strings.contains(y)) {
                            elements.add(new ZipObj(new ZipEntry(y), entry.getTime(), entry.getSize(), entry.isDirectory()));
                            strings.add(y);
                        }
                    } else {
                        String path = y.substring(0, y.indexOf("/") + 1);
                        if (!strings.contains(path)) {
                            ZipObj zipObj = new ZipObj(new ZipEntry(path), entry.getTime(), entry.getSize(), true);
                            strings.add(path);
                            elements.add(zipObj);
                        }

                    }
                } else {
                    String y = entry.getName();
                    if (entry.getName().startsWith("/"))
                        y = y.substring(1, y.length());

                    if (file.getParent() != null && (file.getParent().equals(dir) || file.getParent().equals("/" + dir))) {
                        if (!strings.contains(y)) {
                            elements.add(new ZipObj(new ZipEntry(y), entry.getTime(), entry.getSize(), entry.isDirectory()));
                            strings.add(y);
                        }
                    } else {
                        if (y.startsWith(dir + "/") && y.length() > dir.length() + 1) {
                            String path1 = y.substring(dir.length() + 1, y.length());

                            int index = dir.length() + 1 + path1.indexOf("/");
                            String path = y.substring(0, index + 1);
                            if (!strings.contains(path)) {
                                ZipObj zipObj = new ZipObj(new ZipEntry(y.substring(0, index + 1)), entry.getTime(), entry.getSize(), true);
                                strings.add(path);
                                //System.out.println(path);
                                elements.add(zipObj);
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(elements, new FileListSorter());
        if (zipExplorerFragment.gobackitem && dir != null && dir.trim().length() != 0)
            elements.add(0, new ZipObj(null, 0, 0, true));
        zipExplorerFragment.elements = elements;
        return elements;
    }

    @Override
    protected void onPostExecute(ArrayList<ZipObj> zipEntries) {
        super.onPostExecute(zipEntries);
        zipExplorerFragment.swipeRefreshLayout.setRefreshing(false);
        zipExplorerFragment.createZipViews(zipEntries, dir);
    }

    private class FileListSorter implements Comparator<ZipObj> {
        @Override
        public int compare(ZipObj file1, ZipObj file2) {
            if (file1.isDirectory() && !file2.isDirectory()) {
                return -1;


            } else if (file2.isDirectory() && !(file1).isDirectory()) {
                return 1;
            }
            return file1.getEntry().getName().compareToIgnoreCase(file2.getEntry().getName());
        }

    }

}
