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
import android.os.Environment
import android.provider.MediaStore
import androidx.collection.LruCache
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.filesystem.root.ListFilesCommand.listFiles
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES
import com.amaze.trashbin.DeletePermanentlyCallback
import com.amaze.trashbin.MoveFilesCallback
import com.amaze.trashbin.TrashBin
import com.amaze.trashbin.TrashBinConfig
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

    private var trashBinConfig: TrashBinConfig? = null
    private var trashBin: TrashBin? = null

    private val TRASH_BIN_BASE_PATH = Environment.getExternalStorageDirectory()
        .path + File.separator + ".AmazeData"

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

    fun getTrashBinInstance(): TrashBin {
        if (trashBin == null) {
            trashBin = TrashBin(
                getTrashbinConfig(),
                object : DeletePermanentlyCallback {
                    override fun invoke(deletePath: String): Boolean {
                        viewModelScope.launch(Dispatchers.IO) {
                            val hybridFile = HybridFile(OpenMode.FILE, deletePath)
                            hybridFile.delete(applicationContext, false)
                        }
                        return true
                    }
                },
                null
            )
        }
        return trashBin!!
    }

    fun getTrashbinConfig(): TrashBinConfig {
        if (trashBinConfig == null) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_DAYS,
                TrashBinConfig.RETENTION_DAYS_INFINITE
            )
            val bytes = sharedPrefs.getLong(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_BYTES,
                TrashBinConfig.RETENTION_BYTES_INFINITE
            )
            val numOfFiles = sharedPrefs.getInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_NUM_OF_FILES,
                TrashBinConfig.RETENTION_NUM_OF_FILES
            )
            trashBinConfig = TrashBinConfig(
                TRASH_BIN_BASE_PATH, days, bytes,
                numOfFiles, false, true
            )
        }
        return trashBinConfig!!
    }

    fun moveToBinLightWeight(mediaFileInfoList: List<LayoutElementParcelable>) {
        viewModelScope.launch(Dispatchers.IO) {
            val trashBinFilesList = mediaFileInfoList.map { it.generateBaseFile()
                .toTrashBinFile(applicationContext) }
            getTrashBinInstance().moveToBin(
                trashBinFilesList, true,
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
                        val uri = FileProvider.getUriForFile(
                            applicationContext,
                            applicationContext.packageName, File(originalFilePath)
                        )
                        FileUtils.scanFile(
                            uri,
                            applicationContext
                        )
                        return true
                    }
                }
            )
        }
    }

    fun restoreFromBin(mediaFileInfoList: List<LayoutElementParcelable>) {
        viewModelScope.launch(Dispatchers.IO) {
            LOG.info("Moving media files to bin $mediaFileInfoList")
            val trashBinFilesList = mediaFileInfoList.map { it.generateBaseFile()
                .toTrashBinFile(applicationContext) }
            getTrashBinInstance().restore(
                trashBinFilesList, true,
                object : MoveFilesCallback {
                    override fun invoke(source: String, dest: String): Boolean {
                        val sourceFile = File(source)
                        val destFile = File(dest)
                        if (!sourceFile.renameTo(destFile)) {
                            return false
                        }
                        val uri = FileProvider.getUriForFile(
                            applicationContext,
                            applicationContext.packageName, File(dest)
                        )
                        FileUtils.scanFile(
                            uri,
                            applicationContext
                        )
                        return true
                    }
                }
            )
        }
    }

    fun progressTrashBinFilesLiveData(): MutableLiveData<MutableList<LayoutElementParcelable>?> {
        if (trashBinFilesLiveData == null) {
            trashBinFilesLiveData = MutableLiveData()
            trashBinFilesLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                trashBinFilesLiveData?.postValue(
                    ArrayList(
                        getTrashBinInstance().listFilesInBin()
                            .map {
                                HybridFile(OpenMode.FILE, it.path, it.fileName, it.isDirectory)
                                    .generateLayoutElement(applicationContext, false
                                )
                            }
                    )
                )
            }
        }
        return trashBinFilesLiveData!!
    }
}
