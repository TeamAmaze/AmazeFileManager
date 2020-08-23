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

package com.amaze.filemanager.asynchronous.asynctasks;

import java.util.ArrayList;

import com.amaze.filemanager.ui.fragments.DbViewerFragment;
import com.amaze.filemanager.ui.theme.AppTheme;

import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;
import android.webkit.WebView;

/** Created by Vishal on 20-03-2015. */
public class DbViewerTask extends AsyncTask<Void, Integer, Void> {

  Cursor schemaCursor, contentCursor;
  ArrayList<String> schemaList;
  ArrayList<String[]> contentList;
  DbViewerFragment dbViewerFragment;
  StringBuilder stringBuilder;
  WebView webView;
  String htmlInit;

  public DbViewerTask(
      Cursor schemaCursor,
      Cursor contentCursor,
      WebView webView,
      DbViewerFragment dbViewerFragment) {
    this.schemaCursor = schemaCursor;
    this.contentCursor = contentCursor;
    this.webView = webView;
    this.dbViewerFragment = dbViewerFragment;
    stringBuilder = new StringBuilder();

    this.webView.getSettings().setDefaultTextEncodingName("utf-8");
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();

    if (dbViewerFragment.databaseViewerActivity.getAppTheme().equals(AppTheme.DARK)
        || dbViewerFragment.databaseViewerActivity.getAppTheme().equals(AppTheme.BLACK)) {

      htmlInit = "<html><body><table border='1' style='width:100%;color:#ffffff'>";
    } else {

      htmlInit = "<html><body><table border='1' style='width:100%;color:#000000'>";
    }
    stringBuilder.append(htmlInit);
    dbViewerFragment.loadingText.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    super.onProgressUpdate(values);

    dbViewerFragment.loadingText.setText(values[0] + " records loaded");
  }

  @Override
  protected Void doInBackground(Void... params) {
    schemaList = getDbTableSchema(schemaCursor);
    contentList = getDbTableDetails(contentCursor);
    return null;
  }

  @Override
  protected void onCancelled() {
    super.onCancelled();
    dbViewerFragment.getActivity().onBackPressed();
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);

    dbViewerFragment.loadingText.setVisibility(View.GONE);

    // init schema row
    stringBuilder.append("<tr>");
    for (String s : schemaList) {
      stringBuilder.append("<th>").append(s).append("</th>");
    }
    stringBuilder.append("</tr>");

    for (String[] strings : contentList) {
      // init content row
      stringBuilder.append("<tr>");
      for (int i = 0; i < strings.length; i++) {
        stringBuilder.append("<td>").append(strings[i]).append("</td>");
      }
      stringBuilder.append("</tr>");
    }
    stringBuilder.append("</table></body></html>");
    webView.loadData(stringBuilder.toString(), "text/html;charset=utf-8", "utf-8");
    webView.setVisibility(View.VISIBLE);
  }

  private ArrayList<String[]> getDbTableDetails(Cursor c) {
    ArrayList<String[]> result = new ArrayList<>();
    int j = 0;
    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
      if (!isCancelled()) {
        j++;
        publishProgress(j);
        String[] temp = new String[c.getColumnCount()];
        for (int i = 0; i < temp.length; i++) {
          int dataType = c.getType(i);
          switch (dataType) {
            case 0:
              // #FIELD_TYPE_NULL
              temp[i] = null;
              break;
            case 1:
              // #FIELD_TYPE_INTEGER
              temp[i] = String.valueOf(c.getInt(i));
              break;
            case 2:
              // #FIELD_TYPE_FLOAT
              temp[i] = String.valueOf(c.getFloat(i));
              break;
            case 3:
              // #FIELD_TYPE_STRING
              temp[i] = c.getString(i);
              break;
            case 4:
              // #FIELD_TYPE_BLOB
              /*byte[] blob = c.getBlob(i);
              blobString = new String(blob);*/
              temp[i] = "(BLOB)";
              break;
          }
        }
        result.add(temp);
      } else {
        break;
      }
    }
    return result;
  }

  private ArrayList<String> getDbTableSchema(Cursor c) {
    ArrayList<String> result = new ArrayList<>();
    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
      if (!isCancelled()) {

        result.add(c.getString(1));
      } else break;
    }
    return result;
  }
}
