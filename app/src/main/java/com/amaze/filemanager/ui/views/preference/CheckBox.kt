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
import android.view.ViewGroup
import android.widget.Switch
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference

/** Created by Arpit on 10/18/2015 edited by Emmanuel Messulam <emmanuelbendavid></emmanuelbendavid>@gmail.com>  */
class CheckBox(context: Context?, attrs: AttributeSet?) : SwitchPreference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        clearListenerInViewGroup(holder?.itemView as ViewGroup)
        super.onBindViewHolder(holder)
    }

    /**
     * Clear listener in Switch for specify ViewGroup.
     *
     * @param viewGroup The ViewGroup that will need to clear the listener.
     */
    private fun clearListenerInViewGroup(viewGroup: ViewGroup?) {
        if (null == viewGroup) {
            return
        }
        for (n in 0 until viewGroup.childCount) {
            val childView = viewGroup.getChildAt(n)
            if (childView is Switch) {
                childView.setOnCheckedChangeListener(null)
                return
            } else if (childView is ViewGroup) {
                clearListenerInViewGroup(childView)
            }
        }
    }
}
