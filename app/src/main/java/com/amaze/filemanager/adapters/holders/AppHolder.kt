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

package com.amaze.filemanager.adapters.holders

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.views.ThemedTextView

class AppHolder(view: View) : RecyclerView.ViewHolder(view) {
    @JvmField
    val apkIcon: ImageView = view.findViewById(R.id.apk_icon)

    @JvmField
    val txtTitle: ThemedTextView = view.findViewById(R.id.firstline)

    @JvmField
    val rl: RelativeLayout = view.findViewById(R.id.second)

    @JvmField
    val txtDesc: TextView = view.findViewById(R.id.date)

    @JvmField
    val about: ImageButton = view.findViewById(R.id.properties)

    @JvmField
    val summary: RelativeLayout = view.findViewById(R.id.summary)

    init {
        apkIcon.visibility = View.VISIBLE
        view.findViewById<ImageView>(R.id.picture_icon).visibility = View.GONE
        view.findViewById<ImageView>(R.id.generic_icon).visibility = View.GONE
    }
}
