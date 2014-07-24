package com.amaze.filemanager.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.ZipAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipViewer extends ListFragment
//under construction dont read
{
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        File f = new File("/storage/emulated/0/Download/bootanimation.zip");
        new LoadListTask().execute(f);
        Toast.makeText(getActivity(), "" + f.length(), Toast.LENGTH_SHORT).show();
    }

    class LoadListTask extends AsyncTask<File, Void, ArrayList<ZipEntry>> {

        private File f;

        public LoadListTask() {

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        // Actual download method, run in the task thread
        protected ArrayList<ZipEntry> doInBackground(File... params) {
            // params comes from the execute() call: params[0] is the url.
            ArrayList<ZipEntry> elements = new ArrayList<ZipEntry>();
            try {
                ZipFile zipfile = new ZipFile(params[0]);

                //  int fileCount = zipfile.size();
                for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    elements.add(entry);
                }
            } catch (IOException e) {
            }
            return elements;
        }

        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(ArrayList<ZipEntry> bitmap) {
            ZipAdapter z = new ZipAdapter(getActivity(), R.layout.simplerow, bitmap);
            setListAdapter(z);

        }

    }
}
