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

import org.apache.commons.compress.MemoryLimitException;
import org.tukaani.xz.FinishableOutputStream;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.LZMA2InputStream;
import org.tukaani.xz.LZMA2Options;

class LZMA2Decoder extends CoderBase {
  LZMA2Decoder() {
    super(LZMA2Options.class, Number.class);
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
    try {
      final int dictionarySize = getDictionarySize(coder);
      final int memoryUsageInKb = LZMA2InputStream.getMemoryUsage(dictionarySize);
      if (memoryUsageInKb > maxMemoryLimitInKb) {
        throw new MemoryLimitException(memoryUsageInKb, maxMemoryLimitInKb);
      }
      return new LZMA2InputStream(in, dictionarySize);
    } catch (final IllegalArgumentException ex) { // NOSONAR
      throw new IOException(ex.getMessage());
    }
  }

  @Override
  OutputStream encode(final OutputStream out, final Object opts) throws IOException {
    final LZMA2Options options = getOptions(opts);
    final FinishableOutputStream wrapped = new FinishableWrapperOutputStream(out);
    return options.getOutputStream(wrapped);
  }

  @Override
  byte[] getOptionsAsProperties(final Object opts) {
    final int dictSize = getDictSize(opts);
    final int lead = Integer.numberOfLeadingZeros(dictSize);
    final int secondBit = (dictSize >>> (30 - lead)) - 2;
    return new byte[] {(byte) ((19 - lead) * 2 + secondBit)};
  }

  @Override
  Object getOptionsFromCoder(final Coder coder, final InputStream in) throws IOException {
    return getDictionarySize(coder);
  }

  private int getDictSize(final Object opts) {
    if (opts instanceof LZMA2Options) {
      return ((LZMA2Options) opts).getDictSize();
    }
    return numberOptionOrDefault(opts);
  }

  private int getDictionarySize(final Coder coder) throws IOException {
    if (coder.properties == null) {
      throw new IOException("Missing LZMA2 properties");
    }
    if (coder.properties.length < 1) {
      throw new IOException("LZMA2 properties too short");
    }
    final int dictionarySizeBits = 0xff & coder.properties[0];
    if ((dictionarySizeBits & (~0x3f)) != 0) {
      throw new IOException("Unsupported LZMA2 property bits");
    }
    if (dictionarySizeBits > 40) {
      throw new IOException("Dictionary larger than 4GiB maximum size");
    }
    if (dictionarySizeBits == 40) {
      return 0xFFFFffff;
    }
    return (2 | (dictionarySizeBits & 0x1)) << (dictionarySizeBits / 2 + 11);
  }

  private LZMA2Options getOptions(final Object opts) throws IOException {
    if (opts instanceof LZMA2Options) {
      return (LZMA2Options) opts;
    }
    final LZMA2Options options = new LZMA2Options();
    options.setDictSize(numberOptionOrDefault(opts));
    return options;
  }

  private int numberOptionOrDefault(final Object opts) {
    return numberOptionOrDefault(opts, LZMA2Options.DICT_SIZE_DEFAULT);
  }
}
