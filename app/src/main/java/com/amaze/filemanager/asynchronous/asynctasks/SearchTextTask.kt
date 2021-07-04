/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks

import android.os.AsyncTask
import android.text.TextUtils
import com.amaze.filemanager.ui.activities.texteditor.SearchResultIndex
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import com.amaze.filemanager.utils.OnProgressUpdate
import java.io.IOException
import java.io.LineNumberReader
import java.io.StringReader
import java.util.ArrayList

class SearchTextTask(
    private val textToSearch: String,
    private val searchedText: String,
    private val updateListener: OnProgressUpdate<SearchResultIndex>,
    private val listener: OnAsyncTaskFinished<List<SearchResultIndex>>
) : AsyncTask<Unit, SearchResultIndex, List<SearchResultIndex>>() {
    private val lineNumberReader: LineNumberReader

    override fun doInBackground(vararg params: Unit): List<SearchResultIndex> {
        if (TextUtils.isEmpty(searchedText)) {
            return emptyList()
        }

        val searchResultIndices = ArrayList<SearchResultIndex>()
        var charIndex = 0
        while (charIndex < textToSearch.length - searchedText.length) {
            if (isCancelled) break
            val nextPosition = textToSearch.indexOf(searchedText, charIndex)
            if (nextPosition == -1) {
                break
            }
            try {
                lineNumberReader.skip((nextPosition - charIndex).toLong())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            charIndex = nextPosition
            val index = SearchResultIndex(
                charIndex,
                charIndex + searchedText.length,
                lineNumberReader.lineNumber
            )
            searchResultIndices.add(index)
            publishProgress(index)
            charIndex++
        }

        return searchResultIndices
    }

    override fun onProgressUpdate(vararg values: SearchResultIndex) {
        updateListener.onUpdate(values[0])
    }

    override fun onPostExecute(searchResultIndices: List<SearchResultIndex>) {
        listener.onAsyncTaskFinished(searchResultIndices)
    }

    init {
        val stringReader = StringReader(textToSearch)
        lineNumberReader = LineNumberReader(stringReader)
    }
}
