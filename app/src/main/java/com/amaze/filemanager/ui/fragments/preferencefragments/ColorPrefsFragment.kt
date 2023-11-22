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
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.ColorAdapter
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.DialogGridBinding
import com.amaze.filemanager.ui.colors.ColorPreference
import com.amaze.filemanager.ui.colors.UserColorPreferences
import com.amaze.filemanager.ui.dialogs.ColorPickerDialog

class ColorPrefsFragment : BasePrefsFragment() {
    override val title = R.string.color_title

    private var dialog: MaterialDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.color_prefs, rootKey)

        val showColorChangeDialogListener = Preference.OnPreferenceClickListener {
            showColorChangeDialog(it.key)

            true
        }

        val colorPickerPref = activity.prefs.getInt(
            PreferencesConstants.PREFERENCE_COLOR_CONFIG,
            ColorPickerDialog.NO_DATA
        )

        val skin = findPreference<Preference>(PreferencesConstants.PREFERENCE_SKIN)
        val skinTwo = findPreference<Preference>(PreferencesConstants.PREFERENCE_SKIN_TWO)
        val accent = findPreference<Preference>(PreferencesConstants.PREFERENCE_ACCENT)
        val icon = findPreference<Preference>(PreferencesConstants.PREFERENCE_ICON_SKIN)

        if (colorPickerPref != ColorPickerDialog.CUSTOM_INDEX) {
            skin?.isEnabled = false
            skinTwo?.isEnabled = false
            accent?.isEnabled = false
            icon?.isEnabled = false
        } else {
            skin?.onPreferenceClickListener = showColorChangeDialogListener
            skinTwo?.onPreferenceClickListener = showColorChangeDialogListener
            accent?.onPreferenceClickListener = showColorChangeDialogListener
            icon?.onPreferenceClickListener = showColorChangeDialogListener
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == PreferencesConstants.PRESELECTED_CONFIGS) {
            showPreselectedColorsConfigDialog()
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun showPreselectedColorsConfigDialog() {
        val newDialog = ColorPickerDialog.newInstance(
            PreferencesConstants.PRESELECTED_CONFIGS,
            activity.currentColorPreference,
            activity.appTheme
        )
        newDialog.setListener {
            val colorPickerPref = activity.prefs.getInt(
                PreferencesConstants.PREFERENCE_COLOR_CONFIG,
                ColorPickerDialog.NO_DATA
            )
            if (colorPickerPref == ColorPickerDialog.RANDOM_INDEX) {
                AppConfig.toast(getActivity(), R.string.set_random)
            }

            activity.recreate()
        }
        newDialog.setTargetFragment(this, 0)
        newDialog.show(parentFragmentManager, PreferencesConstants.PREFERENCE_SELECT_COLOR_CONFIG)
    }

    private fun showColorChangeDialog(colorPrefKey: String) {
        val currentColorPreference = activity.currentColorPreference ?: return

        @ColorInt val currentColor = when (colorPrefKey) {
            PreferencesConstants.PREFERENCE_SKIN -> currentColorPreference.primaryFirstTab
            PreferencesConstants.PREFERENCE_SKIN_TWO -> currentColorPreference.primarySecondTab
            PreferencesConstants.PREFERENCE_ACCENT -> currentColorPreference.accent
            PreferencesConstants.PREFERENCE_ICON_SKIN -> currentColorPreference.iconSkin
            else -> 0
        }

        val adapter = ColorAdapter(
            activity,
            ColorPreference.availableColors,
            currentColor
        ) { selectedColor: Int ->
            @ColorInt var primaryFirst = currentColorPreference.primaryFirstTab

            @ColorInt var primarySecond = currentColorPreference.primarySecondTab

            @ColorInt var accent = currentColorPreference.accent

            @ColorInt var iconSkin = currentColorPreference.iconSkin
            when (colorPrefKey) {
                PreferencesConstants.PREFERENCE_SKIN -> primaryFirst = selectedColor
                PreferencesConstants.PREFERENCE_SKIN_TWO -> primarySecond = selectedColor
                PreferencesConstants.PREFERENCE_ACCENT -> accent = selectedColor
                PreferencesConstants.PREFERENCE_ICON_SKIN -> iconSkin = selectedColor
            }
            activity
                .colorPreference
                .saveColorPreferences(
                    activity.prefs,
                    UserColorPreferences(primaryFirst, primarySecond, accent, iconSkin)
                )
            dialog?.dismiss()
            activity.recreate()
        }

        val v = DialogGridBinding.inflate(LayoutInflater.from(requireContext())).root.also {
            it.adapter = adapter
            it.onItemClickListener = adapter
        }

        val fabSkin = activity.accent

        dialog = MaterialDialog.Builder(activity)
            .positiveText(com.amaze.filemanager.R.string.cancel)
            .title(com.amaze.filemanager.R.string.choose_color)
            .theme(activity.appTheme.getMaterialDialogTheme())
            .autoDismiss(true)
            .positiveColor(fabSkin)
            .neutralColor(fabSkin)
            .neutralText(com.amaze.filemanager.R.string.default_string)
            .onNeutral { _, _ ->
                activity
                    .colorPreference
                    .saveColorPreferences(activity.prefs, currentColorPreference)
                activity.recreate()
            }.customView(v, false)
            .show()
    }
}
