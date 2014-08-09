package com.amaze.filemanager.services.asynctasks;


import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.MyAdapter;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Layoutelements;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class LoadSearchList extends AsyncTask<ArrayList<String>, Void, ArrayList<Layoutelements>> {

    private ArrayList<String> f;
    Main ma;

    public LoadSearchList(Main ma) {
        this.ma = ma;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<Layoutelements> doInBackground(ArrayList<String>... params) {
        // params comes from the execute() call: params[0] is the url.
        ma.sFile = new ArrayList<File>();
        f = params[0];
        for (int i = 0; i < f.size(); i++) {
            ma.sFile.add(new File(f.get(i)));
        }

        try {
            /*Collections.sort(ma.sFile,
                    new FileListSorter(ma.dsort, ma.sortby, ma.asc));

            ma.slist = ma.addTo(ma.sFile);
*/

            return ma.slist;

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
        try {
            if (bitmap != null) {
                ma.getActivity().getActionBar().setSubtitle(R.string.searchresults);
                ma.adapter = new MyAdapter(ma.getActivity(), R.layout.rowlayout,
                        bitmap, ma);
                try {
                    ma.setListAdapter(ma.adapter);
                    ma.results = true;
                    try {
                        Intent i = new Intent("updatepager");
                        LocalBroadcastManager.getInstance(ma.getActivity()).sendBroadcast(i);
                    } catch (Exception e) {

                        e.printStackTrace();
                    }

                } catch (Exception e) {
                }
                ma.buttons.setVisibility(View.GONE);

            }
        } catch (Exception e) {
        }

    }
}
