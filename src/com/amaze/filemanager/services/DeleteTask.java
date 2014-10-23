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

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MediaFile;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DeleteTask extends AsyncTask<ArrayList<File>, String, Boolean> {


    ArrayList<File> files;
    ContentResolver contentResolver;
    Main m;
    Futils utils = new Futils();
public  DeleteTask(ContentResolver c,Main m){this.contentResolver=c;this.m=m;}

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Toast.makeText(m.getActivity(),values[0],Toast.LENGTH_LONG).show();
    }

    protected Boolean doInBackground(ArrayList<File>... p1) {
            files=p1[0];
            if(files.get(0).getParentFile().canWrite()) {
                boolean b = true;
                for (int i = 0; i < files.size(); i++) {
                    boolean c = utils.deletefiles(files.get(i));
                    if (!c) {
                        b = false;
                    }

                }return b;
            }
            else if(m.rootMode){for(File f:files){
                RootTools.deleteFileOrDirectory(f.getPath(), true);}
              return true;
            }else{boolean b = true;
                for(File f:files){
                    MediaFile mediaFile=new MediaFile(contentResolver,f);
                    try {
                        boolean c=mediaFile.delete();
                        if(!c){b=false;}
                    } catch (IOException e) {
                        b=false;
                      publishProgress("Error");
                    }
                }
                return b;
            }

        }

        @Override
        public void onPostExecute(Boolean b) {
            m.updateList();
            if(!b){Toast.makeText(m.getActivity(),"Error",Toast.LENGTH_LONG).show();}
              else  Toast.makeText(m.getActivity(),"Done",Toast.LENGTH_LONG).show();
        }
    }


