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

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.HybridFileParcelable
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
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val fileNameTV: TextView

        init {

            view.setOnClickListener {
            }

            view.findViewById<View>(R.id.searchItemSampleColorView)
                .setBackgroundColor(getRandomColor())

            fileNameTV = view.findViewById(R.id.searchItemFileNameTV)
        }

        private fun getRandomColor(): Int {
            val colorArray = arrayOf(
                "#e57373", "#f06292", "#ba68c8", "#9575cd", "#7986cb", "#64b5f6", "#4fc3f7",
                "#4dd0e1", "#4db6ac", "#81c784", "#aed581", "#dce775", "#fff176", "#ffd54f",
                "#ffb74d", "#ff8a65", "#a1887f"
            )
            return Color.parseColor(colorArray[Random().nextInt(colorArray.size - 1)])
        }
    }
}
