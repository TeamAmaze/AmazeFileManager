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

package com.amaze.filemanager.ui.theme;

import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;

import android.content.SharedPreferences;

/** Saves and restores the AppTheme */
public class AppThemeManager {
  private SharedPreferences preferences;
  private AppTheme appTheme;

  public AppThemeManager(SharedPreferences preferences) {
    this.preferences = preferences;
    String themeId = preferences.getString(PreferencesConstants.FRAGMENT_THEME, "0");
    appTheme = AppTheme.getTheme(Integer.parseInt(themeId)).getSimpleTheme();
  }

  /** @return The current Application theme */
  public AppTheme getAppTheme() {
    return appTheme.getSimpleTheme();
  }

  /**
   * Change the current theme of the application. The change is saved.
   *
   * @param appTheme The new theme
   * @return The theme manager.
   */
  public AppThemeManager setAppTheme(AppTheme appTheme) {
    this.appTheme = appTheme;
    preferences
        .edit()
        .putString(PreferencesConstants.FRAGMENT_THEME, Integer.toString(appTheme.getId()))
        .apply();
    return this;
  }
}
