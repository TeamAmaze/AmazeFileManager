package com.amaze.filemanager.services.asynctasks;

import android.os.AsyncTask;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.FileListSorter;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class LoadRootList extends AsyncTask<String, Void, Void> {
    ArrayList<File> a = new ArrayList<File>();
    String c;
    boolean b;
    Main ma;

    public LoadRootList(Main ma) {
        this.ma = ma;
        b = RootTools.isAccessGiven();
    }

    @Override
    public void onProgressUpdate(Void... v) {
        if (b) {
            ma.mFile = a;

            ma.list = ma.addTo(ma.mFile);
            ma.createViews(ma.list, false, new File(c));

        }
    }

    @Override
    protected Void doInBackground(String... strings) {
        if (b) {
            final String path = strings[0];
            c = path;
            Command command = new Command(0, "ls " + path) {
                @Override
                public void commandOutput(int i, String s) {
                    File f = new File(path + "/" + s);
                    a.add(f);
                }


                @Override
                public void commandTerminated(int i, String s) {

                }

                @Override
                public void commandCompleted(int i, int i2) {

                    Collections.sort(a,
                            new FileListSorter(ma.dsort, ma.sortby, ma.asc));
                    publishProgress();
                }
            };
            try {
                RootTools.getShell(true).add(command);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return null;
    }


}
