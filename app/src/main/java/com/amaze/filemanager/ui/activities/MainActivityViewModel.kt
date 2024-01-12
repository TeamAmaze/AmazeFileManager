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
import android.content.Intent
import android.provider.MediaStore
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem.DeepSearch
import com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem.searchParametersFromBoolean
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.files.MediaConnectionUtils.scanFile
import com.amaze.filemanager.filesystem.root.ListFilesCommand.listFiles
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_REGEX
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_REGEX_MATCHES
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES
import com.amaze.trashbin.MoveFilesCallback
import com.amaze.trashbin.TrashBinFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale

class MainActivityViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    var mediaCacheHash: List<List<LayoutElementParcelable>?> = List(5) { null }
    var listCache: LruCache<String, List<LayoutElementParcelable>> = LruCache(50)
    var trashBinFilesLiveData: MutableLiveData<MutableList<LayoutElementParcelable>?>? = null

    companion object {
        /**
         * size of list to be cached for local files
         */
        val CACHE_LOCAL_LIST_THRESHOLD: Int = 100
        private val LOG = LoggerFactory.getLogger(MainActivityViewModel::class.java)
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

    /**
     * Perform basic search: searches on the current directory
     */
    fun basicSearch(mainActivity: MainActivity, query: String):
        MutableLiveData<ArrayList<HybridFileParcelable>> {
        val hybridFileParcelables = ArrayList<HybridFileParcelable>()

        val mutableLiveData:
            MutableLiveData<ArrayList<HybridFileParcelable>> =
            MutableLiveData(hybridFileParcelables)

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

    /**
     * Perform indexed search: on MediaStore items from the current directory & it's children
     */
    fun indexedSearch(
        mainActivity: MainActivity,
        query: String
    ): MutableLiveData<ArrayList<HybridFileParcelable>> {
        val list = ArrayList<HybridFileParcelable>()

        val mutableLiveData: MutableLiveData<ArrayList<HybridFileParcelable>> = MutableLiveData(
            list
        )

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
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                        )

                    if (path != null &&
                        path.contains(mainActivity.currentMainFragment?.currentPath!!) &&
                        File(path).name.lowercase(Locale.getDefault()).contains(
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

    /**
     * Perform deep search: search recursively for files matching [query] in the current path
     */
    fun deepSearch(
        mainActivity: MainActivity,
        query: String
    ): MutableLiveData<ArrayList<HybridFileParcelable>> {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(mainActivity)
        val showHiddenFiles = sharedPref.getBoolean(PREFERENCE_SHOW_HIDDENFILES, false)
        val isRegexEnabled = sharedPref.getBoolean(PREFERENCE_REGEX, false)
        val isMatchesEnabled = sharedPref.getBoolean(PREFERENCE_REGEX_MATCHES, false)
        val isRoot = mainActivity.isRootExplorer
        val searchParameters = searchParametersFromBoolean(
            showHiddenFiles,
            isRegexEnabled,
            isMatchesEnabled,
            isRoot
        )

        val path = mainActivity.currentMainFragment?.currentPath ?: ""
        val openMode =
            mainActivity.currentMainFragment?.mainFragmentViewModel?.openMode ?: OpenMode.FILE

        val context = this.applicationContext

        val deepSearch = DeepSearch(
            context,
            query,
            path,
            openMode,
            searchParameters
        )

        viewModelScope.launch(Dispatchers.IO) {
            deepSearch.search()
        }

        return deepSearch.mutableLiveData
    }

    /**
     * TODO: Documentation
     */
    fun moveToBinLightWeight(mediaFileInfoList: List<LayoutElementParcelable>) {
        viewModelScope.launch(Dispatchers.IO) {
            val trashBinFilesList = mediaFileInfoList.map {
                it.generateBaseFile()
                    .toTrashBinFile(applicationContext)
            }
            AppConfig.getInstance().trashBinInstance.moveToBin(
                trashBinFilesList,
                true,
                object : MoveFilesCallback {
                    override fun invoke(
                        originalFilePath: String,
                        trashBinDestination: String
                    ): Boolean {
                        val source = File(originalFilePath)
                        val dest = File(trashBinDestination)
                        if (!source.renameTo(dest)) {
                            return false
                        }
                        val hybridFile = HybridFile(
                            OpenMode.TRASH_BIN,
                            originalFilePath
                        )
                        scanFile(applicationContext, arrayOf(hybridFile))
                        val intent = Intent(MainActivity.KEY_INTENT_LOAD_LIST)
                        hybridFile.getParent(applicationContext)?.let {
                            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, it)
                            applicationContext.sendBroadcast(intent)
                        }
                        return true
                    }
                }
            )
        }
    }

    /**
     * Restore files from trash bin
     */
    fun restoreFromBin(mediaFileInfoList: List<LayoutElementParcelable>) {
        viewModelScope.launch(Dispatchers.IO) {
            LOG.info("Restoring media files from bin $mediaFileInfoList")
            val filesToRestore = mutableListOf<TrashBinFile>()
            for (element in mediaFileInfoList) {
                val restoreFile = element.generateBaseFile()
                    .toTrashBinRestoreFile(applicationContext)
                if (restoreFile != null) {
                    filesToRestore.add(restoreFile)
                }
            }
            AppConfig.getInstance().trashBinInstance.restore(
                filesToRestore,
                true,
                object : MoveFilesCallback {
                    override fun invoke(source: String, dest: String): Boolean {
                        val sourceFile = File(source)
                        val destFile = File(dest)
                        if (destFile.exists()) {
                            AppConfig.toast(
                                applicationContext,
                                applicationContext.getString(R.string.fileexist)
                            )
                            return false
                        }
                        if (destFile.parentFile != null && !destFile.parentFile!!.exists()) {
                            destFile.parentFile?.mkdirs()
                        }
                        if (!sourceFile.renameTo(destFile)) {
                            return false
                        }
                        val hybridFile = HybridFile(
                            OpenMode.TRASH_BIN,
                            source
                        )
                        scanFile(applicationContext, arrayOf(hybridFile))
                        val intent = Intent(MainActivity.KEY_INTENT_LOAD_LIST)
                        hybridFile.getParent(applicationContext)?.let {
                            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, it)
                            applicationContext.sendBroadcast(intent)
                        }
                        return true
                    }
                }
            )
        }
    }

    /**
     * TODO: Documentation
     */
    fun progressTrashBinFilesLiveData(): MutableLiveData<MutableList<LayoutElementParcelable>?> {
        if (trashBinFilesLiveData == null) {
            trashBinFilesLiveData = MutableLiveData()
            trashBinFilesLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                trashBinFilesLiveData?.postValue(
                    ArrayList(
                        AppConfig.getInstance().trashBinInstance.listFilesInBin()
                            .map {
                                HybridFile(OpenMode.FILE, it.path, it.fileName, it.isDirectory)
                                    .generateLayoutElement(
                                        applicationContext,
                                        false
                                    )
                            }
                    )
                )
            }
        }
        return trashBinFilesLiveData!!
    }
}
