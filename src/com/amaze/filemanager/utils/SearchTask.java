package com.amaze.filemanager.utils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.Main;

import java.io.File;
import java.util.ArrayList;

public class SearchTask extends AsyncTask<Bundle, String, ArrayList<File>> {
    ProgressDialog a;
    boolean run = true;
    MainActivity m;
    Main tab;

    public SearchTask(MainActivity m, Main tab) {
        this.m = m;
        this.tab = tab;
    }

    @Override
    public void onPreExecute() {
        a = new ProgressDialog(m);
        a.setIndeterminate(true);
        a.setTitle("Searching");
        a.setButton("Cancel", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                run = false;
                a.dismiss();
                // TODO: Implement this method
            }
        });
        a.setCancelable(false);
        a.show();

    }

    @Override
    public void onProgressUpdate(String... val) {
        if (a != null) {
            a.setMessage("Searching " + val[0]);
        }

    }

    protected ArrayList<File> doInBackground(Bundle[] p1) {
        Bundle b = p1[0];
        String FILENAME = b.getString("FILENAME");
        String FILEPATH = b.getString("FILEPATH");

        // TODO: Implement this method
        return getSearchResult(new File(FILEPATH), FILENAME);
    }

    @Override
    public void onPostExecute(ArrayList<File> c) {
        if (run) {
            tab.loadsearchlist(new Futils().toStringArray(c));
        }
        a.dismiss();
    }

    ArrayList<File> lis = new ArrayList<File>();

    public ArrayList<File> getSearchResult(File f, String text) {
        lis.clear();

        search(f, text);

        return lis;
    }


    public void search(File file, String text) {
        if (file.isDirectory()) {


            File[] f = file.listFiles();
            // do you have permission to read this directory?
            if (file.canRead()) {
                for (File temp : f) {
                    publishProgress(temp.getPath());
                    if (run) {
                        if (temp.isDirectory()) {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase())) {
                                lis.add(temp);
                            }
                            //System.out
                            //.println(file.getAbsoluteFile() );

                            search(temp, text);

                        } else {
                            if (temp.getName().toLowerCase()
                                    .contains(text.toLowerCase())) {
                                lis.add(temp);
                            }
                        }//	publishProgress(temp.getPath());
                    }
                }
            } else {
                System.out
                        .println(file.getAbsoluteFile() + "Permission Denied");
            }
        }
    }
}
