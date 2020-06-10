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

package com.amaze.filemanager.utils;

import android.app.Activity;
import android.util.DisplayMetrics;

public class ScreenUtils {

  public static final int TOOLBAR_HEIGHT_IN_DP = 128; // 160 dpi

  private Activity activity;

  public ScreenUtils(Activity activity) {
    this.activity = activity;
  }

  public int convertDbToPx(float dp) {
    return Math.round(activity.getResources().getDisplayMetrics().density * dp);
  }

  public int convertPxToDb(float px) {
    return Math.round(px / activity.getResources().getDisplayMetrics().density);
  }

  public int getScreenWidthInPx() {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return displayMetrics.widthPixels;
  }

  public int getScreenHeightInPx() {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    return displayMetrics.heightPixels;
  }

  public int getScreenWidthInDp() {
    return convertPxToDb(getScreenWidthInPx());
  }

  public int getScreeHeightInDb() {
    return convertPxToDb(getScreenHeightInPx());
  }
}
