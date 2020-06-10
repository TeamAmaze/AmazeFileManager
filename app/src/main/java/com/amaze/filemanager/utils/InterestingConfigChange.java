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

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;

/**
 * Created by vishal on 23/2/17.
 *
 * <p>Class determines whether there was a config change
 *
 * <p>Supposed to be used to determine recursive callbacks to fragment/activity/loader Make sure to
 * recycle after you're done
 */
public class InterestingConfigChange {

  private static Configuration lastConfiguration = new Configuration();
  private static int lastDensity = -1;

  /**
   * Check for any config change between various callbacks to this method. Make sure to recycle
   * after done
   */
  public static boolean isConfigChanged(Resources resources) {
    int changedFieldsMask = lastConfiguration.updateFrom(resources.getConfiguration());
    boolean densityChanged = lastDensity != resources.getDisplayMetrics().densityDpi;
    int mode =
        ActivityInfo.CONFIG_SCREEN_LAYOUT
            | ActivityInfo.CONFIG_UI_MODE
            | ActivityInfo.CONFIG_LOCALE;
    return densityChanged || (changedFieldsMask & mode) != 0;
  }

  /** Recycle after usage, to avoid getting inconsistent result because of static modifiers */
  public static void recycle() {
    lastConfiguration = new Configuration();
    lastDensity = -1;
  }
}
