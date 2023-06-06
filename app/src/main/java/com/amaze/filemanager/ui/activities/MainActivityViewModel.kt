/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.activities

import android.app.Application
import android.provider.MediaStore
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.root.ListFilesCommand.listFiles
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainActivityViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    var mediaCacheHash: List<List<LayoutElementParcelable>?> = List(5) { null }
    var listCache: LruCache<String, List<LayoutElementParcelable>> = LruCache(50)

    companion object {
        /**
         * size of list to be cached for local files
         */
        val CACHE_LOCAL_LIST_THRESHOLD: Int = 100
    }

    /**
     * Put list for a given path in cache
     */
    fun putInCache(path: String, listToCache: List<LayoutElementParcelable>) {
        viewModelScope.launch(Dispatchers.Default) {
            listCache.put(path, listToCache)
        }
    }

    /**
     * Removes cache for a given path
     */
    fun evictPathFromListCache(path: String) {
        viewModelScope.launch(Dispatchers.Default) {
            listCache.remove(path)
        }
    }

    /**
     * Get cache from a given path and updates files / folder count
     */
    fun getFromListCache(path: String): List<LayoutElementParcelable>? {
        return listCache.get(path)
    }

    /**
     * Get cache from a given path and updates files / folder count
     */
    fun getFromMediaFilesCache(mediaType: Int): List<LayoutElementParcelable>? {
        return mediaCacheHash[mediaType]
    }

    fun basicSearch(mainActivity: MainActivity, query: String): MutableLiveData<ArrayList<HybridFileParcelable>> {
        val hybridFileParcelables = ArrayList<HybridFileParcelable>()

        val mutableLiveData: MutableLiveData<ArrayList<HybridFileParcelable>> = MutableLiveData(hybridFileParcelables)

        val showHiddenFiles = PreferenceManager
            .getDefaultSharedPreferences(mainActivity)
            .getBoolean(PREFERENCE_SHOW_HIDDENFILES, false)

        viewModelScope.launch(Dispatchers.IO) {
            listFiles(
                mainActivity.currentMainFragment!!.currentPath!!,
                mainActivity.isRootExplorer,
                showHiddenFiles,
                { _: OpenMode? -> null }
            ) { hybridFileParcelable: HybridFileParcelable ->
                if (hybridFileParcelable.getName(mainActivity)
                        .lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault())) &&
                    (showHiddenFiles || !hybridFileParcelable.isHidden)
                ) {
                    hybridFileParcelables.add(hybridFileParcelable)

                    mutableLiveData.postValue(hybridFileParcelables)
                }
            }
        }

        return mutableLiveData
    }

    fun indexedSearch(
        mainActivity: MainActivity,
        query: String,
    ): MutableLiveData< ArrayList<HybridFileParcelable> > {

        val list = ArrayList<HybridFileParcelable>()

        val mutableLiveData: MutableLiveData<ArrayList<HybridFileParcelable>> = MutableLiveData(list)

        val showHiddenFiles =
            PreferenceManager.getDefaultSharedPreferences(mainActivity)
                .getBoolean(PREFERENCE_SHOW_HIDDENFILES, false)

        viewModelScope.launch(Dispatchers.IO) {

            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)

            val cursor = mainActivity
                .contentResolver
                .query(MediaStore.Files.getContentUri("external"), projection, null, null, null)
                ?: return@launch

            if (cursor.count > 0 && cursor.moveToFirst()) {
                do {
                    val path =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))

                    if (path != null
                        && path.contains(mainActivity.currentMainFragment?.currentPath!!)
                        && File(path).name.lowercase(Locale.getDefault()).contains(
                            query.lowercase(Locale.getDefault())
                        )
                    ) {

                        val hybridFileParcelable =
                            RootHelper.generateBaseFile(File(path), showHiddenFiles)

                        if (hybridFileParcelable != null) {
                            list.add(hybridFileParcelable)
                            mutableLiveData.postValue(list)
                        }
                    }
                } while (cursor.moveToNext())
            }

            cursor.close()
        }

        return mutableLiveData
    }
}
