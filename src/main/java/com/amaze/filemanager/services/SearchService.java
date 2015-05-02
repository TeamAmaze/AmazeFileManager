package com.amaze.filemanager.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.amaze.filemanager.utils.RootHelper;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Arpit on 03-01-2015.
 */
public class SearchService extends IntentService {
boolean rootMode,showHidden;
    SharedPreferences Sp;
    public SearchService() {
        super("SearchService");

    }
    public void publishProgress(int l){
        Intent i=new Intent("searchresults");
        i.putExtra("paths",l);
        if (run)LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
    public void publishProgress(ArrayList<String[]> a){
        Intent i=new Intent("loadsearchresults");
       ArrayList<String> b=new ArrayList<String>();
        ArrayList<String> c=new ArrayList<String>();
        ArrayList<String> d=new ArrayList<String>();
        ArrayList<String> f=new ArrayList<String>();
        for(String[] e:a){
            b.add(e[0]);
            c.add(e[1]);
            d.add(e[2]);
            f.add(e[3]);
        }
        i.putExtra("b",b);
        i.putExtra("c",c);
        i.putExtra("d",d);
        i.putExtra("f",f);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Sp= PreferenceManager.getDefaultSharedPreferences(this);
        rootMode = Sp.getBoolean("rootmode", false);
        showHidden=Sp.getBoolean("showHidden",false);
        LocalBroadcastManager.getInstance(this).registerReceiver(RECIEVER,new IntentFilter("searchcancel"));
        publishProgress(0);
        ArrayList<String[]> arrayList=getSearchResult(new File(intent.getStringExtra("path")), intent.getStringExtra("text"));
        publishProgress(arrayList);
        stopSelf();
    }
    @Override
            public void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(this)
.unregisterReceiver(RECIEVER);    }
    ArrayList<String[]> lis = new ArrayList<String[]>();
    boolean run=true;
    public ArrayList<String[]> getSearchResult(File f, String text) {
        lis.clear();
        search(f, text);
        return lis;
    }

    private BroadcastReceiver RECIEVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Cancelled");
            run=false;
        }
    };
    public void search(File file, String text) {
        if (file.isDirectory()) {
              ArrayList<String[]> f= RootHelper.getFilesList(file.getPath(), rootMode, showHidden, false);
            // do you have permission to read this directory?
            if(run)
            for (String[] x : f) {
                File temp=new File(x[0]);
                if (run) {
                    if (temp.isDirectory()) {
                        if (temp.getName().toLowerCase()
                                .contains(text.toLowerCase())) {
                            lis.add(x);
                        publishProgress(lis.size());
                        }
                  if(run)      search(temp, text);

                    } else {
                        if (temp.getName().toLowerCase()
                                .contains(text.toLowerCase())) {
                            lis.add(x);
                            publishProgress(lis.size());
                        }
                    }}
            }
        } else {
            System.out
                    .println(file.getAbsoluteFile() + "Permission Denied");
        }
    }
}
