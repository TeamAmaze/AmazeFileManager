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

package com.amaze.filemanager.activities.superclasses;

import android.os.AsyncTask;

public interface BaseAsyncTask {

  /**
   * Safely fetch an object for null safety, otherwise interrupt the task. Task should implement
   * {@link AsyncTask#onCancelled()}
   *
   * @param t given object to return
   * @param asyncTask task implementing this interface
   * @param <T> data type, preferably activities / fragments / contexts which can be null at any
   *     given point of time while task is running
   * @return T t
   */
  default <T> T nullCheckOrInterrupt(T t, AsyncTask asyncTask) {
    if (t != null) {
      return t;
    } else {
      asyncTask.cancel(true);
      return null;
    }
  }
}
