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

package com.amaze.filemanager.file_operations.filesystem.cloud;

import com.amaze.filemanager.file_operations.filesystem.streams.RandomAccessStream;

import java.io.IOException;
import java.io.InputStream;

public class CloudStreamSource extends RandomAccessStream {
  protected long fp;
  protected String name;
  private InputStream inputStream;

  public CloudStreamSource(String fileName, long length, InputStream inputStream) {
    super(length);

    fp = 0;
    this.name = fileName;
    this.inputStream = inputStream;
  }

  /**
   * You may notice a strange name for the smb input stream. I made some modifications to the
   * original one in the jcifs library for my needs, but streaming required returning to the
   * original one so I renamed it to "old". However, I needed to specify a buffer size in the
   * constructor. It looks now like this:
   *
   * <p>public SmbFileInputStreamOld( SmbFile file, int readBuffer, int openFlags) throws
   * SmbException, MalformedURLException, UnknownHostException { this.file = file; this.openFlags =
   * SmbFile.O_RDONLY & 0xFFFF; this.access = (openFlags >>> 16) & 0xFFFF; if (file.type !=
   * SmbFile.TYPE_NAMED_PIPE) { file.open( openFlags, access, SmbFile.ATTR_NORMAL, 0 );
   * this.openFlags &= ~(SmbFile.O_CREAT | SmbFile.O_TRUNC); } else { file.connect0(); } readSize =
   * readBuffer; fs = file.length(); }
   *
   * <p>Setting buffer size by properties didn't work for me so I created this constructor. In the
   * libs folder there is a library modified by me. If you want to use a stock one, you have to set
   * somehow the buffer size to be equal with http server's buffer size which is 8192.
   */
  public void open() throws IOException {
    if (fp > 0) {
      fp = inputStream.skip(fp);
    }
  }

  @Override
  public int read() throws IOException {
    int read = inputStream.read();
    if (read != -1) fp++;
    return read;
  }

  @Override
  public int read(byte[] bytes, int start, int offs) throws IOException {
    int read = inputStream.read(bytes, start, offs);
    fp += read;
    return read;
  }

  @Override
  public void close() throws IOException {
    if (inputStream != null) {
      inputStream.close();
    }
  }

  public String getName() {
    return name;
  }

  @Override
  public void moveTo(long position) {
    if (position < 0 || length() < position) {
      throw new IllegalArgumentException("Position out of the bounds of the file!");
    }

    fp = position;
  }

  @Override
  protected long getCurrentPosition() {
    return fp;
  }
}
