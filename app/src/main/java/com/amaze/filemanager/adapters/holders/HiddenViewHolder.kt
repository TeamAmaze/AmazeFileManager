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

package com.amaze.filemanager.adapters.holders

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R

/**
 * This is the ViewHolder that formats the hidden files as defined in bookmarkrow.xml.
 *
 * @see com.amaze.filemanager.adapters.HiddenAdapter
 */
class HiddenViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    @JvmField
    val deleteButton: ImageButton = view.findViewById(R.id.delete_button)

    @JvmField
    val textTitle: TextView = view.findViewById(R.id.filename)

    @JvmField
    val textDescription: TextView = view.findViewById(R.id.file_path)

    @JvmField
    val row: LinearLayout = view.findViewById(R.id.bookmarkrow)
}
