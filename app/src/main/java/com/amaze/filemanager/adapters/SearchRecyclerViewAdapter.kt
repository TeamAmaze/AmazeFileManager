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

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.utils.Utils
import java.util.*

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

        holder.dateTV.text = Utils.getDate(holder.itemView.context, item.date)

        if (!item.isDirectory) {
            holder.sizeTV.text = Formatter.formatFileSize(holder.itemView.context, item.size)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val fileNameTV: TextView
        val dateTV: TextView
        val sizeTV: TextView

        init {

            view.setOnClickListener {
            }

            fileNameTV = view.findViewById(R.id.searchItemFileNameTV)
            dateTV = view.findViewById(R.id.searchItemDateTV)
            sizeTV = view.findViewById(R.id.searchItemSizeTV)
        }
    }
}
