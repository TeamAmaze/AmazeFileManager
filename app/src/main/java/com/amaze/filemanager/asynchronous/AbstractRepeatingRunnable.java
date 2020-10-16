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

package com.amaze.filemanager.asynchronous;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public abstract class AbstractRepeatingRunnable implements Runnable {

  protected final ScheduledFuture handle;

  public AbstractRepeatingRunnable(
      long initialDelay, long period, @NonNull TimeUnit unit, boolean startImmediately) {
    if (!startImmediately) {
      throw new UnsupportedOperationException("RepeatingRunnables are immediately executed!");
    }

    ScheduledExecutorService threadExcecutor = Executors.newScheduledThreadPool(0);
    handle = threadExcecutor.scheduleAtFixedRate(this, initialDelay, period, unit);
  }

  public boolean isAlive() {
    return !handle.isDone();
  }

  /**
   * @param immediately sets if the cancellation occurt right now, or after the run() function
   *     returns
   */
  public void cancel(boolean immediately) {
    handle.cancel(immediately);
  }
}
