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
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.filesystem.HFile;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by arpitkh996 on 12-01-2016.
 */

public class CopyFileCheck extends AsyncTask<ArrayList<BaseFile>, String, ArrayList<BaseFile>> {
    Main ma;
    String path;
    Boolean move;
    ArrayList<BaseFile> ab, a, b, lol;
    int counter = 0;
    Futils utils;
    MainActivity mainActivity;
    Context con;
    boolean rootmode=false;
    int openMode=0;
    public CopyFileCheck(Main main, String path, Boolean move, MainActivity context,boolean rootMode) {
        this.ma = main;
        this.path = path;
        this.move = move;
        mainActivity=context;
        con=context;
        openMode=ma.openMode;
        utils=new Futils();
        this.rootmode=rootMode;
        a = new ArrayList<>();
        b = new ArrayList<>();
        lol = new ArrayList<>();
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(con, message[0], Toast.LENGTH_LONG).show();
    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<BaseFile> doInBackground(ArrayList<BaseFile>... params) {

        ab = params[0];
        long totalBytes = 0;

        for (int i = 0; i < params[0].size(); i++) {
            BaseFile f1=ab.get(i);

            if (f1.isDirectory()) {

                totalBytes = totalBytes + f1.folderSize();
            } else {

                totalBytes = totalBytes + f1.length();
            }
        }
        HFile f = new HFile(openMode,path);
        if (f.getUsableSpace() >= totalBytes) {

            for (BaseFile k1 : f.listFiles(rootmode)) {
                for (BaseFile j : ab) {

                    if (k1.getName().equals((j).getName())) {
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

                int mode = mainActivity.mainActivityHelper.checkFolder(new File(path), mainActivity);
                if (mode == 2) {
                    mainActivity.oparrayList = (ab);
                    mainActivity.operation = move ? DataUtils.MOVE : DataUtils.COPY;
                    mainActivity.oppathe = path;
                } else if (mode == 1 || mode == 0) {

                    if (!move) {

                        Intent intent = new Intent(con, CopyService.class);
                        intent.putParcelableArrayListExtra("FILE_PATHS",ab);
                        intent.putExtra("COPY_DIRECTORY", path);
                        intent.putExtra("MODE",openMode);
                        mainActivity.startService(intent);
                    } else {

                        new MoveFiles(ab, ma, ma.getActivity(),openMode).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);
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
            textView.setText(utils.getString(con, R.string.fileexist) + "\n" + a.get(counter).getName());
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
            if (ab.get(0).getParent().equals(path)) {
                View negative = y.getActionButton(DialogAction.NEGATIVE);
                negative.setEnabled(false);
            }
        }
    }

    @Override
    protected void onPostExecute(ArrayList<BaseFile> strings) {
        super.onPostExecute(strings);
        showDialog();
    }
}