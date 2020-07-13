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

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.asynctasks.DbViewerTask;
import com.amaze.filemanager.ui.activities.DatabaseViewerActivity;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.Utils;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Created by Vishal on 06-02-2015. */
public class DbViewerFragment extends Fragment {
  public DatabaseViewerActivity databaseViewerActivity;
  private String tableName;
  private View rootView;
  private Cursor schemaCursor, contentCursor;
  private RelativeLayout relativeLayout;
  public TextView loadingText;
  private WebView webView;

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    databaseViewerActivity = (DatabaseViewerActivity) getActivity();

    rootView = inflater.inflate(R.layout.fragment_db_viewer, null);
    webView = rootView.findViewById(R.id.webView1);
    loadingText = rootView.findViewById(R.id.loadingText);
    relativeLayout = rootView.findViewById(R.id.tableLayout);
    tableName = getArguments().getString("table");
    databaseViewerActivity.setTitle(tableName);

    schemaCursor =
        databaseViewerActivity.sqLiteDatabase.rawQuery(
            "PRAGMA table_info(" + tableName + ");", null);
    contentCursor =
        databaseViewerActivity.sqLiteDatabase.rawQuery("SELECT * FROM " + tableName, null);

    new DbViewerTask(schemaCursor, contentCursor, webView, this).execute();

    return rootView;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (databaseViewerActivity.getAppTheme().equals(AppTheme.DARK)) {
      relativeLayout.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
      webView.setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
    } else if (databaseViewerActivity.getAppTheme().equals(AppTheme.BLACK)) {
      relativeLayout.setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
      webView.setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
    } else {
      relativeLayout.setBackgroundColor(Color.parseColor("#ffffff"));
      webView.setBackgroundColor(Color.parseColor("#ffffff"));
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    schemaCursor.close();
    contentCursor.close();
  }
}
