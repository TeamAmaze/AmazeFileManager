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

import android.graphics.Color;

/** Created by Vishal on 12-05-2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com> */
public class PreferenceUtils {

  public static final int DEFAULT_PRIMARY = 4;
  public static final int DEFAULT_ACCENT = 1;
  public static final int DEFAULT_ICON = -1;
  public static final int DEFAULT_CURRENT_TAB = 1;

  public static int getStatusColor(String skin) {
    return darker(Color.parseColor(skin));
  }

  public static int getStatusColor(int skin) {
    return darker(skin);
  }

  private static int darker(int color) {
    int a = Color.alpha(color);
    int r = Color.red(color);
    int g = Color.green(color);
    int b = Color.blue(color);

    return Color.argb(
        a,
        Math.max((int) (r * 0.6f), 0),
        Math.max((int) (g * 0.6f), 0),
        Math.max((int) (b * 0.6f), 0));
  }
}
