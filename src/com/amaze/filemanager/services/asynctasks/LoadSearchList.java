/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
                    new FileListSorter(ma.dsort, ma.sortby, ma.asc,ma.rootMode));

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
                ((TextView)ma.pathbar.findViewById(R.id.pathname)).setText(ma.utils.getString(ma.getActivity(),R.string.searchresults));
                ma.adapter = new MyAdapter(ma.getActivity(), R.layout.rowlayout,
                        bitmap, ma);
                try {
                    //ListView lv = (ListView) ma.listView.findViewById(R.id.listView);
             if(ma.aBoolean)       ma.listView.setAdapter(ma.adapter);
                    else ma.gridView.setAdapter(ma.adapter);
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
