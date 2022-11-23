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

package com.amaze.filemanager.ui.views.preference

import android.content.Context
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.amaze.filemanager.R

/** @author Emmanuel on 17/4/2017, at 22:22.
 */
class PathSwitchPreference(
    context: Context,
    private val onEdit: (PathSwitchPreference) -> Unit,
    private val onDelete: (PathSwitchPreference) -> Unit
) : Preference(context) {
    var lastItemClicked = -1
        private set

    init {
        widgetLayoutResource = R.layout.namepathswitch_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        holder.itemView.let { view ->
            view.findViewById<View>(R.id.edit).setOnClickListener { onEdit(this) }
            view.findViewById<View>(R.id.delete).setOnClickListener { onDelete(this) }
            view.setOnClickListener(null)
        }

        // Keep this before things that need changing what's on screen
        super.onBindViewHolder(holder)
    }
}
