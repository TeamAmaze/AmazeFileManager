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

package com.amaze.filemanager.ui.fragments;

import com.amaze.filemanager.asynchronous.asynctasks.SearchAsyncTask;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Worker fragment designed to not be destroyed when the activity holding it is recreated (aka the
 * state changes like screen rotation) thus maintaining alive an AsyncTask (SearchTask in this case)
 *
 * <p>Created by vishal on 26/2/16 edited by EmmanuelMess.
 */
public class SearchWorkerFragment extends Fragment {

  public static final String KEY_PATH = "path";
  public static final String KEY_INPUT = "input";
  public static final String KEY_OPEN_MODE = "open_mode";
  public static final String KEY_ROOT_MODE = "root_mode";
  public static final String KEY_REGEX = "regex";
  public static final String KEY_REGEX_MATCHES = "matches";

  public SearchAsyncTask searchAsyncTask;

  private HelperCallbacks callbacks;

  // interface for activity to communicate with asynctask
  public interface HelperCallbacks {
    void onPreExecute(String query);

    void onPostExecute(String query);

    void onProgressUpdate(HybridFileParcelable val, String query);

    void onCancelled();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    // hold instance of activity as there is a change in device configuration
    callbacks = (HelperCallbacks) context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
    String path = getArguments().getString(KEY_PATH);
    String input = getArguments().getString(KEY_INPUT);
    OpenMode openMode = OpenMode.getOpenMode(getArguments().getInt(KEY_OPEN_MODE));
    boolean rootMode = getArguments().getBoolean(KEY_ROOT_MODE);
    boolean isRegexEnabled = getArguments().getBoolean(KEY_REGEX);
    boolean isMatchesEnabled = getArguments().getBoolean(KEY_REGEX_MATCHES);

    searchAsyncTask =
        new SearchAsyncTask(
            getActivity(), input, openMode, rootMode, isRegexEnabled, isMatchesEnabled);
    searchAsyncTask.setCallback(callbacks);
    searchAsyncTask.execute(path);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    searchAsyncTask.setCallback(callbacks);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    // to avoid activity instance leak while changing activity configurations
    callbacks = null;
  }
}
