package com.amaze.filemanager.services.asynctasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
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
public class SearchTask extends AsyncTask<String, ArrayList<String[]>, Void> {
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
    public void onProgressUpdate(ArrayList<String[]>... val) {
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
        ArrayList<String[]> arrayList=new ArrayList<>();
        while (!isCancelled() && i < searchHelper.size()) {
            if (searchHelper.get(i).contains(path)){
                HFile file=new HFile(searchHelper.get(i));
                if (file.getName().toLowerCase()
                        .contains(key.toLowerCase()) && !isPresentInList(searchHelper.get(i)) ) {
                    String k = "", size = "";
                    if (file.isDirectory()) {
                        k = "-1";
                        if (main.showSize) size = "";
                    } else if (main.showSize) size = "" + file.length();
                    String[] string = new String[0];
                    try {
                        string = new String[]{file.getPath(), "", "", k, file.lastModified() + "", size};
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (SmbException e) {
                        e.printStackTrace();
                    }
                    lis.add(string[0]);
                    arrayList.add(string);
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
    ArrayList<String[]> arrayList1=new ArrayList<>();
    public void search(HFile file, String text) {
     if(arrayList1.size()>5){
         publishProgress(arrayList1);
         arrayList1=new ArrayList<>();
     }
        if (file.isDirectory()) {
            ArrayList<String[]> f = file.listFiles(main.rootMode);
            // do you have permission to read this directory?
            if (!isCancelled())
                for (String[] x : f) {
                    HFile temp = new HFile(x[0]);
                    if (!isCancelled()) {
                        if (temp.isDirectory()) {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase()) && !isPresentInList(x[0])) {
                                lis.add(x[0]);
                                arrayList1.add(x);
                            }
                            if (!isCancelled()) search(temp, text);

                        } else {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase())&& !isPresentInList(x[0])) {
                                lis.add(x[0]);
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
