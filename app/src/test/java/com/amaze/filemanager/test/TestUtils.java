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

package com.amaze.filemanager.test;

import static org.robolectric.Shadows.shadowOf;

import java.lang.reflect.Field;

import org.robolectric.util.Scheduler;

import com.amaze.filemanager.utils.application.AppConfig;

import android.os.Handler;

public class TestUtils {

  /**
   * Flush the background handler thread.
   *
   * <p>Due to the way Robolectric implement threads, Runnables posted to the handler thread via
   * {@link AppConfig#runInBackground(Runnable)} may not be executed, making tests involving it will
   * fail. This method accesses the {@link Scheduler} behind the {@link
   * org.robolectric.shadows.ShadowLooper} of the background {@link Handler} and execute any {@link
   * Runnable}s posted to it.
   *
   * <p>This method throws {@link AssertionError} directly so test code don't need to handle
   * possible exceptions occur when accessing
   *
   * <pre>AppConfig.backgroundHandler</pre>
   *
   * via reflection.
   *
   * @see Scheduler#advanceToNextPostedRunnable()
   * @see AppConfig#backgroundHandler
   * @see AppConfig#runInBackground(Runnable)
   */
  public static void flushAppConfigHandlerThread() {
    try {
      Field f = AppConfig.class.getDeclaredField("backgroundHandler");
      f.setAccessible(true);
      Handler h = (Handler) f.get(null);
      Scheduler scheduler = shadowOf(h.getLooper()).getScheduler();
      scheduler.advanceToNextPostedRunnable();
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError("Unable to access backgroundHandler within AppConfig");
    }
  }
}
