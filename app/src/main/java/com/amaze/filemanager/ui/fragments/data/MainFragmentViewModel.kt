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

package com.amaze.filemanager.ui.fragments.data

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.database.CloudHandler
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import com.amaze.filemanager.utils.DataUtils
import java.util.*

class MainFragmentViewModel : ViewModel() {

    var currentPath: String? = null

    /** This is not an exact copy of the elements in the adapter  */
    var listElements: ArrayList<LayoutElementParcelable>? = null

    var fileCount = 0
    var folderCount: Int = 0
    var columns: Int = 0
    var smbPath: String? = null
    var searchHelper = ArrayList<HybridFileParcelable>()
    var no = 0

    var sortby = 0
    var dsort = 0
    var asc = 0
    var home: String? = null
    var selection = false
    var results: Boolean = false
    lateinit var openMode: OpenMode

    // defines the current visible tab, default either 0 or 1
    // private int mCurrentTab;
    /** For caching the back button  */
    var back: LayoutElementParcelable? = null

    var dragAndDropPreference = 0

    var isEncryptOpen = false // do we have to open a file when service is begin destroyed

    // the cached base file which we're to open, delete it later
    var encryptBaseFile: HybridFileParcelable? = null

    /** a list of encrypted base files which are supposed to be deleted  */
    var encryptBaseFiles = ArrayList<HybridFileParcelable>()

    // defines the current visible tab, default either 0 or 1
    // private int mCurrentTab;

    /*boolean identifying if the search task should be re-run on back press after pressing on
    any of the search result*/
    var retainSearchTask = false

    /** boolean to identify if the view is a list or grid  */
    var isList = true
    var addHeader = false
    var accentColor = 0
    var primaryColor = 0
    var primaryTwoColor = 0
    var stopAnims = true

    /**
     * Initialize arguemnts from bundle in MainFragment
     */
    fun initBundleArguments(bundle: Bundle?) {
        bundle?.run {
            if (no == 0) {
                no = getInt("no", 1)
            }

            if (home.isNullOrBlank()) {
                getString("home")?.run {
                    home = this
                }
            }

            if (currentPath.isNullOrBlank()) {
                getString("lastpath")?.run {
                    currentPath = this
                }
            }
            if (!::openMode.isInitialized) {
                openMode = if (getInt("openmode", -1) !== -1) {
                    OpenMode.getOpenMode(getInt("openmode", -1))
                } else {
                    OpenMode.FILE
                }
            }
        }
    }

    /**
     * Initialize drag drop preference
     */
    fun initDragAndDropPreference(sharedPreferences: SharedPreferences) {
        dragAndDropPreference = sharedPreferences.getInt(
            PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
            PreferencesConstants.PREFERENCE_DRAG_DEFAULT
        )
    }

    /**
     * Initialize isList from dataUtils
     */
    fun initIsList() {
        isList = DataUtils.getInstance().getListOrGridForPath(
            currentPath,
            DataUtils.LIST
        ) == DataUtils.LIST
    }

    /**
     * Initialize column number from preference
     */
    fun initColumns(sharedPreferences: SharedPreferences) {
        if (columns == 0) {
            sharedPreferences.getString(
                PreferencesConstants
                    .PREFERENCE_GRID_COLUMNS,
                "-1"
            )?.toInt()?.run {
                columns = this
            }
        }
    }

    /**
     * Assigns sort modes A value from 0 to 3 defines sort mode as name/last modified/size/type in
     * ascending order Values from 4 to 7 defines sort mode as name/last modified/size/type in
     * descending order
     *
     *
     * Final value of [.sortby] varies from 0 to 3
     */
    fun initSortModes(sortType: Int, sharedPref: SharedPreferences) {
        if (sortType <= 3) {
            sortby = sortType
            asc = 1
        } else {
            asc = -1
            sortby = sortType - 4
        }
        sharedPref.getString(
            PreferencesConstants.PREFERENCE_DIRECTORY_SORT_MODE,
            "0"
        )?.run {
            dsort = Integer.parseInt(this)
        }
    }

    /**
     * Initialize encrypted file
     */
    fun initEncryptBaseFile(path: String) {
        encryptBaseFile = HybridFileParcelable(path)
        encryptBaseFile?.run {
            encryptBaseFiles.add(this)
        }
    }

    /**
     * Check if current path is cloud path
     */
    fun getIsOnCloud(): Boolean {
        return CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/" == currentPath ||
            CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/" == currentPath ||
            CloudHandler.CLOUD_PREFIX_BOX + "/" == currentPath ||
            CloudHandler.CLOUD_PREFIX_DROPBOX + "/" == currentPath
    }
}
