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
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.views.ThemedTextView

/**
 * Check RecyclerAdapter's doc. TODO load everything related to this item here instead of in
 * RecyclerAdapter.
 */
class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    // each data item is just a string in this case
    @JvmField
    val pictureIcon: ImageView? = view.findViewById(R.id.picture_icon)

    @JvmField
    val genericIcon: ImageView = view.findViewById(R.id.generic_icon)

    @JvmField
    val apkIcon: ImageView? = view.findViewById(R.id.apk_icon)

    @JvmField
    val imageView1: ImageView? = view.findViewById(R.id.icon_thumb)

    @JvmField
    val txtTitle: ThemedTextView = view.findViewById(R.id.firstline)

    @JvmField
    val txtDesc: TextView = view.findViewById(R.id.secondLine)

    @JvmField
    val date: TextView = view.findViewById(R.id.date)

    @JvmField
    val perm: TextView = view.findViewById(R.id.permis)

    @JvmField
    val rl: View = view.findViewById(R.id.second)

    @JvmField
    val genericText: TextView? = view.findViewById(R.id.generictext)

    @JvmField
    val about: ImageButton = view.findViewById(R.id.properties)

    @JvmField
    val checkImageView: ImageView? = view.findViewById(R.id.check_icon)

    @JvmField
    val checkImageViewGrid: ImageView? = view.findViewById(R.id.check_icon_grid)

    @JvmField
    val iconLayout: RelativeLayout? = view.findViewById(R.id.icon_frame_grid)

    @JvmField
    val dummyView: View? = view.findViewById(R.id.dummy_view)
}
