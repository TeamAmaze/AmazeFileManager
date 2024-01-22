/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem.SearchResult
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.colors.ColorPreference
import java.util.Random

class SearchRecyclerViewAdapter :
    ListAdapter<SearchResult, SearchRecyclerViewAdapter.ViewHolder>(

        object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(
                oldItem: SearchResult,
                newItem: SearchResult
            ): Boolean {
                return oldItem.file.path == newItem.file.path &&
                    oldItem.file.name == newItem.file.name
            }

            override fun areContentsTheSame(
                oldItem: SearchResult,
                newItem: SearchResult
            ): Boolean {
                return oldItem.file.path == newItem.file.path &&
                    oldItem.file.name == newItem.file.name &&
                    oldItem.matchRange == newItem.matchRange
            }
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_row_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: SearchRecyclerViewAdapter.ViewHolder, position: Int) {
        val (file, matchResult) = getItem(position)

        holder.fileNameTV.text = file.name
        holder.filePathTV.text = file.path.substring(0, file.path.lastIndexOf("/"))

        holder.colorView.setBackgroundColor(getRandomColor(holder.colorView.context))

        val colorPreference =
            (AppConfig.getInstance().mainActivityContext as MainActivity).currentColorPreference

        if (file.isDirectory) {
            holder.colorView.setBackgroundColor(colorPreference.primaryFirstTab)
        } else {
            holder.colorView.setBackgroundColor(colorPreference.accent)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val fileNameTV: AppCompatTextView
        val filePathTV: AppCompatTextView
        val colorView: View

        init {

            fileNameTV = view.findViewById(R.id.searchItemFileNameTV)
            filePathTV = view.findViewById(R.id.searchItemFilePathTV)
            colorView = view.findViewById(R.id.searchItemSampleColorView)

            view.setOnClickListener {

                val (file, _) = getItem(adapterPosition)

                if (!file.isDirectory) {
                    file.openFile(
                        AppConfig.getInstance().mainActivityContext as MainActivity?,
                        false
                    )
                } else {
                    (AppConfig.getInstance().mainActivityContext as MainActivity?)
                        ?.goToMain(file.path)
                }

                (AppConfig.getInstance().mainActivityContext as MainActivity?)
                    ?.appbar?.searchView?.hideSearchView()
            }
        }
    }

    private fun getRandomColor(context: Context): Int {
        return ContextCompat.getColor(
            context,
            ColorPreference.availableColors[
                Random().nextInt(
                    ColorPreference.availableColors.size - 1
                )
            ]
        )
    }
}
