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

package com.amaze.filemanager.ui.colors;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.utils.Utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.ColorInt;

/** @author Emmanuel on 24/5/2017, at 18:56. */
public class ColorUtils {

  public static void colorizeIcons(
      Context context, int iconType, GradientDrawable background, @ColorInt int defaultColor) {
    switch (iconType) {
      case Icons.VIDEO:
      case Icons.IMAGE:
        background.setColor(Utils.getColor(context, R.color.video_item));
        break;
      case Icons.AUDIO:
        background.setColor(Utils.getColor(context, R.color.audio_item));
        break;
      case Icons.PDF:
        background.setColor(Utils.getColor(context, R.color.pdf_item));
        break;
      case Icons.CODE:
        background.setColor(Utils.getColor(context, R.color.code_item));
        break;
      case Icons.TEXT:
        background.setColor(Utils.getColor(context, R.color.text_item));
        break;
      case Icons.COMPRESSED:
        background.setColor(Utils.getColor(context, R.color.archive_item));
        break;
      case Icons.APK:
        background.setColor(Utils.getColor(context, R.color.apk_item));
        break;
      case Icons.NOT_KNOWN:
        background.setColor(Utils.getColor(context, R.color.generic_item));
        break;
      default:
        background.setColor(defaultColor);
        break;
    }
  }
}
