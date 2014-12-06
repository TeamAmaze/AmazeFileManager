package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.adarshr.raroscope.RARFile;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.ZipAdapter;
import com.amaze.filemanager.fragments.ZipViewer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Vishal on 11/23/2014.
 */
public class ZipHelperTask extends AsyncTask<File, Void, ArrayList<ZipEntry>> {

    ZipViewer zipViewer;
    int counter;
    String dir;

    public ZipHelperTask(ZipViewer zipViewer, int counter, String dir) {

        this.zipViewer = zipViewer;
        this.counter = counter;
        this.dir = dir;
    }

    public ZipHelperTask(ZipViewer zipViewer, int counter) {
        this.zipViewer = zipViewer;
        this.counter = counter;
    }

    @Override
    protected ArrayList<ZipEntry> doInBackground(File... params) {
        ArrayList<ZipEntry> elements = new ArrayList<ZipEntry>();
        try {
            ZipFile zipfile = new ZipFile(params[0]);

            //  int fileCount = zipfile.size();
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String s = entry.getName().toString();
                File file = new File(entry.getName());
                if (counter==0) {

                    if (file.getParent() == null) {
                        elements.add(entry);
                        zipViewer.results = false;
                    }
                } else if (counter==1) {

                    Log.d("Test", dir);
                    if (file.getParent()!=null && file.getParent().equals(dir)) {
                        elements.add(entry);
                        zipViewer.results = true;
                    }
                } else if (counter==2) {
                    if (file.getParent()!=null && file.getParent().equals(dir)) {

                        elements.add(entry);
                        zipViewer.results = true;
                    } else if (file.getParent()==null) {
                        if (dir==null) {

                            elements.add(entry);
                            zipViewer.results = false;
                        }
                    }
                }
            }

        } catch (IOException e) {
        }
        return elements;
    }

    @Override
    protected void onPostExecute(ArrayList<ZipEntry> zipEntries) {
        super.onPostExecute(zipEntries);
        ZipAdapter z = new ZipAdapter(zipViewer.getActivity(), R.layout.simplerow, zipEntries, zipViewer);
        zipViewer.setListAdapter(z);       ((TextView) zipViewer.getActivity().findViewById(R.id.title)).setText(zipViewer.current);

    }
}
