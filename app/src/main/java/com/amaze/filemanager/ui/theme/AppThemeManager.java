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

import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;

import android.content.Context;
import android.content.SharedPreferences;

/** Saves and restores the AppTheme */
public class AppThemeManager {
  private SharedPreferences preferences;
  private AppThemePreference appThemePreference;
  private final Context context;

  public AppThemeManager(SharedPreferences preferences, Context context) {
    this.preferences = preferences;
    this.context = context;
    String themeId = preferences.getString(PreferencesConstants.FRAGMENT_THEME, "4");
    appThemePreference = AppThemePreference.getTheme(Integer.parseInt(themeId));
  }

  /**
   * @return The current Application theme
   */
  public AppTheme getAppTheme() {
    return appThemePreference.getSimpleTheme(context);
  }

  /**
   * Change the current theme of the application. The change is saved.
   *
   * @param appThemePreference The new theme
   * @return The theme manager.
   */
  public AppThemeManager setAppThemePreference(AppThemePreference appThemePreference) {
    this.appThemePreference = appThemePreference;
    return this;
  }
}
