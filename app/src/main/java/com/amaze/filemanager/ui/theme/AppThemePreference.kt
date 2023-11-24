/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.Theme
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import java.util.Calendar

/**
 * This enum represents the theme selected by the user in the appearance preferences.
 *
 * [id] corresponds to the index of the value in the selection dialog in the preferences.
 *
 * [canBeLight] specifies if the theme can be light in some situations. Used for the "Follow battery saver"
 * option.
 */
enum class AppThemePreference(val id: Int, val canBeLight: Boolean) {
    LIGHT(0, true),
    DARK(1, false),
    TIMED(2, true),
    BLACK(3, false),
    SYSTEM(4, true);

    /**
     * Returns the correct [Theme] associated with this [AppThemePreference] based on [context].
     */
    fun getMaterialDialogTheme(context: Context): Theme {
        return getSimpleTheme(context).getMaterialDialogTheme()
    }

    /**
     * Returns the correct [AppTheme]. If this is [AppThemePreference.TIME_INDEX], current time is used to select the theme.
     *
     * @return The [AppTheme] for the given index
     */
    fun getSimpleTheme(context: Context): AppTheme {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val followBatterySaver = preferences.getBoolean(
            PreferencesConstants.FRAGMENT_FOLLOW_BATTERY_SAVER,
            false
        )
        return getSimpleTheme(
            isNightMode(context),
            followBatterySaver && isBatterySaverMode(context)
        )
    }

    /**
     * Returns the correct [AppTheme] based on [isNightMode] and [isBatterySaver].
     */
    fun getSimpleTheme(isNightMode: Boolean, isBatterySaver: Boolean): AppTheme {
        return if (canBeLight && isBatterySaver) {
            AppTheme.DARK
        } else {
            when (this) {
                LIGHT -> AppTheme.LIGHT
                DARK -> AppTheme.DARK
                TIMED -> {
                    val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
                    if (hour <= 6 || hour >= 18) {
                        AppTheme.DARK
                    } else {
                        AppTheme.LIGHT
                    }
                }

                BLACK -> AppTheme.BLACK
                SYSTEM -> if (isNightMode) AppTheme.DARK else AppTheme.LIGHT
            }
        }
    }

    /**
     * Checks if night mode is on using [context]
     */
    private fun isNightMode(context: Context): Boolean {
        return (
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                == Configuration.UI_MODE_NIGHT_YES
            )
    }

    /**
     * Checks if battery saver mode is on using [context]
     */
    private fun isBatterySaverMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }

    companion object {
        const val LIGHT_INDEX = 0
        const val DARK_INDEX = 1
        const val TIME_INDEX = 2
        const val BLACK_INDEX = 3
        const val SYSTEM_INDEX = 4

        /**
         * Returns the correct AppTheme . If [index] == TIME_INDEX, TIMED is returned.
         *
         * @param index The theme index
         * @return The AppTheme for the given index
         */
        @JvmStatic
        fun getTheme(index: Int): AppThemePreference {
            return when (index) {
                LIGHT_INDEX -> LIGHT
                DARK_INDEX -> DARK
                TIME_INDEX -> TIMED
                BLACK_INDEX -> BLACK
                SYSTEM_INDEX -> SYSTEM
                else -> LIGHT
            }
        }
    }
}
