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

package com.amaze.filemanager.services;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.RarViewer;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MediaFile;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DeleteTask extends AsyncTask<ArrayList<File>, String, Boolean> {


    ArrayList<File> files;
    ContentResolver contentResolver;
    Context cd;
    Futils utils = new Futils();
    boolean rootMode;
    ZipViewer zipViewer;
    RarViewer rarViewer;

    public DeleteTask(ContentResolver c, Context cd) {
        this.contentResolver = c;
        this.cd = cd;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
    }

    public DeleteTask(ContentResolver c, Context cd, ZipViewer zipViewer) {
        this.contentResolver = c;
        this.cd = cd;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
        this.zipViewer = zipViewer;
    }

    public DeleteTask(ContentResolver c, Context cd, RarViewer rarViewer) {
        this.contentResolver = c;
        this.cd = cd;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
        this.rarViewer = rarViewer;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Toast.makeText(cd, values[0], Toast.LENGTH_LONG).show();
    }

    protected Boolean doInBackground(ArrayList<File>... p1) {
        files = p1[0];
        boolean b = true;
        if (files.get(0).getParentFile().canWrite()) {

            for (int i = 0; i < files.size(); i++) {
                boolean c = utils.deletefiles(files.get(i));
                if (!c) {
                    b = false;
                }
            }
            if (!b && Build.VERSION.SDK_INT >= 19) {
                for (File f : files) {
                    MediaFile mediaFile = new MediaFile(cd, f);
                    try {
                        boolean delete = mediaFile.delete();
                        if (!delete) {
                            b = false;
                        }
                    } catch (IOException e) {
                        b = false;
                        publishProgress(utils.getString(cd, R.string.error));
                    }
                }
            } else if (!b && rootMode) for (File f : files) {
                b=RootTools.deleteFileOrDirectory(f.getPath(), true);
                return true;
            }
        } else if (rootMode) {
            for (File f : files) {
               b= RootTools.deleteFileOrDirectory(f.getPath(), true);
            }

            return b;


        }
        return b;
    }

    @Override
    public void onPostExecute(Boolean b) {
        Intent intent = new Intent("loadlist");
        cd.sendBroadcast(intent);
        for(File file:files)
        utils.scanFile(file.getPath(), cd);
        if (!b) {
            Toast.makeText(cd, utils.getString(cd, R.string.error), Toast.LENGTH_LONG).show();
        } else if (zipViewer==null && rarViewer ==null) {
            Toast.makeText(cd, utils.getString(cd, R.string.done), Toast.LENGTH_LONG).show();
        }
        if (zipViewer!=null) {
            zipViewer.files.clear();
        } else if (rarViewer!=null) {
            rarViewer.files.clear();
        }
    }
}



