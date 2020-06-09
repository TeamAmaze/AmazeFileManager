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

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.CircularColorsView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * This is the external notification that shows some text and a CircularColorsView.
 *
 * @author Emmanuel on 6/10/2017, at 15:36.
 */
public class SelectedColorsPreference extends DialogPreference {

  private int[] colors = {
    Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT
  };
  private int backgroundColor;
  private int visibility = View.VISIBLE;

  public SelectedColorsPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected View onCreateView(ViewGroup parent) {
    setWidgetLayoutResource(R.layout.selectedcolors_preference);
    return super.onCreateView(parent);
  }

  @SuppressLint("WrongConstant")
  @Override
  protected void onBindView(View view) {
    super.onBindView(view); // Keep this before things that need changing what's on screen

    CircularColorsView colorsView = view.findViewById(R.id.colorsection);
    colorsView.setColors(colors[0], colors[1], colors[2], colors[3]);
    colorsView.setDividerColor(backgroundColor);
    colorsView.setVisibility(visibility);
  }

  public void setColorsVisibility(int visibility) {
    this.visibility = visibility;
    notifyChanged();
  }

  public void setDividerColor(int color) {
    backgroundColor = color;
  }

  public void setColors(int color, int color1, int color2, int color3) {
    colors = new int[] {color, color1, color2, color3};
    notifyChanged();
  }

  public void invalidateColors() {
    notifyChanged();
  }
}
