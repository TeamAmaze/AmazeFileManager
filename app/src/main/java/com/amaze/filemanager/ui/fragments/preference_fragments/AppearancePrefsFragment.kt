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

package com.amaze.filemanager.ui.fragments.preference_fragments

import android.os.Bundle
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.theme.AppTheme

class AppearancePrefsFragment : BasePrefsFragment() {
    override val title = R.string.appearance

    private var gridColumnPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_prefs, rootKey)

        val themePref = findPreference<Preference>(PreferencesConstants.FRAGMENT_THEME)!!
        val themes = resources.getStringArray(R.array.theme)
        val currentTheme = activity
            .prefs
            .getString(PreferencesConstants.FRAGMENT_THEME, "4")!!
            .toInt()
        themePref.summary = themes[currentTheme]
        themePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val builder = MaterialDialog.Builder(activity)
            builder.items(*themes)
                .itemsCallbackSingleChoice(currentTheme) { dialog, _, which, _ ->
                    val editor = activity.prefs.edit()
                    editor.putString(PreferencesConstants.FRAGMENT_THEME, which.toString())
                    editor.apply()

                    activity.utilsProvider.themeManager.appTheme =
                        AppTheme.getTheme(activity, which)
                    activity.recreate()

                    dialog.dismiss()
                    true
                }
                .title(R.string.theme)
                .build()
                .show()

            true
        }

        findPreference<Preference>(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION)
            ?.let {
                it.isEnabled = true
                it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    activity.invalidateNavBar()

                    true
                }
            }

        val colorPrefs = findPreference<Preference>(
            PreferencesConstants.PREFERENCE_SELECT_COLOR_CONFIG
        )!!
        colorPrefs.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity.pushFragment(ColorPrefsFragment())

            true
        }

        val gridColumnItems = resources.getStringArray(R.array.columns)
        gridColumnPref = findPreference(PreferencesConstants.PREFERENCE_GRID_COLUMNS)
        updateGridColumnSummary()
        gridColumnPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val builder = MaterialDialog.Builder(activity)
            builder.theme(activity.utilsProvider.appTheme.getMaterialDialogTheme(activity))
            builder.title(R.string.gridcolumnno)
            var current = activity
                .prefs
                .getString(PreferencesConstants.PREFERENCE_GRID_COLUMNS, "-1")!!
                .toInt() - 1
            if (current < 0) current = 0
            builder
                .items(*gridColumnItems)
                .itemsCallbackSingleChoice(current) { dialog, _, which, _ ->
                    val editor = activity.prefs.edit()
                    editor.putString(
                        PreferencesConstants.PREFERENCE_GRID_COLUMNS,
                        if (which != 0) gridColumnItems[which] else "-1"
                    )
                    editor.apply()
                    dialog.dismiss()
                    updateGridColumnSummary()
                    true
                }
            builder.build().show()

            true
        }
    }

    private fun updateGridColumnSummary() {
        val gridColumnItems = resources.getStringArray(R.array.columns)

        activity.prefs
            .getString(PreferencesConstants.PREFERENCE_GRID_COLUMNS, "-1")
            ?.let {
                if (it == "-1") {
                    gridColumnPref!!.summary = gridColumnItems[0]
                } else {
                    gridColumnPref!!.summary = it
                }
            }
    }
}
