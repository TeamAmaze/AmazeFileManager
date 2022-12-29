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
import java.io.OutputStream;

import org.tukaani.xz.DeltaOptions;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.UnsupportedOptionsException;

class DeltaDecoder extends CoderBase {
  DeltaDecoder() {
    super(Number.class);
  }

  @Override
  InputStream decode(
      final String archiveName,
      final InputStream in,
      final long uncompressedLength,
      final Coder coder,
      final byte[] password,
      final int maxMemoryLimitInKb)
      throws IOException {
    return new DeltaOptions(getOptionsFromCoder(coder)).getInputStream(in);
  }

  @SuppressWarnings("resource")
  @Override
  OutputStream encode(final OutputStream out, final Object options) throws IOException {
    final int distance = numberOptionOrDefault(options, 1);
    try {
      return new DeltaOptions(distance).getOutputStream(new FinishableWrapperOutputStream(out));
    } catch (final UnsupportedOptionsException ex) { // NOSONAR
      throw new IOException(ex.getMessage());
    }
  }

  @Override
  byte[] getOptionsAsProperties(final Object options) {
    return new byte[] {(byte) (numberOptionOrDefault(options, 1) - 1)};
  }

  @Override
  Object getOptionsFromCoder(final Coder coder, final InputStream in) {
    return getOptionsFromCoder(coder);
  }

  private int getOptionsFromCoder(final Coder coder) {
    if (coder.properties == null || coder.properties.length == 0) {
      return 1;
    }
    return (0xff & coder.properties[0]) + 1;
  }
}
