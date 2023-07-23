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
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.colors.ColorPreference
import java.util.Random

class SearchRecyclerViewAdapter :
    ListAdapter<HybridFileParcelable, SearchRecyclerViewAdapter.ViewHolder>(

        object : DiffUtil.ItemCallback<HybridFileParcelable>() {
            override fun areItemsTheSame(
                oldItem: HybridFileParcelable,
                newItem: HybridFileParcelable
            ): Boolean {
                return oldItem.path == newItem.path && oldItem.name == newItem.name
            }

            override fun areContentsTheSame(
                oldItem: HybridFileParcelable,
                newItem: HybridFileParcelable
            ): Boolean {
                return oldItem.path == newItem.path && oldItem.name == newItem.name
            }
        }
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_row_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: SearchRecyclerViewAdapter.ViewHolder, position: Int) {
        val item = getItem(position)

        holder.fileNameTV.text = item.name
        holder.filePathTV.text = item.path.substring(0, item.path.lastIndexOf("/"))

        holder.colorView.setBackgroundColor(getRandomColor(holder.colorView.context))

        val colorPreference =
            (AppConfig.getInstance().mainActivityContext as MainActivity).currentColorPreference

        if (item.isDirectory) {
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

                val item = getItem(adapterPosition)

                if (!item.isDirectory) {
                    item.openFile(
                        AppConfig.getInstance().mainActivityContext as MainActivity?,
                        false
                    )
                } else {
                    (AppConfig.getInstance().mainActivityContext as MainActivity?)
                        ?.goToMain(item.path)
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
