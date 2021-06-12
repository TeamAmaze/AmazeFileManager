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
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.amaze.filemanager.utils.PreferenceUtils

/** @author Emmanuel on 15/10/2017, at 20:46.
 */
class InvalidablePreferenceCategory(context: Context?, attrs: AttributeSet?) :
    PreferenceCategory(context, attrs) {

    private var titleColor = 0

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val title: AppCompatTextView = holder?.findViewById(android.R.id.title) as AppCompatTextView
        title.setTextColor(titleColor)
    }

    /**
     * notify change of title colour as necessary
     */
    fun invalidate(@ColorInt accentColor: Int) {
        titleColor = PreferenceUtils.getStatusColor(accentColor)
        notifyChanged()
    }
}
