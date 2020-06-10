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

import com.amaze.filemanager.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.widget.EditText;

import androidx.appcompat.widget.AppCompatEditText;

/**
 * Created by vishal on 20/2/17.
 *
 * <p>Use this class when dealing with {@link androidx.appcompat.widget.AppCompatEditText} for it's
 * color states for different user interactions
 */
public class EditTextColorStateUtil {

  public static void setTint(Context context, EditText editText, int color) {
    if (Build.VERSION.SDK_INT >= 21) return;
    ColorStateList editTextColorStateList = createEditTextColorStateList(context, color);
    if (editText instanceof AppCompatEditText) {
      ((AppCompatEditText) editText).setSupportBackgroundTintList(editTextColorStateList);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      editText.setBackgroundTintList(editTextColorStateList);
    }
  }

  private static ColorStateList createEditTextColorStateList(Context context, int color) {
    int[][] states = new int[3][];
    int[] colors = new int[3];
    int i = 0;
    states[i] = new int[] {-android.R.attr.state_enabled};
    colors[i] = Utils.getColor(context, R.color.text_disabled);
    i++;
    states[i] = new int[] {-android.R.attr.state_pressed, -android.R.attr.state_focused};
    colors[i] = Utils.getColor(context, R.color.grey);
    i++;
    states[i] = new int[] {};
    colors[i] = color;
    return new ColorStateList(states, colors);
  }
}
