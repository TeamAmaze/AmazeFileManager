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

import java.util.concurrent.atomic.AtomicInteger;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.widget.TextView;

import androidx.core.util.Pair;

/** @author Emmanuel on 12/5/2017, at 19:40. */
public class CountItemsOrAndSizeTask extends AsyncTask<Void, Pair<Integer, Long>, String> {

  private Context context;
  private TextView itemsText;
  private HybridFileParcelable file;
  private boolean isStorage;

  public CountItemsOrAndSizeTask(
      Context c, TextView itemsText, HybridFileParcelable f, boolean storage) {
    this.context = c;
    this.itemsText = itemsText;
    file = f;
    isStorage = storage;
  }

  @Override
  protected String doInBackground(Void[] params) {
    String items = "";
    long fileLength = file.length(context);

    if (file.isDirectory(context)) {
      final AtomicInteger x = new AtomicInteger(0);
      file.forEachChildrenFile(context, false, file -> x.incrementAndGet());
      final int folderLength = x.intValue();
      long folderSize;

      if (isStorage) {
        folderSize = file.getUsableSpace();
      } else {
        folderSize =
            FileUtils.folderSize(file, data -> publishProgress(new Pair<>(folderLength, data)));
      }

      items = getText(folderLength, folderSize, false);
    } else {
      items =
          Formatter.formatFileSize(context, fileLength)
              + (" ("
                  + fileLength
                  + " "
                  + context
                      .getResources()
                      .getQuantityString(
                          R.plurals.bytes, (int) fileLength) // truncation is insignificant
                  + ")");
    }

    return items;
  }

  @Override
  protected void onProgressUpdate(Pair<Integer, Long>[] dataArr) {
    Pair<Integer, Long> data = dataArr[0];

    itemsText.setText(getText(data.first, data.second, true));
  }

  private String getText(int filesInFolder, long length, boolean loading) {
    String numOfItems =
        (filesInFolder != 0 ? filesInFolder + " " : "")
            + context.getResources().getQuantityString(R.plurals.items, filesInFolder);

    return numOfItems + "; " + (loading ? ">" : "") + Formatter.formatFileSize(context, length);
  }

  protected void onPostExecute(String items) {
    itemsText.setText(items);
  }
}
