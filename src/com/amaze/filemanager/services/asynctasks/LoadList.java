package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;
import android.view.View;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.RootHelper;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class LoadList extends AsyncTask<File, String, ArrayList<Layoutelements>> {

    private File f;
    boolean back;
    Main ma;

    public LoadList(boolean back, Main ma) {
        this.back = back;
        this.ma = ma;
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
        try {


            ma.list = ma.addTo(RootHelper.getFilesList(f.getPath(),ma.rootMode,ma.showHidden));

            Collections.sort(ma.list,
                    new FileListSorter(ma.dsort, ma.sortby, ma.asc));

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

        ma.getListView().setVisibility(View.VISIBLE);
    }



}
