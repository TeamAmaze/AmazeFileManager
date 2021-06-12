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

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.provider.UtilitiesProvider
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.Utils

/**
 * Check [com.amaze.filemanager.adapters.RecyclerAdapter]'s doc.
 */
class SpecialViewHolder(
    c: Context,
    view: View,
    utilsProvider: UtilitiesProvider,
    val type: Int,
) : RecyclerView.ViewHolder(view) {
    // each data item is just a string in this case
    private val txtTitle: TextView = view.findViewById(R.id.text)

    companion object {
        const val HEADER_FILES = 0
        const val HEADER_FOLDERS = 1
    }

    init {
        when (type) {
            HEADER_FILES -> txtTitle.setText(R.string.files)
            HEADER_FOLDERS -> txtTitle.setText(R.string.folders)
            else -> throw IllegalStateException(": $type")
        }

        // if(utilsProvider.getAppTheme().equals(AppTheme.DARK))
        //    view.setBackgroundResource(R.color.holo_dark_background);
        if (utilsProvider.appTheme == AppTheme.LIGHT) {
            txtTitle.setTextColor(Utils.getColor(c, R.color.text_light))
        } else {
            txtTitle.setTextColor(Utils.getColor(c, R.color.text_dark))
        }
    }
}
