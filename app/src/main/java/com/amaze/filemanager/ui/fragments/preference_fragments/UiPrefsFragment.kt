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

import android.os.Bundle
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R

class UiPrefsFragment : BasePrefsFragment() {
    override val title = R.string.ui

    private var dragAndDropPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.ui_prefs, rootKey)

        findPreference<Preference>("sidebar_bookmarks")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity.pushFragment(BookmarksPrefsFragment())
            true
        }

        findPreference<Preference>("sidebar_quick_access")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity.pushFragment(QuickAccessesPrefsFragment())
            true
        }

        val dragToMoveArray = resources.getStringArray(R.array.dragAndDropPreference)
        dragAndDropPref = findPreference(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE)
        updateDragAndDropPreferenceSummary()
        dragAndDropPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val dragDialogBuilder = MaterialDialog.Builder(activity)
            dragDialogBuilder.theme(
                    activity.utilsProvider.appTheme.getMaterialDialogTheme(requireContext()))
            dragDialogBuilder.title(R.string.drag_and_drop_preference)
            val currentDragPreference: Int = activity.prefs.getInt(
                    PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE,
                    PreferencesConstants.PREFERENCE_DRAG_DEFAULT)
            dragDialogBuilder
                    .items(*dragToMoveArray)
                    .itemsCallbackSingleChoice(currentDragPreference)
                    { dialog, _, which, _ ->
                        val editor = activity.prefs.edit()
                        editor.putInt(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE, which)
                        editor.putString(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_REMEMBERED, null)
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
        val value = activity.prefs.getInt(PreferencesConstants.PREFERENCE_DRAG_AND_DROP_PREFERENCE, PreferencesConstants.PREFERENCE_DRAG_DEFAULT)
        val dragToMoveArray = resources.getStringArray(R.array.dragAndDropPreference)
        dragAndDropPref?.summary = dragToMoveArray[value]
    }
}