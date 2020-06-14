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

package com.amaze.filemanager.asynchronous.asynctasks;

/**
 * Container for AsyncTask results. Allow either result object or exception to be contained.
 *
 * @param <T> Result type
 */
public class AsyncTaskResult<T> {
  public final T result;
  public final Throwable exception;

  public AsyncTaskResult(T result) {
    this.result = result;
    this.exception = null;
  }

  public AsyncTaskResult(Throwable exception) {
    this.result = null;
    this.exception = exception;
  }

  /** Callback interface for use in {@link android.os.AsyncTask}. Think Promise callback in JS. */
  public interface Callback<T> {

    /** Implement logic on what to do with the result here. */
    void onResult(T result);
  }
}
