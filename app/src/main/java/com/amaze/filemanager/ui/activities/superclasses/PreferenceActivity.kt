/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
package com.amaze.filemanager.ui.activities.superclasses

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_BOOKMARKS_ADDED
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_CHANGEPATHS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_COLORIZE_ICONS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_DISABLE_PLAYER_INTENT_FILTERS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_NEED_TO_SET_HOME
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_ROOTMODE
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_ROOT_LEGACY_LISTING
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_DIVIDERS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HEADERS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_THUMB
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_USE_CIRCULAR_IMAGES
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_VIEW
import com.amaze.filemanager.utils.PreferenceUtils

/**
 * @author Emmanuel on 24/8/2017, at 23:13.
 */
open class PreferenceActivity : BasicActivity() {
    private var sharedPrefs: SharedPreferences? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        // Fragments are created before the super call returns, so we must
        // initialize sharedPrefs before the super call otherwise it cannot be used by fragments
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)
    }

    val prefs: SharedPreferences
        get() = sharedPrefs!!

    val isRootExplorer: Boolean
        get() = getBoolean(PREFERENCE_ROOTMODE)

    val currentTab: Int
        get() =
            prefs
                .getInt(
                    PreferencesConstants.PREFERENCE_CURRENT_TAB,
                    PreferenceUtils.DEFAULT_CURRENT_TAB,
                )

    /**
     * Convenience method to [SharedPreferences.getBoolean] for quickly getting user preference flags.
     */
    fun getBoolean(key: String): Boolean {
        val defaultValue =
            when (key) {
                PREFERENCE_SHOW_PERMISSIONS,
                PREFERENCE_SHOW_GOBACK_BUTTON,
                PREFERENCE_SHOW_HIDDENFILES,
                PREFERENCE_BOOKMARKS_ADDED,
                PREFERENCE_ROOTMODE,
                PREFERENCE_COLORED_NAVIGATION,
                PREFERENCE_TEXTEDITOR_NEWSTACK,
                PREFERENCE_CHANGEPATHS,
                PREFERENCE_ROOT_LEGACY_LISTING,
                PREFERENCE_DISABLE_PLAYER_INTENT_FILTERS,
                -> false
                PREFERENCE_SHOW_FILE_SIZE,
                PREFERENCE_SHOW_DIVIDERS,
                PREFERENCE_SHOW_HEADERS,
                PREFERENCE_USE_CIRCULAR_IMAGES,
                PREFERENCE_COLORIZE_ICONS,
                PREFERENCE_SHOW_THUMB,
                PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES,
                PREFERENCE_NEED_TO_SET_HOME,
                PREFERENCE_SHOW_SIDEBAR_FOLDERS,
                PREFERENCE_VIEW,
                PREFERENCE_SHOW_LAST_MODIFIED,
                PREFERENCE_ENABLE_MARQUEE_FILENAME,
                -> true
                else -> throw IllegalArgumentException("Please map '$key'")
            }
        return sharedPrefs!!.getBoolean(key, defaultValue)
    }
}
