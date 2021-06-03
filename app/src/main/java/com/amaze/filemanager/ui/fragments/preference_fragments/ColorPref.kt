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

package com.amaze.filemanager.ui.fragments.preference_fragments

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorInt
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.amaze.filemanager.adapters.ColorAdapter
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.DialogGridBinding
import com.amaze.filemanager.ui.activities.PreferencesActivity
import com.amaze.filemanager.ui.colors.ColorPreference
import com.amaze.filemanager.ui.colors.UserColorPreferences
import com.amaze.filemanager.ui.dialogs.ColorPickerDialog
import com.amaze.filemanager.ui.views.preference.InvalidablePreferenceCategory
import com.amaze.filemanager.ui.views.preference.SelectedColorsPreference

/**
 * This class uses two sections, so that there doesn't need to be two different Fragments. For
 * sections info check switchSections() below.
 *
 *
 * Created by Arpit on 21-06-2015 edited by Emmanuel Messulam <emmanuelbendavid></emmanuelbendavid>@gmail.com>
 */
@Suppress("TooManyFunctions")
class ColorPref : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private var currentSection = SECTION_0
    private var dialog: MaterialDialog? = null
    private var sharedPref: SharedPreferences? = null

    private val activity: PreferencesActivity
        get() = requireActivity() as PreferencesActivity

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        if (savedInstanceState == null) {
            loadSection0()
            reloadListeners()
        } else {
            onRestoreInstanceState(savedInstanceState)
            activity.supportFragmentManager.findFragmentByTag(KEY_SELECT_COLOR_CONFIG)?.apply {
                (this as ColorPickerDialog).setListener(createColorPickerDialogListener())
            }
        }
    }

    override fun onPause() {
        dialog?.dismiss()
        super.onPause()
    }

    /** Deal with the "up" button going to last fragment, instead of section 0.  */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home && currentSection != SECTION_0) {
            switchSections()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Handle back button behaviour, returns to first section of colour preferences as necessary
     */
    fun onBackPressed(): Boolean {
        return if (currentSection != SECTION_0) {
            switchSections()
            true // dealt with click
        } else {
            false
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_COLOREDNAV -> activity.invalidateNavBar()
            PreferencesConstants.PREFERENCE_SKIN,
            PreferencesConstants.PREFERENCE_SKIN_TWO,
            PreferencesConstants.PREFERENCE_ACCENT,
            PreferencesConstants.PREFERENCE_ICON_SKIN -> {
                colorChangeDialog(preference.key)
                return false
            }
            KEY_SELECT_COLOR_CONFIG -> {
                switchSections()
                return true
            }
        }
        return false
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is SelectedColorsPreference) {
            openSelectColorDialog(preference)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun colorChangeDialog(colorPrefKey: String) {
        val userColorPreferences = activity.currentColorPreference
        if (userColorPreferences != null) {
            @ColorInt val currentColor = when (colorPrefKey) {
                PreferencesConstants.PREFERENCE_SKIN -> userColorPreferences.primaryFirstTab
                PreferencesConstants.PREFERENCE_SKIN_TWO -> userColorPreferences.primarySecondTab
                PreferencesConstants.PREFERENCE_ACCENT -> userColorPreferences.accent
                PreferencesConstants.PREFERENCE_ICON_SKIN -> userColorPreferences.iconSkin
                else -> 0
            }

            val adapter = ColorAdapter(
                getActivity(),
                ColorPreference.availableColors,
                currentColor
            ) { selectedColor: Int ->
                @ColorInt var primaryFirst = userColorPreferences.primaryFirstTab
                @ColorInt var primarySecond = userColorPreferences.primarySecondTab
                @ColorInt var accent = userColorPreferences.accent
                @ColorInt var iconSkin = userColorPreferences.iconSkin
                when (colorPrefKey) {
                    PreferencesConstants.PREFERENCE_SKIN -> primaryFirst = selectedColor
                    PreferencesConstants.PREFERENCE_SKIN_TWO -> primarySecond = selectedColor
                    PreferencesConstants.PREFERENCE_ACCENT -> accent = selectedColor
                    PreferencesConstants.PREFERENCE_ICON_SKIN -> iconSkin = selectedColor
                }
                activity
                    .colorPreference
                    .saveColorPreferences(
                        sharedPref,
                        UserColorPreferences(primaryFirst, primarySecond, accent, iconSkin)
                    )
                dialog?.dismiss()
                invalidateEverything()
            }
            val v = DialogGridBinding.inflate(LayoutInflater.from(requireContext())).root.also {
                it.adapter = adapter
                it.onItemClickListener = adapter
            }
            val fab_skin = activity.accent
            dialog = MaterialDialog.Builder(activity)
                .positiveText(com.amaze.filemanager.R.string.cancel)
                .title(com.amaze.filemanager.R.string.choose_color)
                .theme(activity.appTheme.materialDialogTheme)
                .autoDismiss(true)
                .positiveColor(fab_skin)
                .neutralColor(fab_skin)
                .neutralText(com.amaze.filemanager.R.string.default_string)
                .onNeutral { _, _ ->
                    activity.setRestartActivity()
                    activity
                        .colorPreference
                        .saveColorPreferences(sharedPref, userColorPreferences)
                    invalidateEverything()
                }.customView(v, false)
                .show()
        }
    }

    private fun switchSections() {
        preferenceScreen.removeAll()
        if (currentSection == SECTION_0) {
            currentSection = SECTION_1
            loadSection1()
        } else if (currentSection == SECTION_1) {
            currentSection = SECTION_0
            loadSection0()
        }
        reloadListeners()
    }

    private fun loadSection0() {
        if ((getActivity() as PreferencesActivity?)!!.restartActivity) {
            (getActivity() as PreferencesActivity?)!!.restartActivity(getActivity())
        }
        addPreferencesFromResource(com.amaze.filemanager.R.xml.color_prefs)
        if (Build.VERSION.SDK_INT >= 21) {
            findPreference<Preference>(KEY_COLOREDNAV)!!.isEnabled = true
        }
    }

    private fun loadSection1() {
        addPreferencesFromResource(com.amaze.filemanager.R.xml.conficolor_prefs)
        findPreference<SelectedColorsPreference>(KEY_PRESELECTED_CONFIGS).let { selectedColors ->
            invalidateColorPreference(selectedColors)
            selectedColors?.onPreferenceClickListener = this
        }
        checkCustomization()
        (findPreference<Preference>(KEY_CUSTOMIZE) as InvalidablePreferenceCategory?)
            ?.invalidate(activity.accent)
    }

    private fun openSelectColorDialog(pref: SelectedColorsPreference) {
        val dialog = ColorPickerDialog.newInstance(
            pref.key,
            activity.currentColorPreference,
            activity.appTheme
        )
        dialog.setListener(createColorPickerDialogListener())
        (findPreference<Preference>(KEY_CUSTOMIZE) as InvalidablePreferenceCategory?)
            ?.invalidate(activity.accent)
        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, KEY_SELECT_COLOR_CONFIG)
    }

    private fun createColorPickerDialogListener() = ColorPickerDialog.OnAcceptedConfig {
        activity.setRestartActivity()
        checkCustomization()
        invalidateEverything()
        sharedPref!!.getInt(
            PreferencesConstants.PREFERENCE_COLOR_CONFIG,
            ColorPickerDialog.NO_DATA
        ).let { colorPickerPref ->
            if (colorPickerPref == ColorPickerDialog.RANDOM_INDEX) {
                AppConfig.toast(getActivity(), com.amaze.filemanager.R.string.set_random)
            }
        }
    }

    private fun checkCustomization() {
        (
            sharedPref!!.getInt(
                PreferencesConstants.PREFERENCE_COLOR_CONFIG,
                ColorPickerDialog.NO_DATA
            ) == ColorPickerDialog.CUSTOM_INDEX
            ).apply {
            findPreference<Preference>(PreferencesConstants.PREFERENCE_SKIN)?.isEnabled =
                this
            findPreference<Preference>(PreferencesConstants.PREFERENCE_SKIN_TWO)?.isEnabled =
                this
            findPreference<Preference>(PreferencesConstants.PREFERENCE_ACCENT)?.isEnabled =
                this
            findPreference<Preference>(PreferencesConstants.PREFERENCE_ICON_SKIN)?.isEnabled =
                this
        }
    }

    private fun reloadListeners() {
        for (
            PREFERENCE_KEY in if (currentSection == SECTION_0) {
                PREFERENCE_KEYS_SECTION_0
            } else {
                PREFERENCE_KEYS_SECTION_1
            }
        ) {
            findPreference<Preference>(PREFERENCE_KEY)!!.onPreferenceClickListener = this
        }
    }

    private fun invalidateEverything() {
        activity.invalidateRecentsColorAndIcon()
        activity.invalidateToolbarColor()
        activity.invalidateNavBar()
        if (currentSection == SECTION_1) {
            findPreference<SelectedColorsPreference>(KEY_PRESELECTED_CONFIGS).let {
                selectedColors ->
                if (selectedColors != null) {
                    invalidateColorPreference(selectedColors)
                    selectedColors.invalidateColors()
                }
            }
            (findPreference<Preference>(KEY_CUSTOMIZE) as InvalidablePreferenceCategory?)
                ?.invalidate(activity.accent)
        }
    }

    private fun invalidateColorPreference(selectedColors: SelectedColorsPreference?) {
        val colorPickerPref = sharedPref!!.getInt(
            PreferencesConstants.PREFERENCE_COLOR_CONFIG,
            ColorPickerDialog.NO_DATA
        )
        val isColor = (
            colorPickerPref != ColorPickerDialog.CUSTOM_INDEX &&
                colorPickerPref != ColorPickerDialog.RANDOM_INDEX
            )
        if (isColor) {
            selectedColors!!.setColorsVisibility(View.VISIBLE)
            val userColorPreferences = activity.currentColorPreference
            selectedColors.setColors(
                userColorPreferences.primaryFirstTab,
                userColorPreferences.primarySecondTab,
                userColorPreferences.accent,
                userColorPreferences.iconSkin
            )
            if (activity.appTheme.materialDialogTheme == Theme.LIGHT) {
                selectedColors.setDividerColor(Color.WHITE)
            } else {
                selectedColors.setDividerColor(Color.BLACK)
            }
        } else {
            selectedColors!!.setColorsVisibility(View.GONE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SECTION, currentSection)
    }

    private fun onRestoreInstanceState(inState: Bundle) {
        currentSection = inState.getInt(KEY_SECTION, SECTION_0)
        if (currentSection == SECTION_0) {
            loadSection0()
            reloadListeners()
        } else {
            loadSection1()
            reloadListeners()
        }
    }

    companion object {
        private const val SECTION_0 = 0
        private const val SECTION_1 = 1
        private const val KEY_PRESELECTED_CONFIGS = "preselectedconfigs"
        private const val KEY_COLOREDNAV = "colorednavigation"
        private const val KEY_SELECT_COLOR_CONFIG = "selectcolorconfig"
        private val PREFERENCE_KEYS_SECTION_0 = arrayOf(KEY_COLOREDNAV, KEY_SELECT_COLOR_CONFIG)
        private val PREFERENCE_KEYS_SECTION_1 = arrayOf(
            KEY_PRESELECTED_CONFIGS,
            PreferencesConstants.PREFERENCE_SKIN,
            PreferencesConstants.PREFERENCE_SKIN_TWO,
            PreferencesConstants.PREFERENCE_ACCENT,
            PreferencesConstants.PREFERENCE_ICON_SKIN
        )
        private const val KEY_SECTION = "section"
        private const val KEY_CUSTOMIZE = "category"
    }
}
