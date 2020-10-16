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

/** This enum represents the theme of the app (LIGHT or DARK) */
public enum AppTheme {
  LIGHT(0),
  DARK(1),
  TIMED(2),
  BLACK(3);

  public static final int LIGHT_INDEX = 0;
  public static final int DARK_INDEX = 1;
  public static final int TIME_INDEX = 2;
  public static final int BLACK_INDEX = 3;

  private int id;

  AppTheme(int id) {
    this.id = id;
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
    }
  }

  /**
   * @return The Theme enum to provide to {@link
   *     com.afollestad.materialdialogs.MaterialDialog.Builder}
   */
  public Theme getMaterialDialogTheme() {
    switch (id) {
      default:
      case LIGHT_INDEX:
        return Theme.LIGHT;
      case DARK_INDEX:
      case BLACK_INDEX:
        return Theme.DARK;
      case TIME_INDEX:
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
          return Theme.DARK;
        } else {
          return Theme.LIGHT;
        }
    }
  }

  /**
   * Returns the correct AppTheme. If index == TIME_INDEX, current time is used to select the theme.
   *
   * @return The AppTheme for the given index
   */
  public AppTheme getSimpleTheme() {
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
    }
  }

  public int getId() {
    return id;
  }
}
