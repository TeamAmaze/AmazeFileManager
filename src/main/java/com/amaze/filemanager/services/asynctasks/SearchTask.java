package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;

import java.util.ArrayList;

/**
 * Created by chinmay on 6/9/2015.
 */
public class SearchTask extends AsyncTask<String, ArrayList<BaseFile>, Void> {
    ArrayList<BaseFile> searchHelper;
    ArrayList<BaseFile> lis = new ArrayList<>();
    Main main;
    String key;
    boolean check = false;

    public SearchTask(ArrayList<BaseFile> arrayList, Main main, String key) {
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
boolean isPresentInList(BaseFile p){
    boolean add=false;
    for(BaseFile baseFile:lis)
    if(baseFile.getPath().equals(p)){add=true;}
    return add;
}
    @Override
    protected Void doInBackground(String... params) {
        int i = 0;
        String path = params[0];
        ArrayList<BaseFile> arrayList=new ArrayList<>();
        while (!isCancelled() && i < searchHelper.size()) {
            if (searchHelper.get(i).getPath().contains(path)){
                BaseFile file=searchHelper.get(i);
                if (file.getName().toLowerCase().contains(key.toLowerCase()) && !isPresentInList(searchHelper.get(i)) ) {
                        lis.add(file);
                        arrayList.add(file);
                }
            }
            i++;
        }
        publishProgress(arrayList);
        HFile file=new HFile(main.openMode,path);
        file.generateMode(main.getActivity());
        if(file.isSmb())return null;
        getSearchResult(file, key);
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
                    HFile temp = new HFile(x.getMode(),x.getPath());
                    if (!isCancelled()) {
                        if (temp.isDirectory()) {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase()) && !isPresentInList(x)) {
                                lis.add(x);
                                arrayList1.add(x);
                            }
                            if (!isCancelled()) search(temp, text);

                        } else {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase())&& !isPresentInList(x)) {
                                lis.add(x);
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
