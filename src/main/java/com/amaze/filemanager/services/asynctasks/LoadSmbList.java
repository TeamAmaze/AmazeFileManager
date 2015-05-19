package com.amaze.filemanager.services.asynctasks;

/**
 * Created by Arpit on 19-05-2015.
 *//*
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

import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.RootHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import jcifs.smb.SmbFile;


public class LoadSmbList extends AsyncTask<SmbFile, String, ArrayList<Layoutelements>> {

    private SmbFile f;
    boolean back;
    Main ma;

    public LoadSmbList(boolean back, Main ma) {
        this.back = back;
        this.ma = ma;
    }

    @Override
    protected void onPreExecute() {
        ma.history.addPath(ma.current);
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(ma.getActivity(), message[0], Toast.LENGTH_LONG).show();
    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<Layoutelements> doInBackground(SmbFile... params) {
        // params comes from the execute() call: params[0] is the url.

        f = params[0];
        ma.list.clear();
        try {
            ma.addToSmb(f.listFiles());
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
        ma.createViews(bitmap, back, new File(f.getPath()));

        //ListView lv = (ListView) ma.listView.findViewById(R.id.listView);
        ma.listView.setVisibility(View.VISIBLE);
    }



}

