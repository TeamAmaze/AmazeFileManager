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

package com.amaze.filemanager.ui.views.drawer;

import android.view.MenuItem;
import android.widget.ImageButton;

import androidx.annotation.ColorInt;

/**
 * This manages to set the color of the selected ActionView and unset the ActionView that is not
 * selected anymore
 */
public class ActionViewStateManager {

  private ImageButton lastItemSelected = null;
  private @ColorInt int idleIconColor;
  private @ColorInt int selectedIconColor;

  public ActionViewStateManager(@ColorInt int idleColor, @ColorInt int accentColor) {
    idleIconColor = idleColor;
    selectedIconColor = accentColor;
  }

  public void deselectCurrentActionView() {
    if (lastItemSelected != null) {
      lastItemSelected.setColorFilter(idleIconColor);
      lastItemSelected = null;
    }
  }

  public void selectActionView(MenuItem item) {
    if (lastItemSelected != null) {
      lastItemSelected.setColorFilter(idleIconColor);
    }
    if (item.getActionView() != null) {
      lastItemSelected = (ImageButton) item.getActionView();
      lastItemSelected.setColorFilter(selectedIconColor);
    }
  }
}
