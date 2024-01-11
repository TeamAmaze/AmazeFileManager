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

package com.amaze.filemanager.ui.fragments.preferencefragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.utils.getLangPreferenceDropdownEntries

class UiPrefsFragment : BasePrefsFragment() {

    override val title = R.string.ui

    private var dragAndDropPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.ui_prefs, rootKey)

        findPreference<Preference>("sidebar_bookmarks")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(BookmarksPrefsFragment())
                true
            }

        findPreference<Preference>("sidebar_quick_access")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                activity.pushFragment(QuickAccessesPrefsFragment())
                true
            }

        findPreference<Preference>(PreferencesConstants.PREFERENCE_LANGUAGE)?.apply {
            val availableLocales = requireContext().getLangPreferenceDropdownEntries()
            val currentLanguagePreference = AppCompatDelegate.getApplicationLocales().let {
                if (AppCompatDelegate.getApplicationLocales() ==
                    LocaleListCompat.getEmptyLocaleList()
                ) {
                    0
                } else {
                    availableLocales.values.indexOf(
                        AppCompatDelegate.getApplicationLocales()[0]
                    ) + 1
                }
            }
            this.summary = if (currentLanguagePreference == 0) {
                getString(R.string.preference_language_system_default)
            } else {
                availableLocales.entries.find {
                    it.value == AppCompatDelegate.getApplicationLocales()[0]
                }?.key
            }
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialDialog.Builder(activity).apply {
                    theme(activity.utilsProvider.appTheme.materialDialogTheme)
                    title(R.string.preference_language_dialog_title)
                    items(
                        arrayOf(getString(R.string.preference_language_system_default))
                            .plus(availableLocales.keys.toTypedArray())
                            .toSet()
                    )
                    itemsCallbackSingleChoice(currentLanguagePreference) {
                            dialog, _, _, textLabel ->
                        if (textLabel == getString(R.string.preference_language_system_default)) {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.getEmptyLocaleList()
                            )
                        } else {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.create(availableLocales[textLabel])
                            )
                        }
                        dialog.dismiss()
                        true
                    }
                }.show()
                true
            }
        }

        val dragToMoveArray = resources.getStringArray(R.array.dragAndDropPreference)
        dragAndDropPref = findPreference(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE)
        updateDragAndDropPreferenceSummary()
        dragAndDropPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val dragDialogBuilder = MaterialDialog.Builder(activity)
            dragDialogBuilder.theme(
                activity.utilsProvider.appTheme.getMaterialDialogTheme()
            )
            dragDialogBuilder.title(R.string.drag_and_drop_preference)
            val currentDragPreference: Int = activity.prefs.getInt(
                PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
                PreferencesConstants.PREFERENCE_DRAG_DEFAULT
            )
            dragDialogBuilder
                .items(R.array.dragAndDropPreference)
                .itemsCallbackSingleChoice(currentDragPreference) { dialog, _, which, _ ->
                    val editor = activity.prefs.edit()
                    editor.putInt(
                        PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
                        which
                    )
                    editor.putString(
                        PreferencesConstants.PREFERENCE_DRAG_AND_DROP_REMEMBERED,
                        null
                    )
                    editor.apply()
                    dialog.dismiss()
                    updateDragAndDropPreferenceSummary()
                    true
                }
            dragDialogBuilder.build().show()
            true
        }
    }

    private fun updateDragAndDropPreferenceSummary() {
        val value = activity.prefs.getInt(
            PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
            PreferencesConstants.PREFERENCE_DRAG_DEFAULT
        )
        val dragToMoveArray = resources.getStringArray(R.array.dragAndDropPreference)
        dragAndDropPref?.summary = dragToMoveArray[value]
    }
}
