package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by arpitkh996 on 12-01-2016.
 */

public class CopyFileCheck extends AsyncTask<ArrayList<BaseFile>, String, ArrayList<BaseFile>> {

    private Main main;
    private String path;
    private Boolean move;
    private ArrayList<BaseFile> filesToCopy, conflictingFiles;
    private int counter = 0;
    private MainActivity mainActivity;
    private Context context;
    private boolean rootMode = false;
    private OpenMode openMode = OpenMode.FILE;

    public CopyFileCheck(Main ma, String path, Boolean move, MainActivity con, boolean rootMode) {
        main = ma;
        this.path = path;
        this.move = move;
        mainActivity = con;
        context = con;
        openMode = main.openMode;
        this.rootMode = rootMode;
        conflictingFiles = new ArrayList<>();
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(context, message[0], Toast.LENGTH_LONG).show();
    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<BaseFile> doInBackground(ArrayList<BaseFile>... params) {
        filesToCopy = params[0];
        long totalBytes = 0;

        for (int i = 0; i < params[0].size(); i++) {
            BaseFile f1 = filesToCopy.get(i);

            if (f1.isDirectory()) {

                totalBytes = totalBytes + f1.folderSize();
            } else {

                totalBytes = totalBytes + f1.length();
            }
        }
        HFile f = new HFile(openMode, path);
        if (f.getUsableSpace() >= totalBytes) {
            for (BaseFile k1 : f.listFiles(rootMode)) {
                for (BaseFile j : filesToCopy) {
                    if (k1.getName().equals((j).getName())) {
                        conflictingFiles.add(j);
                    }
                }
            }
        } else publishProgress(context.getResources().getString(R.string.in_safe));

        return conflictingFiles;
    }

    @Override
    protected void onPostExecute(ArrayList<BaseFile> strings) {
        super.onPostExecute(strings);
        showDialog();
    }

    private void showDialog() {
        if (counter == conflictingFiles.size() || conflictingFiles.size() == 0) {
            if (filesToCopy != null && filesToCopy.size() != 0) {
                int mode = mainActivity.mainActivityHelper.checkFolder(new File(path), context);
                if (mode == MainActivityHelper.CAN_CREATE_FILES) {
                    mainActivity.oparrayList = filesToCopy;
                    mainActivity.operation = move ? DataUtils.MOVE : DataUtils.COPY;
                    mainActivity.oppathe = path;
                } else if (mode == MainActivityHelper.WRITABLE_OR_ON_SDCARD
                        || mode == MainActivityHelper.DOESNT_EXIST) {
                    if (!move) {
                        Intent intent = new Intent(context, CopyService.class);
                        intent.putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, filesToCopy);
                        intent.putExtra(CopyService.TAG_COPY_TARGET, path);
                        intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, openMode.ordinal());
                        ServiceWatcherUtil.runService(context, intent);
                    } else {
                        new MoveFiles(filesToCopy, main, main.getActivity(), openMode)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);
                    }
                }
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.no_file_overwrite), Toast.LENGTH_SHORT).show();
            }
        } else {
            final MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(context);
            LayoutInflater layoutInflater =
                    (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.copy_dialog, null);
            dialogBuilder.customView(view, true);
            // textView
            TextView textView = (TextView) view.findViewById(R.id.textView);
            textView.setText(context.getResources().getString(R.string.fileexist) + "\n" + conflictingFiles.get(counter).getName());
            // checkBox
            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            Futils.setTint(checkBox, Color.parseColor(BaseActivity.accentSkin));
            dialogBuilder.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
            dialogBuilder.title(context.getResources().getString(R.string.paste));
            dialogBuilder.positiveText(R.string.skip);
            dialogBuilder.negativeText(R.string.overwrite);
            dialogBuilder.neutralText(R.string.cancel);
            dialogBuilder.positiveColor(Color.parseColor(BaseActivity.accentSkin));
            dialogBuilder.negativeColor(Color.parseColor(BaseActivity.accentSkin));
            dialogBuilder.neutralColor(Color.parseColor(BaseActivity.accentSkin));
            dialogBuilder.onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (counter < conflictingFiles.size()) {
                        if (!checkBox.isChecked()) {
                            filesToCopy.remove(conflictingFiles.get(counter));
                            counter++;
                        } else {
                            for (int j = counter; j < conflictingFiles.size(); j++) {
                                filesToCopy.remove(conflictingFiles.get(j));
                            }
                            counter = conflictingFiles.size();
                        }
                        showDialog();
                    }
                }
            });
            dialogBuilder.onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (counter < conflictingFiles.size()) {
                        if (!checkBox.isChecked()) {
                            counter++;
                        } else {
                            counter = conflictingFiles.size();
                        }
                        showDialog();
                    }
                }
            });

            final MaterialDialog dialog = dialogBuilder.build();
            dialog.show();
            if (filesToCopy.get(0).getParent().equals(path)) {
                View negative = dialog.getActionButton(DialogAction.NEGATIVE);
                negative.setEnabled(false);
            }
        }
    }
}