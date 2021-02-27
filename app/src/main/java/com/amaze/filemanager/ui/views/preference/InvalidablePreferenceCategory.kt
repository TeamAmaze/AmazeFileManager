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

package com.amaze.filemanager.ui.views.preference;

import com.amaze.filemanager.utils.PreferenceUtils;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatTextView;

/** @author Emmanuel on 15/10/2017, at 20:46. */
public class InvalidablePreferenceCategory extends PreferenceCategory {

  private int titleColor;

  public InvalidablePreferenceCategory(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    AppCompatTextView title = view.findViewById(android.R.id.title);
    title.setTextColor(titleColor);
  }

  public void invalidate(@ColorInt int accentColor) {
    titleColor = PreferenceUtils.getStatusColor(accentColor);
    notifyChanged();
  }
}
