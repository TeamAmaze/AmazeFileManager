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

package com.amaze.filemanager.file_operations.filesystem.streams;

import java.io.IOException;
import java.io.InputStream;

public abstract class RandomAccessStream extends InputStream {

  private long markedPosition;
  private long length;

  public RandomAccessStream(long length) {
    this.length = length;

    mark(-1);
  }

  @Override
  public synchronized void reset() {
    moveTo(markedPosition);
  }

  @Override
  public synchronized void mark(int readLimit) {
    if (readLimit != -1) {
      throw new IllegalArgumentException(
          "readLimit argument of RandomAccessStream.mark() is not used, please set to -1!");
    }

    markedPosition = getCurrentPosition();
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  public long availableExact() {
    return length - getCurrentPosition();
  }

  public long length() {
    return length;
  }

  @Override
  public int available() throws IOException {
    throw new IOException("Use availableExact()!");
  }

  public abstract int read() throws IOException;

  public abstract void moveTo(long position);

  protected abstract long getCurrentPosition();
}
