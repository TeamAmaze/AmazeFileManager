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


import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.CopyService;

import java.io.File;
import java.util.ArrayList;

public class MoveFiles extends AsyncTask<String,Void,Boolean> {
    ArrayList<File> files;
    Main ma;
    String path;
    public MoveFiles(ArrayList<File> files,Main ma){
        this.ma=ma;
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
        if(b && ma.current.equals(path)){ma.updateList();}
        if(!b){Intent intent = new Intent(ma.getActivity(), CopyService.class);
            intent.putExtra("FILE_PATHS", files);
            intent.putExtra("COPY_DIRECTORY", path);
            ma.getActivity().startService(intent);}
    }
}
