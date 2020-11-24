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

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.Utils;

import android.content.Context;
import android.util.AttributeSet;

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
    if (((ThemedActivity) AppConfig.getInstance().getThemedActivityContext())
        .getAppTheme()
        .equals(AppTheme.LIGHT)) {
      setTextColor(Utils.getColor(getContext(), android.R.color.black));
    } else if (((ThemedActivity) AppConfig.getInstance().getThemedActivityContext())
            .getAppTheme()
            .equals(AppTheme.DARK)
        || ((ThemedActivity) AppConfig.getInstance().getThemedActivityContext())
            .getAppTheme()
            .equals(AppTheme.BLACK)) {
      setTextColor(Utils.getColor(getContext(), android.R.color.white));
    }
  }
}
