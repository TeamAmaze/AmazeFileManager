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
package com.amaze.filemanager.ui.views.appbar

import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity
import com.google.android.material.appbar.AppBarLayout
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants

/**
 * layout_appbar.xml contains the layout for AppBar and BottomBar
 *
 * This is a class containing containing methods to each section of the AppBar, creating the
 * object loads the views.
 */
class AppBar(
        mainActivity: MainActivity,
        sharedPref: SharedPreferences,
        onSearch: (queue: String) -> Unit
) {

    val toolbar: Toolbar = mainActivity.findViewById(R.id.action_bar)
    val searchView: SearchView = SearchView(this, mainActivity, onSearch)
    val bottomBar: BottomBar = BottomBar(this, mainActivity)
    val appbarLayout: AppBarLayout = mainActivity.findViewById(R.id.lin)

    init {
        if (Build.VERSION.SDK_INT >= 21) {
            toolbar.elevation = 0f
        }

        if (!sharedPref.getBoolean(PreferencesConstants.PREFERENCE_INTELLI_HIDE_TOOLBAR, true)) {
            val params = toolbar.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = 0
            appbarLayout.setExpanded(true, true)
        }
    }

    /**
     * Sets the app title, or sets it as empty if null is passed
     */
    fun setTitle(title: String?) {
        toolbar.title = title
    }

    /**
     * Sets the app title
     */
    fun setTitle(@StringRes title: Int) {
        toolbar.setTitle(title)
    }
}