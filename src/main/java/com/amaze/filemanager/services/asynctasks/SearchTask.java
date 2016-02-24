package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;

import java.util.ArrayList;

/**
 * Created by chinmay on 6/9/2015.
 */
public class SearchTask extends AsyncTask<String, BaseFile, Void> {
    ArrayList<BaseFile> searchHelper;
    Main main;
    String key;

    public SearchTask(ArrayList<BaseFile> arrayList, Main main, String key) {
        searchHelper = arrayList;
        this.main = main;
        this.key = key;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        main.mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected Void doInBackground(String... params) {

        String path = params[0];
        HFile file=new HFile(main.openMode,path);
        file.generateMode(main.getActivity());
        if(file.isSmb())return null;
        search(file, key);
        return null;
    }

    @Override
    public void onPostExecute(Void c){
        main.onSearchCompleted();
        main.mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onProgressUpdate(BaseFile... val) {
        if (!isCancelled()) {
            main.addSearchResult(val[0]);
        }
    }

    public void search(HFile file, String text) {

        if (file.isDirectory()) {
            ArrayList<BaseFile> f = file.listFiles(main.ROOT_MODE);
            // do you have permission to read this directory?
            if (!isCancelled())
                for (BaseFile x : f) {
                    if (!isCancelled()) {
                        if (x.isDirectory()) {
                            if (x.getName().toLowerCase()
                                    .contains(text.toLowerCase())) {
                                publishProgress(x);
                            }
                            if (!isCancelled()) search(x, text);

                        } else {
                            if (x.getName().toLowerCase()
                                    .contains(text.toLowerCase())) {
                                publishProgress(x);
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
