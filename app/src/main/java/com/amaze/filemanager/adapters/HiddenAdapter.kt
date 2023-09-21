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

package com.amaze.filemanager.adapters

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.holders.HiddenViewHolder
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.utils.DataUtils
import java.io.File
import java.util.ArrayList
import kotlin.concurrent.thread

/**
 * This Adapter contains all logic related to showing the list of hidden files.
 *
 * @see com.amaze.filemanager.adapters.holders.HiddenViewHolder
 */
class HiddenAdapter(
    private val context: Context,
    private val mainFragment: MainFragment,
    private val sharedPrefs: SharedPreferences,
    hiddenFiles: List<HybridFile>,
    var materialDialog: MaterialDialog?,
    private val hide: Boolean
) : RecyclerView.Adapter<HiddenViewHolder>() {

    companion object {
        private const val TAG = "HiddenAdapter"
    }

    private val hiddenFiles = hiddenFiles.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiddenViewHolder {
        val mInflater = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = mInflater.inflate(R.layout.bookmarkrow, parent, false)
        return HiddenViewHolder(view)
    }

    override fun onBindViewHolder(holder: HiddenViewHolder, position: Int) {
        val file = hiddenFiles[position]
        holder.textTitle.text = file.getName(context)
        holder.textDescription.text = file.getReadablePath(file.path)
        if (hide) {
            holder.deleteButton.visibility = View.GONE
        }
        holder.deleteButton.setOnClickListener {
            // if the user taps on the delete button, un-hide the file.
            // TODO: the "hide files" feature just hide files from view in Amaze and not create
            // .nomedia
            if (!file.isSmb && file.isDirectory(context)) {
                val nomediaFile = HybridFileParcelable(
                    hiddenFiles[position].path + "/" + FileUtils.NOMEDIA_FILE
                )
                nomediaFile.mode = OpenMode.FILE
                val filesToDelete = ArrayList<HybridFileParcelable>()
                filesToDelete.add(nomediaFile)
                val task = DeleteTask(context, false)
                task.execute(filesToDelete)
            }
            DataUtils.getInstance().removeHiddenFile(hiddenFiles[position].path)
            hiddenFiles.remove(hiddenFiles[position])
            notifyItemRemoved(position)
        }
        holder.row.setOnClickListener {
            // if the user taps on the hidden file, take the user there.
            materialDialog?.dismiss()

            thread {
                val fragmentActivity = mainFragment.requireActivity()
                if (file.isDirectory(context)) {
                    fragmentActivity.runOnUiThread {
                        mainFragment.loadlist(
                            file.path,
                            false,
                            OpenMode.UNKNOWN,
                            false
                        )
                    }
                } else if (!file.isSmb) {
                    fragmentActivity.runOnUiThread {
                        FileUtils.openFile(
                            File(file.path),
                            (fragmentActivity as MainActivity),
                            sharedPrefs
                        )
                    }
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int = hiddenFiles.size
}
