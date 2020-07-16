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

package com.amaze.filemanager.ui.views;

import com.amaze.filemanager.ui.theme.AppTheme;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;

import androidx.annotation.ColorInt;

/**
 * Created by vishal on 30/5/16. Class used to create background of check icon on selection with a
 * Custom {@link Color} and Stroke (boundary)
 */
public class CircleGradientDrawable extends GradientDrawable {

  private static final int STROKE_WIDTH = 2;
  private static final String STROKE_COLOR_LIGHT = "#EEEEEE";
  private static final String STROKE_COLOR_DARK = "#424242";
  private DisplayMetrics mDisplayMetrics;

  /**
   * Constructor
   *
   * @param color the hex color of circular icon
   * @param appTheme current theme light/dark which will determine the boundary color
   * @param metrics to convert the boundary width for {@link #setStroke} method from dp to px
   */
  public CircleGradientDrawable(String color, AppTheme appTheme, DisplayMetrics metrics) {
    this(appTheme, metrics);
    setColor(Color.parseColor(color));
  }

  /**
   * Constructor
   *
   * @param color the color of circular icon
   * @param appTheme current theme light/dark which will determine the boundary color
   * @param metrics to convert the boundary width for {@link #setStroke} method from dp to px
   */
  public CircleGradientDrawable(@ColorInt int color, AppTheme appTheme, DisplayMetrics metrics) {
    this(appTheme, metrics);
    setColor(color);
  }

  public CircleGradientDrawable(AppTheme appTheme, DisplayMetrics metrics) {
    this.mDisplayMetrics = metrics;

    setShape(OVAL);
    setSize(1, 1);
    setStroke(
        dpToPx(STROKE_WIDTH),
        (appTheme.equals(AppTheme.DARK) || appTheme.equals(AppTheme.BLACK))
            ? Color.parseColor(STROKE_COLOR_DARK)
            : Color.parseColor(STROKE_COLOR_LIGHT));
  }

  private int dpToPx(int dp) {
    int px = Math.round(mDisplayMetrics.density * dp);
    return px;
  }
}
