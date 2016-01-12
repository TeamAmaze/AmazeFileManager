package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.BaseFile;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HFile;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by arpitkh996 on 12-01-2016.
 */

public class CopyFileCheck extends AsyncTask<ArrayList<String>, String, ArrayList<String>> {
    Main ma;
    String path;
    Boolean move;
    ArrayList<String> ab, a, b, lol,names;
    int counter = 0;
    Futils utils;
    MainActivity mainActivity;
    Context con;
    boolean rootmode=false;
    public CopyFileCheck(Main main, String path, Boolean move, MainActivity context,boolean rootMode) {
        this.ma = main;
        this.path = path;
        this.move = move;
        mainActivity=context;
        con=context;
        utils=new Futils();
        this.rootmode=rootMode;
        a = new ArrayList<>();
        b = new ArrayList<>();
        lol = new ArrayList<>();
        names=new ArrayList<>();
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(con, message[0], Toast.LENGTH_LONG).show();
    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<String> doInBackground(ArrayList<String>... params) {

        ab = params[0];
        long totalBytes = 0;

        for (int i = 0; i < params[0].size(); i++) {

            HFile f1 = new HFile(params[0].get(i));

            if (f1.isDirectory()) {

                totalBytes = totalBytes + f1.folderSize();
            } else {

                totalBytes = totalBytes + f1.length();
            }
        }
        HFile f = new HFile(path);
        if (f.getUsableSpace() > totalBytes) {

            for (BaseFile k1 : f.listFiles(rootmode)) {
                HFile k = new HFile(k1.getPath());
                for (String j : ab) {

                    if (k.getName().equals(new HFile(j).getName())) {

                        a.add(j);
                    }
                }
            }
        } else publishProgress(utils.getString(con, R.string.in_safe));

        return a;
    }

    public void showDialog() {

        if (counter == a.size() || a.size() == 0) {

            if (ab != null && ab.size() != 0) {

                ArrayList<String> names=new ArrayList<>();
                for(String a:ab){
                    names.add(new HFile(a).getName());
                }
                int mode = mainActivity.mainActivityHelper.checkFolder(new File(path), mainActivity);
                if (mode == 2) {
                    mainActivity.oparrayList = (ab);
                    mainActivity.operation = move ? mainActivity.MOVE : mainActivity.COPY;
                    mainActivity.oppathe = path;
                    mainActivity.opnameList=names;
                } else if (mode == 1 || mode == 0) {

                    if (!move) {

                        Intent intent = new Intent(con, CopyService.class);
                        intent.putExtra("FILE_PATHS", ab);
                        intent.putExtra("COPY_DIRECTORY", path);
                        intent.putExtra("FILE_NAMES",names);
                        mainActivity.startService(intent);
                    } else {

                        new MoveFiles(utils.toFileArray(ab),names, ma, ma.getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);
                    }
                }
            } else {

                Toast.makeText(mainActivity, utils.getString(con, R.string.no_file_overwrite), Toast.LENGTH_SHORT).show();
            }
        } else {

            final MaterialDialog.Builder x = new MaterialDialog.Builder(mainActivity);
            LayoutInflater layoutInflater = (LayoutInflater) mainActivity.getSystemService(mainActivity.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.copy_dialog, null);
            x.customView(view, true);
            // textView
            TextView textView = (TextView) view.findViewById(R.id.textView);
            textView.setText(utils.getString(con, R.string.fileexist) + "\n" + new File(a.get(counter)).getName());
            // checkBox
            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            utils.setTint(checkBox, Color.parseColor(mainActivity.fabskin));
            if (mainActivity.theme1 == 1) x.theme(Theme.DARK);
            x.title(utils.getString(con, R.string.paste));
            x.positiveText(R.string.skip);
            x.negativeText(R.string.overwrite);
            x.neutralText(R.string.cancel);
            x.positiveColor(Color.parseColor(mainActivity.fabskin));
            x.negativeColor(Color.parseColor(mainActivity.fabskin));
            x.neutralColor(Color.parseColor(mainActivity.fabskin));
            x.callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog materialDialog) {

                    if (counter < a.size()) {

                        if (!checkBox.isChecked()) {

                            ab.remove(a.get(counter));
                            counter++;

                        } else {
                            for (int j = counter; j < a.size(); j++) {

                                ab.remove(a.get(j));
                            }
                            counter = a.size();
                        }
                        showDialog();
                    }
                }

                @Override
                public void onNegative(MaterialDialog materialDialog) {

                    if (counter < a.size()) {

                        if (!checkBox.isChecked()) {

                            counter++;
                        } else {

                            counter = a.size();
                        }
                        showDialog();
                    }

                }
            });
            final MaterialDialog y = x.build();
            y.show();
            if (new File(ab.get(0)).getParent().equals(path)) {
                View negative = y.getActionButton(DialogAction.NEGATIVE);
                negative.setEnabled(false);
            }
        }
    }

    @Override
    protected void onPostExecute(ArrayList<String> strings) {
        super.onPostExecute(strings);
        showDialog();
    }
}