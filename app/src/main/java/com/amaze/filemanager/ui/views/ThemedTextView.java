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

import org.jetbrains.annotations.NotNull;

import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.Utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created by vishal on 18/1/17.
 *
 * <p>Class sets text color based on current theme, without explicit method call in app lifecycle To
 * be used only under themed activity context
 */
public class ThemedTextView extends AppCompatTextView {

  public ThemedTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTextViewColor(this, context);
  }

  public static void setTextViewColor(@NotNull TextView textView, @NonNull Context context) {
    if (((MainActivity) context).getAppTheme().equals(AppTheme.LIGHT)) {
      textView.setTextColor(Utils.getColor(context, android.R.color.black));
    } else if (((MainActivity) context).getAppTheme().equals(AppTheme.DARK)
        || ((MainActivity) context).getAppTheme().equals(AppTheme.BLACK)) {
      textView.setTextColor(Utils.getColor(context, android.R.color.white));
    }
  }
}
