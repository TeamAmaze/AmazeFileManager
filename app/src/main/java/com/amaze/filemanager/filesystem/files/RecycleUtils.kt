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
import kotlin.collections.ArrayList

/**
 * This class contains the code for the implementation of recycle bin
 *
 * @author Vishnu on 12/12/2021, at 12:12
 */
class RecycleUtils {

    companion object {

        private const val RECYCLE_META_DATA_FILE_NAME = ".recycle_meta_data.json"

        fun moveToRecycleBin(
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

                    PrepareCopyTask(
                        getRecycleBinPath(),
                        true,
                        mainActivity,
                        mainActivity.isRootExplorer,
                        mainActivity.currentMainFragment?.mainFragmentViewModel?.openMode
                    ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, positions)

                    addRecycledFile(positions)
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
            positions: ArrayList<HybridFileParcelable>,
            mainActivity: MainActivity,
        ) {
            moveToRecycleBin(positions, mainActivity.applicationContext, mainActivity)
        }

        fun restoreFromRecycleBin(
            positions: ArrayList<HybridFileParcelable>,
            mainActivity: MainActivity,
        ) {
            restoreFromRecycleBin(positions, mainActivity.applicationContext, mainActivity)
        }

        fun restoreFromRecycleBin(
            positions: ArrayList<HybridFileParcelable>,
            context: Context,
            mainActivity: MainActivity,
        ) {

            val list = loadMetaDataJSONFile()

            for (item in positions) {

                if (list.size == 1) {

                    val o = list[0]

                    if (o.name.equals(item.name)) {

                        Toast.makeText(
                            context,
                            context.getString(R.string.restoring),
                            Toast.LENGTH_LONG
                        ).show()

                        PrepareCopyTask(
                            o.path?.replace(item.name, ""),
                            true,
                            mainActivity,
                            mainActivity.isRootExplorer,
                            mainActivity.currentMainFragment?.mainFragmentViewModel?.openMode
                        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, positions)

                        list.removeAt(0)

                        writeMetaDataJSONFile(list)
                    }
                }

                for (i in 0 until list.size - 1) {

                    val o = list[i]

                    if (o.name.equals(item.name)) {

                        Toast.makeText(
                            context,
                            context.getString(R.string.restoring),
                            Toast.LENGTH_LONG
                        ).show()

                        PrepareCopyTask(
                            o.path?.replace(item.name, ""),
                            true,
                            mainActivity,
                            mainActivity.isRootExplorer,
                            mainActivity.currentMainFragment?.mainFragmentViewModel?.openMode
                        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, positions)

                        list.removeAt(i)

                        writeMetaDataJSONFile(list)
                    }
                }
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

        private fun getRecycleMetaDataFilePath(): String {
            var s = Environment.getExternalStorageDirectory()
                .toString() + File.separator + ".AmazeFileManager/RecycleBin"

            if (!File(s).exists()) File(s).mkdirs()

            s += File.separator + RECYCLE_META_DATA_FILE_NAME

            DataUtils.getInstance().addHiddenFile(s)

            return s
        }

        private fun addRecycledFile(positions: ArrayList<HybridFileParcelable>) {

            val list = loadMetaDataJSONFile()

            for (item in positions) {
                list.add(
                    RecycleItem(
                        item.path,
                        item.name,
                        item.date
                    )
                )
            }

            writeMetaDataJSONFile(list)
        }

        private fun writeMetaDataJSONFile(list: ArrayList<RecycleItem>) {

            val bufferedWriter = BufferedWriter(FileWriter(File(getRecycleMetaDataFilePath())))
            val type = object : TypeToken<ArrayList<RecycleItem>?>() {}.type
            bufferedWriter.write(Gson().toJson(list, type))
            bufferedWriter.close()
        }

        private fun loadMetaDataJSONFile(): ArrayList<RecycleItem> {

            val stringBuilder = StringBuilder("")

            try {

                val bufferedReader = BufferedReader(FileReader(File(getRecycleMetaDataFilePath())))

                var line: String? = bufferedReader.readLine()

                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = bufferedReader.readLine()
                }

                bufferedReader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val s = stringBuilder.toString()

            return if (s == "") {
                ArrayList()
            } else {

                Gson().fromJson(
                    s,
                    object : TypeToken<ArrayList<RecycleItem>?>() {}.type
                )
            }
        }
    }
}
