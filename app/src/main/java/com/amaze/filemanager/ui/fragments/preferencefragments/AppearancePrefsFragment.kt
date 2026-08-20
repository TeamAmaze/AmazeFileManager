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

package com.amaze.filemanager.ui.fragments.preferencefragments

import android.os.Bundle
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.fragments.data.MainFragmentViewModel
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_GRID_COLUMNS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_GRID_COLUMNS_DEFAULT
import com.amaze.filemanager.ui.theme.AppThemePreference

class AppearancePrefsFragment : BasePrefsFragment() {
    override val title = R.string.appearance

    private var currentTheme = 0
    private var gridColumnPref: Preference? = null

    private val onClickTheme =
        Preference.OnPreferenceClickListener {
            val builder = MaterialDialog.Builder(activity)
            builder.items(R.array.theme)
                .itemsCallbackSingleChoice(currentTheme) { dialog, _, which, _ ->
                    val editor = activity.prefs.edit()
                    editor.putString(PreferencesConstants.FRAGMENT_THEME, which.toString())
                    editor.apply()

                    activity.utilsProvider.themeManager.setAppThemePreference(
                        AppThemePreference.getTheme(which),
                    )
                    activity.recreate()

                    dialog.dismiss()
                    true
                }
                .title(R.string.theme)
                .build()
                .show()

            true
        }

    private val onClickGridColumn =
        Preference.OnPreferenceClickListener {
            // Offer only the column counts this device can practically show: "Automatic" plus
            // 2..maxGridColumns (2 on phones, up to 4 on tablets).
            val maxColumns = MainFragmentViewModel.maxGridColumns()
            val savedValues =
                listOf(PREFERENCE_GRID_COLUMNS_DEFAULT) + (2..maxColumns).map { it.toString() }
            val labels: List<CharSequence> =
                listOf(getString(R.string.default_string)) + (2..maxColumns).map { it.toString() }

            val saved =
                activity.prefs.getString(PREFERENCE_GRID_COLUMNS, PREFERENCE_GRID_COLUMNS_DEFAULT)
            val current = savedValues.indexOf(saved).coerceAtLeast(0)

            MaterialDialog.Builder(activity)
                .also { builder ->
                    builder.theme(activity.utilsProvider.appTheme.getMaterialDialogTheme())
                    builder.title(R.string.gridcolumnno)
                    builder
                        .items(labels)
                        .itemsCallbackSingleChoice(current) { dialog, _, which, _ ->
                            activity.prefs
                                .edit()
                                .putString(PREFERENCE_GRID_COLUMNS, savedValues[which])
                                .apply()
                            dialog.dismiss()
                            updateGridColumnSummary()
                            true
                        }
                }.build()
                .show()

            true
        }

    private val onClickFollowBatterySaver =
        Preference.OnPreferenceClickListener {
            // recreate the activity since the theme could have changed with this preference change
            activity.recreate()
            true
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.appearance_prefs, rootKey)

        val themePref = findPreference<Preference>(PreferencesConstants.FRAGMENT_THEME)
        val themes = resources.getStringArray(R.array.theme)
        currentTheme =
            activity
                .prefs
                .getString(PreferencesConstants.FRAGMENT_THEME, "4")!!
                .toInt()

        themePref?.summary = themes[currentTheme]
        themePref?.onPreferenceClickListener = onClickTheme

        val batterySaverPref =
            findPreference<Preference>(
                PreferencesConstants.FRAGMENT_FOLLOW_BATTERY_SAVER,
            )

        val currentThemeEnum = AppThemePreference.getTheme(currentTheme)
        batterySaverPref?.isVisible = currentThemeEnum.canBeLight
        batterySaverPref?.onPreferenceClickListener = onClickFollowBatterySaver

        findPreference<Preference>(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION)
            ?.let {
                it.isEnabled = true
                it.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        activity.invalidateNavBar()

                        true
                    }
            }

        findPreference<Preference>(
            PreferencesConstants.PREFERENCE_SELECT_COLOR_CONFIG,
        )?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(ColorPrefsFragment())

                true
            }

        gridColumnPref = findPreference(PREFERENCE_GRID_COLUMNS)
        updateGridColumnSummary()
        gridColumnPref?.onPreferenceClickListener = onClickGridColumn
    }

    private fun updateGridColumnSummary() {
        val preferenceColumns =
            activity.prefs.getString(
                PREFERENCE_GRID_COLUMNS,
                PREFERENCE_GRID_COLUMNS_DEFAULT,
            )
        // Show the effective (device-clamped) column count so it matches what the grid renders.
        val effective =
            (preferenceColumns?.toIntOrNull() ?: PREFERENCE_GRID_COLUMNS_DEFAULT.toInt())
                .coerceAtMost(MainFragmentViewModel.maxGridColumns())
        gridColumnPref?.summary = effective.toString()
    }
}
