package com.amaze.filemanager.services.asynctasks;


import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.MyAdapter;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Layoutelements;

import java.util.ArrayList;
import java.util.Collections;

public class LoadSearchList extends AsyncTask<ArrayList<String[]>, Void, ArrayList<Layoutelements>> {

    private ArrayList<String[]> f;
    Main ma;

    public LoadSearchList(Main ma) {
        this.ma = ma;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<Layoutelements> doInBackground(ArrayList<String[]>... params) {
        // params comes from the execute() call: params[0] is the url.

        f = params[0];


        try {
            ma.slist = ma.addTo(f);
            Collections.sort(ma.slist,
                    new FileListSorter(ma.dsort, ma.sortby, ma.asc));

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
                ((TextView)ma.pathbar.findViewById(R.id.pathname)).setText("Search Results");
                ma.adapter = new MyAdapter(ma.getActivity(), R.layout.rowlayout,
                        bitmap, ma);
                try {
                    //ListView lv = (ListView) ma.listView.findViewById(R.id.listView);
                    ma.listView.setAdapter(ma.adapter);
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
