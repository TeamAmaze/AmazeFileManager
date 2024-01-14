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

package com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem

import android.content.Context
import android.os.AsyncTask
import androidx.preference.PreferenceManager
import com.amaze.filemanager.asynchronous.asynctasks.StatefulAsyncTask
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.fragments.SearchWorkerFragment.HelperCallbacks
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES
import com.amaze.filemanager.utils.OnFileFound
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.regex.Pattern

class SearchAsyncTask(
    context: Context,
    private val input: String,
    openMode: OpenMode?,
    private val rootMode: Boolean,
    private val isRegexEnabled: Boolean,
    private val isMatchesEnabled: Boolean,
    path: String?
) : AsyncTask<Void?, HybridFileParcelable?, Void?>(), StatefulAsyncTask<HelperCallbacks?> {
    private val LOG = LoggerFactory.getLogger(SearchAsyncTask::class.java)

    /** This necessarily leaks the context  */
    private val applicationContext: Context
    private var callbacks: HelperCallbacks? = null
    private val file: HybridFile

    init {
        applicationContext = context.applicationContext
        file = HybridFile(openMode, path)
    }

    override fun onPreExecute() {
        /*
     * Note that we need to check if the callbacks are null in each
     * method in case they are invoked after the Activity's and
     * Fragment's onDestroy() method have been called.
     */
        if (callbacks != null) {
            callbacks!!.onPreExecute(input)
        }
    }

    // callbacks not checked for null because of possibility of
    // race conditions b/w worker thread main thread
    protected override fun doInBackground(vararg params: Void?): Void? {
        if (file.isSmb) return null

        // level 1
        // if regex or not
        if (!isRegexEnabled) {
            search(file, input)
        } else {
            // compile the regular expression in the input
            val pattern = Pattern.compile(
                bashRegexToJava(
                    input
                )
            )
            // level 2
            if (!isMatchesEnabled) searchRegExFind(file, pattern) else searchRegExMatch(
                file,
                pattern
            )
        }
        return null
    }

    public override fun onPostExecute(c: Void?) {
        if (callbacks != null) {
            callbacks!!.onPostExecute(input)
        }
    }

    override fun onCancelled() {
        if (callbacks != null) callbacks!!.onCancelled()
    }

    override fun onProgressUpdate(vararg values: HybridFileParcelable?) {
        if (!isCancelled && callbacks != null) {
            values[0]?.let { callbacks!!.onProgressUpdate(it, input) }
        }
    }

    override fun setCallback(helperCallbacks: HelperCallbacks?) {
        callbacks = helperCallbacks
    }

    /**
     * Recursively search for occurrences of a given text in file names and publish the result
     *
     * @param directory the current path
     */
    private fun search(directory: HybridFile, filter: SearchFilter) {
        if (directory.isDirectory(
                applicationContext
            )
        ) { // do you have permission to read this directory?
            directory.forEachChildrenFile(
                applicationContext,
                rootMode,
                object : OnFileFound {
                    override fun onFileFound(file: HybridFileParcelable) {
                        val showHiddenFiles =
                            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                                .getBoolean(PREFERENCE_SHOW_HIDDENFILES, false)
                        if (!isCancelled && (showHiddenFiles || !file.isHidden)) {
                            if (filter.searchFilter(file.getName(applicationContext))) {
                                publishProgress(file)
                            }
                            if (file.isDirectory && !isCancelled) {
                                search(file, filter)
                            }
                        }
                    }
                }
            )
        } else {
            LOG.warn("Cannot search " + directory.path + ": Permission Denied")
        }
    }

    /**
     * Recursively search for occurrences of a given text in file names and publish the result
     *
     * @param file the current path
     * @param query the searched text
     */
    private fun search(file: HybridFile, query: String) {
        search(file) { fileName: String ->
            fileName.lowercase(Locale.getDefault()).contains(
                query.lowercase(
                    Locale.getDefault()
                )
            )
        }
    }

    /**
     * Recursively find a java regex pattern [Pattern] in the file names and publish the result
     *
     * @param file the current file
     * @param pattern the compiled java regex
     */
    private fun searchRegExFind(file: HybridFile, pattern: Pattern) {
        search(file) { fileName: String? -> pattern.matcher(fileName).find() }
    }

    /**
     * Recursively match a java regex pattern [Pattern] with the file names and publish the
     * result
     *
     * @param file the current file
     * @param pattern the compiled java regex
     */
    private fun searchRegExMatch(file: HybridFile, pattern: Pattern) {
        search(file) { fileName: String? -> pattern.matcher(fileName).matches() }
    }

    /**
     * method converts bash style regular expression to java. See [Pattern]
     *
     * @return converted string
     */
    private fun bashRegexToJava(originalString: String): String {
        val stringBuilder = StringBuilder()
        for (i in originalString.indices) {
            when (originalString[i].toString() + "") {
                "*" -> stringBuilder.append("\\w*")
                "?" -> stringBuilder.append("\\w")
                else -> stringBuilder.append(originalString[i])
            }
        }
        return stringBuilder.toString()
    }

    fun interface SearchFilter {
        fun searchFilter(fileName: String): Boolean
    }
}
