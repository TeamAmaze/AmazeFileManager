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

package com.amaze.filemanager.ui.activities.superclasses;

import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_BOOKMARKS_ADDED;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_CHANGEPATHS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORIZE_ICONS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_ENABLE_MARQUEE_FILENAME;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_NEED_TO_SET_HOME;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_ROOTMODE;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_ROOT_LEGACY_LISTING;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_DIVIDERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HEADERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_USE_CIRCULAR_IMAGES;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_VIEW;

import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.utils.PreferenceUtils;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

/** @author Emmanuel on 24/8/2017, at 23:13. */
public class PreferenceActivity extends BasicActivity {

  private SharedPreferences sharedPrefs;

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
  }

  public SharedPreferences getPrefs() {
    return sharedPrefs;
  }

  public boolean isRootExplorer() {
    return getBoolean(PREFERENCE_ROOTMODE);
  }

  public int getCurrentTab() {
    return getPrefs()
        .getInt(PreferencesConstants.PREFERENCE_CURRENT_TAB, PreferenceUtils.DEFAULT_CURRENT_TAB);
  }

  public boolean getBoolean(String key) {
    boolean defaultValue;

    switch (key) {
      case PREFERENCE_SHOW_PERMISSIONS:
      case PREFERENCE_SHOW_GOBACK_BUTTON:
      case PREFERENCE_SHOW_HIDDENFILES:
      case PREFERENCE_BOOKMARKS_ADDED:
      case PREFERENCE_ROOTMODE:
      case PREFERENCE_COLORED_NAVIGATION:
      case PREFERENCE_TEXTEDITOR_NEWSTACK:
      case PREFERENCE_CHANGEPATHS:
      case PREFERENCE_ROOT_LEGACY_LISTING:
        defaultValue = false;
        break;
      case PREFERENCE_SHOW_FILE_SIZE:
      case PREFERENCE_SHOW_DIVIDERS:
      case PREFERENCE_SHOW_HEADERS:
      case PREFERENCE_USE_CIRCULAR_IMAGES:
      case PREFERENCE_COLORIZE_ICONS:
      case PREFERENCE_SHOW_THUMB:
      case PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES:
      case PREFERENCE_NEED_TO_SET_HOME:
      case PREFERENCE_SHOW_SIDEBAR_FOLDERS:
      case PREFERENCE_VIEW:
      case PREFERENCE_SHOW_LAST_MODIFIED:
      case PREFERENCE_ENABLE_MARQUEE_FILENAME:
        defaultValue = true;
        break;
      default:
        throw new IllegalArgumentException("Please map \'" + key + "\'");
    }

    return sharedPrefs.getBoolean(key, defaultValue);
  }
}
