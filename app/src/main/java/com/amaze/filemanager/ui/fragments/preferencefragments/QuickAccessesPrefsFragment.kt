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
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.amaze.filemanager.R
import com.amaze.filemanager.utils.TinyDB

class QuickAccessesPrefsFragment : BasePrefsFragment() {
    override val title = R.string.show_quick_access_pref

    companion object {
        const val KEY = "quick access array"
        val KEYS = arrayOf(
            "fastaccess",
            "recent",
            "image",
            "video",
            "audio",
            "documents",
            "apks"
        )
        val DEFAULT = arrayOf(true, true, true, true, true, true, true)

        val prefPos: Map<String, Int> = KEYS.withIndex().associate {
            Pair(it.value, it.index)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.quickaccess_prefs, rootKey)

        val currentValue = TinyDB.getBooleanArray(activity.prefs, KEY, DEFAULT)!!

        val onChange = Preference.OnPreferenceClickListener { preference ->
            prefPos[preference.key]?.let {
                currentValue[it] = (preference as SwitchPreference).isChecked
                TinyDB.putBooleanArray(activity.prefs, KEY, currentValue)
            }

            true
        }

        for (key in KEYS) {
            findPreference<Preference>(key)?.onPreferenceClickListener = onChange
        }
    }
}
