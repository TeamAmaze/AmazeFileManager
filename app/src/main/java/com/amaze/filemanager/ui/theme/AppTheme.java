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

import java.util.Calendar;

import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.PowerManager;

import androidx.preference.PreferenceManager;

/** This enum represents the theme of the app (LIGHT or DARK) */
public enum AppTheme {
  LIGHT(0, true),
  DARK(1, false),
  TIMED(2, true),
  BLACK(3, false),
  SYSTEM(4, true);

  public static final int LIGHT_INDEX = 0;
  public static final int DARK_INDEX = 1;
  public static final int TIME_INDEX = 2;
  public static final int BLACK_INDEX = 3;
  public static final int SYSTEM_INDEX = 4;

  private int id;

  /**
   * Specifies if the theme can be light in some situations. Used for the "Follow battery saver"
   * option.
   */
  private final boolean canBeLight;

  AppTheme(int id, boolean canBeLight) {
    this.id = id;
    this.canBeLight = canBeLight;
  }

  /**
   * Returns the correct AppTheme. If index == TIME_INDEX, TIMED is returned.
   *
   * @param index The theme index
   * @return The AppTheme for the given index
   */
  public static AppTheme getTheme(int index) {
    switch (index) {
      default:
      case LIGHT_INDEX:
        return LIGHT;
      case DARK_INDEX:
        return DARK;
      case TIME_INDEX:
        return TIMED;
      case BLACK_INDEX:
        return BLACK;
      case SYSTEM_INDEX:
        return SYSTEM;
    }
  }

  public Theme getMaterialDialogTheme(Context context) {
    AppTheme simpleTheme = getSimpleTheme(context);
    switch (simpleTheme.id) {
      default:
      case LIGHT_INDEX:
        return Theme.LIGHT;
      case DARK_INDEX:
      case BLACK_INDEX:
        return Theme.DARK;
    }
  }

  /**
   * Returns the correct AppTheme. If index == TIME_INDEX, current time is used to select the theme.
   *
   * @return The AppTheme for the given index
   */
  public AppTheme getSimpleTheme(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean followBatterySaver =
        preferences.getBoolean(PreferencesConstants.FRAGMENT_FOLLOW_BATTERY_SAVER, false);
    return getSimpleTheme(isNightMode(context), followBatterySaver && isBatterySaverMode(context));
  }

  public AppTheme getSimpleTheme(boolean isNightMode, boolean isBatterySaver) {
    if (this.canBeLight() && isBatterySaver) {
      return DARK;
    }

    switch (id) {
      default:
      case LIGHT_INDEX:
        return LIGHT;
      case DARK_INDEX:
        return DARK;
      case TIME_INDEX:
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
          return DARK;
        } else {
          return LIGHT;
        }
      case BLACK_INDEX:
        return BLACK;
      case SYSTEM_INDEX:
        return isNightMode ? DARK : LIGHT;
    }
  }

  public int getId() {
    return id;
  }

  /** Returns if the theme can be light in some situations */
  public boolean canBeLight() {
    return this.canBeLight;
  }

  private static boolean isNightMode(Context context) {
    return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
        == Configuration.UI_MODE_NIGHT_YES;
  }

  /**
   * Checks if battery saver mode is on
   *
   * @param context The context in which the battery saver mode should be checked
   * @return Whether battery saver mode is on or off
   */
  private static boolean isBatterySaverMode(Context context) {
    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return powerManager.isPowerSaveMode();
    } else {
      return false;
    }
  }
}
