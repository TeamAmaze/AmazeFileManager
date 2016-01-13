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
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.Futils;

import java.io.File;
import java.util.ArrayList;

public class MoveFiles extends AsyncTask<String,Void,Boolean> {
    ArrayList<BaseFile> files;
    Main ma;
    String path;
    Context context;
    int mode;
    public MoveFiles(ArrayList<BaseFile> files, Main ma, Context context,int mode){
        this.ma=ma;
        this.context=context;
        this.files=files;
        this.mode=mode;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        path=strings[0];
        boolean b=true;
        int i=0;
        if(files.size()==0)return true;
        if(mode!=0)return false;
        if(files.get(0).isSmb()){return false;}
        for(BaseFile f:files){
            File file=new File(path+"/"+f.getName());
            File file1=new File(f.getPath());
            if(!file1.renameTo(file)){b=false;}
            i++;
        }
        return b;
    }
    @Override
    public void onPostExecute(Boolean b){
        Futils futils=new Futils();
        if(b ){
            if(ma!=null)if(ma.CURRENT_PATH.equals(path))ma.updateList();
                for(BaseFile f:files) {
                    futils.scanFile(f.getPath(), context);
                    futils.scanFile(path + "/" + f.getName(), context);

                }
        }
        else if(!b){
            Intent intent = new Intent(context, CopyService.class);
            intent.putExtra("FILE_PATHS", (files));
            intent.putExtra("COPY_DIRECTORY", path);
            intent.putExtra("move",true);
            intent.putExtra("MODE",mode);
            context.startService(intent);}
    }
}
