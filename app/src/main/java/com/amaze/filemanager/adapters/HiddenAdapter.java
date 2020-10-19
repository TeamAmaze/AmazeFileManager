/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.adapters;

import java.io.File;
import java.util.ArrayList;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.holders.HiddenViewHolder;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.utils.DataUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This Adapter contains all logic related to showing the list of hidden files.
 *
 * <p>Created by Arpit on 16-11-2014 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>.
 *
 * @author Bowie Chen on 2019-10-26.
 * @see com.amaze.filemanager.adapters.holders.HiddenViewHolder
 */
public class HiddenAdapter extends RecyclerView.Adapter<HiddenViewHolder> {
  private static final String TAG = "HiddenAdapter";

  private SharedPreferences sharedPrefs;
  private MainFragment mainFragment;
  private Context context;
  private ArrayList<HybridFile> hiddenFiles;
  private MaterialDialog materialDialog;
  private boolean hide;
  private DataUtils dataUtils = DataUtils.getInstance();

  public HiddenAdapter(
      Context context,
      MainFragment mainFrag,
      SharedPreferences sharedPreferences,
      ArrayList<HybridFile> hiddenFiles,
      MaterialDialog materialDialog,
      boolean hide) {
    this.context = context;
    this.mainFragment = mainFrag;
    sharedPrefs = sharedPreferences;
    this.hiddenFiles = new ArrayList<>(hiddenFiles);
    this.hide = hide;
    this.materialDialog = materialDialog;
  }

  @Override
  @NonNull
  public HiddenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater mInflater =
        (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    View view = mInflater.inflate(R.layout.bookmarkrow, parent, false);

    return new HiddenViewHolder(view);
  }

  @Override
  @SuppressWarnings("unchecked") // suppress varargs warnings
  public void onBindViewHolder(HiddenViewHolder holder, int position) {
    HybridFile file = hiddenFiles.get(position);

    holder.textTitle.setText(file.getName(context));
    holder.textDescription.setText(file.getReadablePath(file.getPath()));

    if (hide) {
      holder.deleteButton.setVisibility(View.GONE);
    }

    holder.deleteButton.setOnClickListener(
        view -> {
          // if the user taps on the delete button, un-hide the file.
          // TODO: the "hide files" feature just hide files from view in Amaze and not create
          // .nomedia

          if (!file.isSmb() && file.isDirectory(context)) {
            HybridFileParcelable nomediaFile =
                new HybridFileParcelable(
                    hiddenFiles.get(position).getPath() + "/" + FileUtils.NOMEDIA_FILE);
            nomediaFile.setMode(OpenMode.FILE);

            ArrayList<HybridFileParcelable> filesToDelete = new ArrayList<>();
            filesToDelete.add(nomediaFile);

            DeleteTask task = new DeleteTask(context);
            task.execute(filesToDelete);
          }

          dataUtils.removeHiddenFile(hiddenFiles.get(position).getPath());
          hiddenFiles.remove(hiddenFiles.get(position));
          notifyDataSetChanged();
        });
    holder.row.setOnClickListener(
        view -> {
          // if the user taps on the hidden file, take the user there.
          materialDialog.dismiss();
          new Thread(
                  () -> {
                    FragmentActivity fragmentActivity = mainFragment.getActivity();
                    if (fragmentActivity == null) {
                      // nullity check
                      return;
                    }

                    if (file.isDirectory(context)) {
                      fragmentActivity.runOnUiThread(
                          () -> mainFragment.loadlist(file.getPath(), false, OpenMode.UNKNOWN));
                    } else if (!file.isSmb()) {
                      fragmentActivity.runOnUiThread(
                          () ->
                              FileUtils.openFile(
                                  new File(file.getPath()),
                                  (MainActivity) fragmentActivity,
                                  sharedPrefs));
                    } else {
                      Log.w(
                          TAG,
                          "User tapped on a directory but conditions not met; nothing is done.");
                    }
                  })
              .start();
        });
  }

  public void updateDialog(MaterialDialog dialog) {
    materialDialog = dialog;
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemCount() {
    return hiddenFiles.size();
  }
}
