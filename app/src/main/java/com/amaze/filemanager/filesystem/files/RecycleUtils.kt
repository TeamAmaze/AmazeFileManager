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

package com.amaze.filemanager.filesystem.files

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.asynctasks.PrepareCopyTask
import com.amaze.filemanager.database.models.utilities.RecycleItem
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.RecyclePromptDialog
import com.amaze.filemanager.utils.DataUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * This class contains the code for the implementation of recycle bin
 *
 * @author Vishnu on 12/12/2021, at 12:12
 */
class RecycleUtils {

    companion object {

        private val TAG: String = "RecycleUtils"

        @Suppress("INACCESSIBLE_TYPE")
        fun moveToRecycleBin(
            path: String,
            positions: ArrayList<HybridFileParcelable>,
            context: Context,
            mainActivity: MainActivity,
        ) {
            RecyclePromptDialog(
                recycleCallback = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.recycling),
                        Toast.LENGTH_LONG
                    ).show()

                    val date: String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

                    PrepareCopyTask(
                        getRecycleBinPath() + File.separator + date + File.separator + path,
                        true,
                        mainActivity,
                        mainActivity.isRootExplorer,
                        mainActivity.currentMainFragment?.mainFragmentViewModel?.openMode,
                    ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, positions)
                },
                deleteCallback = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.deleting),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    mainActivity.mainActivityHelper.deleteFiles(positions)
                }
            ).show(mainActivity.supportFragmentManager, RecyclePromptDialog.TAG)
        }

        fun moveToRecycleBin(
            path: String,
            positions: ArrayList<HybridFileParcelable>,
            mainActivity: MainActivity,
        ) {
            moveToRecycleBin(path, positions, mainActivity.applicationContext, mainActivity)
        }

        fun restoreFromRecycleBin(
            positions: ArrayList<HybridFileParcelable>,
            mainActivity: MainActivity,
        ) {
            restoreFromRecycleBin(positions, mainActivity.applicationContext, mainActivity)
        }

        @Suppress("INACCESSIBLE_TYPE")
        fun restoreFromRecycleBin(
            positions: ArrayList<HybridFileParcelable>,
            context: Context,
            mainActivity: MainActivity,
        ) {

            for (file in positions) {

                Toast.makeText(
                    context,
                    context.getString(R.string.restoring),
                    Toast.LENGTH_LONG
                ).show()

                var path = file.path
                    .replace(
                        getRecycleBinPath(),
                        ""
                    )

                path = path
                    .replace(
                        "/" + path.split("/")[1],
                        ""
                    )
                    .replace(
                        file.name,
                        ""
                    )

                Log.e(TAG, "restoreFromRecycleBin path: $path")

                PrepareCopyTask(
                    path,
                    true,
                    mainActivity,
                    mainActivity.isRootExplorer,
                    mainActivity.currentMainFragment?.mainFragmentViewModel?.openMode,
                ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, positions)

            }

        }

        fun getRecycleBinPath(): String {
            var s = Environment.getExternalStorageDirectory()
                .toString() + File.separator + ".AmazeFileManager"

            DataUtils.getInstance().addHiddenFile(s)

            s += "/RecycleBin"

            if (!File(s).exists()) File(s).mkdirs()

            return s
        }

    }
}
