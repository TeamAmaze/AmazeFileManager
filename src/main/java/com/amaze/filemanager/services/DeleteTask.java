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
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.utils.FileUtil;
import com.amaze.filemanager.utils.Futils;

import com.stericson.RootTools.RootTools;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class DeleteTask extends AsyncTask<ArrayList<String>, String, Boolean> {


    ArrayList<String> files;
    ContentResolver contentResolver;
    Context cd;
    Futils utils = new Futils();
    boolean rootMode;
    ZipViewer zipViewer;

    public DeleteTask(ContentResolver c, Context cd) {
        this.cd = cd;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
    }

    public DeleteTask(ContentResolver c, Context cd, ZipViewer zipViewer) {
        this.cd = cd;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
        this.zipViewer = zipViewer;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Toast.makeText(cd, values[0], Toast.LENGTH_LONG).show();
    }

    protected Boolean doInBackground(ArrayList<String>... p1) {
        files = p1[0];
        boolean b = true;
        if(files.get(0).startsWith("smb:/")){
            for(String a:files)
                try {
                    new SmbFile(a).delete();
                } catch (SmbException e) {
                    b=false;
                } catch (MalformedURLException e) {
                b=false;
                }
        }
        int mode=checkFolder(new File(files.get(0)).getParentFile(),cd);
        if (mode==1) {
            for(String f:files) {
                try {

                    if(!FileUtil.deleteFile(new File(f),cd))b=false;
                } catch (Exception e) {
                    b = false;
                }
            }
            }
         if ((!b || mode==0 || mode ==2) && rootMode)
             for (String f : files) {
                 try {
                     b=RootTools.deleteFileOrDirectory(f, true);
                 } catch (Exception e) {
                     b = false;
                 }
            }

        return b;
    }
    private int checkFolder(final File folder,Context context) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(folder, context)) {
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }

            if (!FileUtil.isWritableNormalOrSaf(folder,context)) {
                return 2;
            }
            return 1;
        }
        else if (Build.VERSION.SDK_INT==19 && FileUtil.isOnExtSdCard(folder,context)) {
            // Assume that Kitkat workaround works
            return 1;
        }
        else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
            return 1;
        }
        else {
            return 0;
        }
    }

    @Override
    public void onPostExecute(Boolean b) {
        Intent intent = new Intent("loadlist");
        cd.sendBroadcast(intent);
        for(String  file:files)
        utils.scanFile(file, cd);
        if (!b) {
            Toast.makeText(cd, utils.getString(cd, R.string.error), Toast.LENGTH_SHORT).show();
        } else if (zipViewer==null) {
            Toast.makeText(cd, utils.getString(cd, R.string.done), Toast.LENGTH_SHORT).show();
        }
        if (zipViewer!=null) {
            zipViewer.files.clear();
        }
    }
}



