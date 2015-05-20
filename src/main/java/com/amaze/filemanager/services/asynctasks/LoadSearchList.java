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
import com.amaze.filemanager.adapters.Recycleradapter;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;

import java.io.File;
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

         System.out.println("async"+f.size());
        try {
            ma.list = addTo(f);

            Collections.sort(ma.list,
                    new FileListSorter(ma.dsort, ma.sortby, ma.asc,ma.rootMode));

            return ma.list;

        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }

    }
    public ArrayList<Layoutelements> addTo(ArrayList<String[]> mFile) {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        for (int i = 0; i < mFile.size(); i++) {
            String[] ele=mFile.get(i);
            File f=new File(ele[0]);
            String size="";
                if (ma.isDirectory(ele)) {
                    a.add(ma.utils.newElement(ma.folder, f.getPath(),mFile.get(i)[2],mFile.get(i)[1],size,true,false,""));

                } else {

                    try {
                        a.add(ma.utils.newElement(Icons.loadMimeIcon(ma.getActivity(), f.getPath(), !ma.islist), f.getPath(),mFile.get(i)[2],mFile.get(i)[1],size,false,false,""));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }}
            }
        return a;
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
                try {
                    ma.createViews(ma.list,false,new File(ma.current));
                    ((TextView)ma.pathbar.findViewById(R.id.pathname)).setText(ma.utils.getString(ma.getActivity(), R.string.searchresults));
                    ma.results = true;

                } catch (Exception e) {
                }
                ma.buttons.setVisibility(View.GONE);

            }
        } catch (Exception e) {
        }

    }
}
