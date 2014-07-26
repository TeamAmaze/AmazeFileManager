package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Layoutelements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class LoadList extends AsyncTask<File, String, ArrayList<Layoutelements>> {

    private File f;
    boolean back;
    Main ma;
    public LoadList(boolean back,Main ma) {
        this.back = back;
        this.ma=ma;
    }

    @Override
    protected void onPreExecute() {
        ma.history.addPath(ma.current);
    }

    @Override
    public void onProgressUpdate(String... message) {
        Crouton.makeText(ma.getActivity(), message[0], Style.ALERT).show();
    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<Layoutelements> doInBackground(File... params) {
        // params comes from the execute() call: params[0] is the url.

        f = params[0];
        ma.mFile.clear();
        try {
            if (ma.utils.canListFiles(f)) {
                ma.file = f.listFiles();
                ma.mFile.clear();
                for (File f:ma.file) {
                    ma.mFile.add(f);
                }
            } else {
                if(ma.rootMode)
               new LoadRootList(ma).execute(f.getPath());
                else publishProgress("Access is Denied");
            }

            Collections.sort(ma.mFile,
                    new FileListSorter(ma.dsort,ma.sortby, ma.asc));

            ma.list = ma.addTo(ma.mFile);


            return ma.list;

        } catch (Exception e) {
            return null;
        }

    }

    @Override
    // Once the image is downloaded, associates it to the imageView
    protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
        if (isCancelled()) {
            bitmap = null;

        }
        ma.createViews(bitmap, back, f);

    }
}
