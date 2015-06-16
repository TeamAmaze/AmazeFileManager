package com.amaze.filemanager.services.asynctasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.RootHelper;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by chinmay on 6/9/2015.
 */
public class SearchTask extends AsyncTask<String, String[], Void> {
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
    public void onProgressUpdate(String[]... val) {
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
        while (!isCancelled() && i < searchHelper.size()) {
            if (searchHelper.get(0).contains(path))
                if (new File(searchHelper.get(i)).getName().toLowerCase()
                        .contains(key.toLowerCase()) && !isPresentInList(searchHelper.get(i)) ) {
                    File x = new File(searchHelper.get(i));
                    String k = "", size = "";
                    if (x.isDirectory()) {
                        k = "-1";
                        if (main.showSize) size = "" + RootHelper.getCount(x);
                    } else if (main.showSize) size = "" + x.length();
                    String[] string = new String[]{x.getPath(), "", RootHelper.parseFilePermission(x), k, x.lastModified() + "", size};
                    lis.add(string[0]);
                    publishProgress(string);


                }
            i++;

        }
        getSearchResult(new File(path), key);
        return null;
    }
    public void getSearchResult(File f, String text) {
        search(f, text);
    }

    public void search(File file, String text) {
        if (file.isDirectory()) {
            ArrayList<String[]> f = RootHelper.getFilesList(file.getPath(), main.rootMode, main.showHidden, false);
            // do you have permission to read this directory?
            if (!isCancelled())
                for (String[] x : f) {
                    File temp = new File(x[0]);
                    if (!isCancelled()) {
                        if (temp.isDirectory()) {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase()) && !isPresentInList(x[0])) {
                                lis.add(x[0]);
                                publishProgress(x);
                            }
                            if (!isCancelled()) search(temp, text);

                        } else {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase())&& !isPresentInList(x[0])) {
                                lis.add(x[0]);
                                publishProgress(x);
                            }
                        }
                    }
                }
        } else {
            System.out
                    .println(file.getAbsoluteFile() + "Permission Denied");
        }
    }
}
