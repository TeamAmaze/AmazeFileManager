package com.amaze.filemanager.services.asynctasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.BaseFile;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HFile;
import com.amaze.filemanager.utils.RootHelper;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;

/**
 * Created by chinmay on 6/9/2015.
 */
public class SearchTask extends AsyncTask<String, ArrayList<BaseFile>, Void> {
    ArrayList<String> searchHelper;
    ArrayList<String> lis = new ArrayList<>();
    Main main;
    String key;
    boolean check = false;

    public SearchTask(ArrayList<String> arrayList, Main main, String key) {
        searchHelper = arrayList;
        this.main = main;
        this.key = key;
    }
    @Override
    public void onPostExecute(Void c){
    main.onSearchCompleted();
    }
    @Override
    public void onProgressUpdate(ArrayList<BaseFile>... val) {
        if (!isCancelled()) {
                main.addSearchResult(val[0]);
        }
    }
boolean isPresentInList(String p){
    boolean add=false;
    if(lis.contains(p)){add=true;}
    return add;
}
    @Override
    protected Void doInBackground(String... params) {
        int i = 0;
        String path = params[0];
        ArrayList<BaseFile> arrayList=new ArrayList<>();
        while (!isCancelled() && i < searchHelper.size()) {
            if (searchHelper.get(i).contains(path)){
                HFile file=new HFile(searchHelper.get(i));
                if (file.getName().toLowerCase()
                        .contains(key.toLowerCase()) && !isPresentInList(searchHelper.get(i)) ) {
                    BaseFile baseFile=RootHelper.generateBaseFile(new File(file.getPath()), main.SHOW_HIDDEN);
                    if(baseFile!=null){
                        lis.add(baseFile.getPath());
                        arrayList.add(baseFile);
                    }
                }
            }
            i++;
        }
        publishProgress(arrayList);
        if(new HFile(path).isSmb())return null;
        getSearchResult(new HFile(path), key);
        if(arrayList1.size()>0)publishProgress(arrayList1);
        return null;
    }
    public void getSearchResult(HFile f, String text) {
        search(f, text);
    }
    ArrayList<BaseFile> arrayList1=new ArrayList<>();
    public void search(HFile file, String text) {
     if(arrayList1.size()>5){
         publishProgress(arrayList1);
         arrayList1=new ArrayList<>();
     }
        if (file.isDirectory()) {
            ArrayList<BaseFile> f = file.listFiles(main.ROOT_MODE);
            // do you have permission to read this directory?
            if (!isCancelled())
                for (BaseFile x : f) {
                    HFile temp = new HFile(x.getPath());
                    if (!isCancelled()) {
                        if (temp.isDirectory()) {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase()) && !isPresentInList(x.getPath())) {
                                lis.add(x.getPath());
                                arrayList1.add(x);
                            }
                            if (!isCancelled()) search(temp, text);

                        } else {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase())&& !isPresentInList(x.getPath())) {
                                lis.add(x.getPath());
                                arrayList1.add(x);
                            }
                        }
                    }
                }
        } else {
            System.out
                    .println(file.getPath() + "Permission Denied");
        }
    }
}
