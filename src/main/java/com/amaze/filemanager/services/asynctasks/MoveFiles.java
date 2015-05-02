/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>
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


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.Futils;

import java.io.File;
import java.util.ArrayList;

public class MoveFiles extends AsyncTask<String,Void,Boolean> {
    ArrayList<File> files;
    Main ma;
    String path;
    Context context;
    public MoveFiles(ArrayList<File> files,Main ma,Context context){
        this.ma=ma;
        this.context=context;
        this.files=files;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        path=strings[0];
        boolean b=true;
        for(File f:files){
            File file=new File(path+"/"+f.getName());
            if(!f.renameTo(file)){b=false;}
        }
        return b;
    }
    @Override
    public void onPostExecute(Boolean b){
        Futils futils=new Futils();
        if(b ){if(ma!=null)if(ma.current.equals(path))ma.updateList();
            try {    for(File f:files){
               futils.scanFile(f.getPath(),context);
                futils.scanFile(path+"/"+f.getName(),context);
            }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(!b){
            Intent intent = new Intent(context, CopyService.class);
            intent.putExtra("FILE_PATHS", new Futils().toStringArray(files));
            intent.putExtra("COPY_DIRECTORY", path);
            intent.putExtra("move",true);
            context.startService(intent);}
    }
}
