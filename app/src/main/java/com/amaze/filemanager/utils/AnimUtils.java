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

import com.amaze.filemanager.ui.views.ThemedTextView;

import android.content.Context;
import android.os.Handler;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

/** Utility methods for working with animations. */
public class AnimUtils {

  private static Interpolator fastOutSlowIn;

  public static Interpolator getFastOutSlowInInterpolator(Context context) {
    if (fastOutSlowIn == null) {
      fastOutSlowIn =
          AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in);
    }
    return fastOutSlowIn;
  }

  /**
   * Animates filenames textview to marquee after a delay. Make sure to set {@link
   * TextView#setSelected(boolean)} to false in order to stop the marquee later
   */
  public static void marqueeAfterDelay(int delayInMillis, ThemedTextView marqueeView) {
    new Handler()
        .postDelayed(
            () -> {
              // marquee works only when text view has focus
              marqueeView.setSelected(true);
            },
            delayInMillis);
  }
}
