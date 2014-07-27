package com.amaze.filemanager.services.asynctasks;


import android.os.AsyncTask;
import android.widget.Toast;

import com.amaze.filemanager.fragments.Main;

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
        if(!b){
            Toast.makeText(ma.getActivity(),"Done with errors",Toast.LENGTH_LONG).show();}
    }
}
