/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.compressed.sevenz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

class BoundedFileChannelInputStream extends InputStream {
  private static final int MAX_BUF_LEN = 8192;
  private final ByteBuffer buffer;
  private final FileChannel channel;
  private long bytesRemaining;

  public BoundedFileChannelInputStream(final FileChannel channel, final long size) {
    this.channel = channel;
    this.bytesRemaining = size;
    if (size < MAX_BUF_LEN && size > 0) {
      buffer = ByteBuffer.allocate((int) size);
    } else {
      buffer = ByteBuffer.allocate(MAX_BUF_LEN);
    }
  }

  @Override
  public int read() throws IOException {
    if (bytesRemaining > 0) {
      --bytesRemaining;
      int read = read(1);
      if (read < 0) {
        return read;
      }
      return buffer.get() & 0xff;
    }
    return -1;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    if (bytesRemaining == 0) {
      return -1;
    }
    int bytesToRead = len;
    if (bytesToRead > bytesRemaining) {
      bytesToRead = (int) bytesRemaining;
    }
    int bytesRead;
    ByteBuffer buf;
    if (bytesToRead <= buffer.capacity()) {
      buf = buffer;
      bytesRead = read(bytesToRead);
    } else {
      buf = ByteBuffer.allocate(bytesToRead);
      bytesRead = channel.read(buf);
      buf.flip();
    }
    if (bytesRead >= 0) {
      buf.get(b, off, bytesRead);
      bytesRemaining -= bytesRead;
    }
    return bytesRead;
  }

  private int read(int len) throws IOException {
    buffer.rewind().limit(len);
    int read = channel.read(buffer);
    buffer.flip();
    return read;
  }

  @Override
  public void close() {
    // the nested channel is controlled externally
  }
}
