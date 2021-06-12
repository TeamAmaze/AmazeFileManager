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

package com.amaze.filemanager.utils

import android.content.SharedPreferences
import android.text.TextUtils

/**
 * Extract from: https://github.com/kcochibili/TinyDB--Android-Shared-Preferences-Turbo Author:
 * https://github.com/kcochibili
 */
object TinyDB {
    /*
   *  The "‚" character is not a comma, it is the SINGLE LOW-9 QUOTATION MARK. U-201A
   *  + U-2017 + U-201A are used for separating the items in a list.
   */
    private const val DIVIDER = "‚‗‚"

    /**
     * Put array of Boolean into SharedPreferences with 'key' and save
     *
     * @param key SharedPreferences key
     * @param array array of Booleans to be added
     */
    @JvmStatic
    fun putBooleanArray(preferences: SharedPreferences, key: String?, array: Array<Boolean>) {
        preferences.edit().putString(key, TextUtils.join(DIVIDER, array)).apply()
    }

    /**
     * Get parsed array of Booleans from SharedPreferences at 'key'
     *
     * @param key SharedPreferences key
     * @return Array of Booleans
     */
    @JvmStatic
    fun getBooleanArray(
        preferences: SharedPreferences,
        key: String?,
        defaultValue: Array<Boolean>?
    ): Array<Boolean>? {
        val prefValue = preferences.getString(key, "")
        if (prefValue == "") {
            return defaultValue
        }

        return TextUtils.split(prefValue, DIVIDER).map {
            java.lang.Boolean.valueOf(it)
        }.toTypedArray()
    }
}
