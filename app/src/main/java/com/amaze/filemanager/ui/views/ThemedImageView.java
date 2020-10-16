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

import com.amaze.filemanager.ui.activities.superclasses.BasicActivity;
import com.amaze.filemanager.ui.theme.AppTheme;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.util.AttributeSet;

/**
 * Created by vishal on 18/2/17.
 *
 * <p>A custom image view which adds an extra attribute to determine a source image when in material
 * dark preference
 */
public class ThemedImageView extends androidx.appcompat.widget.AppCompatImageView {

  public ThemedImageView(Context context) {
    this(context, null, 0);
  }

  public ThemedImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ThemedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    BasicActivity a = (BasicActivity) getActivity();

    // dark preference found
    if (a != null
        && (a.getAppTheme().equals(AppTheme.DARK) || a.getAppTheme().equals(AppTheme.BLACK))) {
      setColorFilter(Color.argb(255, 255, 255, 255)); // White Tint
    } else if (a == null) {
      throw new IllegalStateException("Could not get activity! Can't show correct icon color!");
    }
  }

  private Activity getActivity() {
    Context context = getContext();
    while (context instanceof ContextWrapper) {
      if (context instanceof Activity) {
        return (Activity) context;
      }
      context = ((ContextWrapper) context).getBaseContext();
    }
    return null;
  }
}
