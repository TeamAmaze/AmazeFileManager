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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amaze.filemanager.utils.OnProgressUpdate;
import com.amaze.filemanager.ui.activities.texteditor.SearchResultIndex;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public class SearchTextTask extends AsyncTask<Void, SearchResultIndex, List<SearchResultIndex>> {

  private final String searchedText;
  private final String textToSearch;
  private final OnProgressUpdate<SearchResultIndex> updateListener;
  private final OnAsyncTaskFinished<List<SearchResultIndex>> listener;

  private final LineNumberReader lineNumberReader;

  public SearchTextTask(@NonNull String textToSearch, @NonNull String searchedText,
                        @NonNull OnProgressUpdate<SearchResultIndex> updateListener,
                        @NonNull OnAsyncTaskFinished<List<SearchResultIndex>> listener) {
    this.searchedText = searchedText;
    this.textToSearch = textToSearch;
    this.updateListener = updateListener;
    this.listener = listener;

    StringReader stringReader = new StringReader(textToSearch);
    lineNumberReader = new LineNumberReader(stringReader);
  }

  @Override
  protected List<SearchResultIndex> doInBackground(Void... params) {
    if (TextUtils.isEmpty(searchedText)) {
      return Collections.emptyList();
    }

    final ArrayList<SearchResultIndex> searchResultIndices = new ArrayList<>();

    for (int charIndex = 0; charIndex < (textToSearch.length() - searchedText.length()); charIndex++) {
      if (isCancelled()) break;

      final int nextPosition = textToSearch.indexOf(searchedText, charIndex + 1);

      if(nextPosition == -1) {
        break;
      }

      try {
        lineNumberReader.skip(nextPosition - charIndex);
      } catch (IOException e) {
        e.printStackTrace();
      }

      charIndex = nextPosition;

      final SearchResultIndex index = new SearchResultIndex(charIndex, charIndex + searchedText.length(), lineNumberReader.getLineNumber());

      searchResultIndices.add(index);

      publishProgress(index);
    }

    return searchResultIndices;
  }

  @Override
  protected void onProgressUpdate(SearchResultIndex... values) {
    super.onProgressUpdate(values);

    updateListener.onUpdate(values[0]);
  }

  @Override
  protected void onPostExecute(final List<SearchResultIndex> searchResultIndices) {
    super.onPostExecute(searchResultIndices);

    listener.onAsyncTaskFinished(searchResultIndices);
  }
}
