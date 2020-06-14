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

package com.amaze.filemanager.adapters;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.CheckableCircleView;
import com.amaze.filemanager.utils.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

public class ColorAdapter extends ArrayAdapter<Integer> implements AdapterView.OnItemClickListener {

  private LayoutInflater inflater;
  private @ColorInt int selectedColor;
  private OnColorSelected onColorSelected;

  /**
   * Constructor for adapter that handles the view creation of color chooser dialog in preferences
   *
   * @param context the context
   * @param colors array list of color hex values in form of string; for the views
   * @param selectedColor currently selected color
   * @param l OnColorSelected listener for when a color is selected
   */
  public ColorAdapter(
      Context context, Integer[] colors, @ColorInt int selectedColor, OnColorSelected l) {
    super(context, R.layout.rowlayout, colors);
    this.selectedColor = selectedColor;
    this.onColorSelected = l;

    inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @ColorRes
  private int getColorResAt(int position) {
    return getItem(position);
  }

  @NonNull
  @Override
  public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
    CheckableCircleView colorView;
    if (convertView != null && convertView instanceof CheckableCircleView) {
      colorView = (CheckableCircleView) convertView;
    } else {
      colorView = (CheckableCircleView) inflater.inflate(R.layout.dialog_grid_item, parent, false);
    }

    @ColorInt int color = Utils.getColor(getContext(), getColorResAt(position));

    colorView.setChecked(color == selectedColor);
    colorView.setColor(color);

    return colorView;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    this.selectedColor = Utils.getColor(getContext(), getColorResAt(position));
    ((CheckableCircleView) view).setChecked(true);
    onColorSelected.onColorSelected(this.selectedColor);
  }

  public interface OnColorSelected {
    void onColorSelected(@ColorInt int color);
  }
}
