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
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.amaze.filemanager.R
import com.amaze.filemanager.utils.TinyDB
import java.util.*

/** @author Emmanuel on 17/4/2017, at 23:17.
 */
class QuickAccessPref : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    companion object {
        const val KEY = "quick access array"
        val KEYS = arrayOf(
            "fastaccess", "recent", "image", "video", "audio", "documents", "apks"
        )
        val DEFAULT = arrayOf(true, true, true, true, true, true, true)
        private var prefPos: Map<String, Int> = HashMap()

        init {
            prefPos = KEYS.withIndex().associate {
                Pair(it.value, it.index)
            }
        }
    }

    private var preferences: SharedPreferences? = null
    private var currentValue: Array<Boolean>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.fastaccess_prefs)
        preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        currentValue = TinyDB.getBooleanArray(preferences!!, KEY, DEFAULT)
        for (i in 0 until preferenceScreen.preferenceCount) {
            preferenceScreen.getPreference(i).onPreferenceClickListener = this
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        currentValue!![prefPos[preference.key]!!] = (preference as SwitchPreference).isChecked
        TinyDB.putBooleanArray(preferences!!, KEY, currentValue!!)
        return true
    }
}
