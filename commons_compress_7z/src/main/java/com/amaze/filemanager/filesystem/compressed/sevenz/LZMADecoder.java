/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.compress.utils.FlushShieldFilterOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.LZMAOutputStream;

class LZMADecoder extends CoderBase {
  LZMADecoder() {
    super(LZMA2Options.class, Number.class);
  }

  @Override
  InputStream decode(
      final String archiveName,
      final InputStream in,
      final long uncompressedLength,
      final Coder coder,
      final byte[] password)
      throws IOException {
    final byte propsByte = coder.properties[0];
    final int dictSize = getDictionarySize(coder);
    if (dictSize > LZMAInputStream.DICT_SIZE_MAX) {
      throw new IOException("Dictionary larger than 4GiB maximum size used in " + archiveName);
    }
    return new LZMAInputStream(in, uncompressedLength, propsByte, dictSize);
  }

  @SuppressWarnings("resource")
  @Override
  OutputStream encode(final OutputStream out, final Object opts) throws IOException {
    // NOOP as LZMAOutputStream throws an exception in flush
    return new FlushShieldFilterOutputStream(new LZMAOutputStream(out, getOptions(opts), false));
  }

  @Override
  byte[] getOptionsAsProperties(final Object opts) throws IOException {
    final LZMA2Options options = getOptions(opts);
    final byte props = (byte) ((options.getPb() * 5 + options.getLp()) * 9 + options.getLc());
    int dictSize = options.getDictSize();
    byte[] o = new byte[5];
    o[0] = props;
    ByteUtils.toLittleEndian(o, dictSize, 1, 4);
    return o;
  }

  @Override
  Object getOptionsFromCoder(final Coder coder, final InputStream in) throws IOException {
    final byte propsByte = coder.properties[0];
    int props = propsByte & 0xFF;
    int pb = props / (9 * 5);
    props -= pb * 9 * 5;
    int lp = props / 9;
    int lc = props - lp * 9;
    LZMA2Options opts = new LZMA2Options();
    opts.setPb(pb);
    opts.setLcLp(lc, lp);
    opts.setDictSize(getDictionarySize(coder));
    return opts;
  }

  private int getDictionarySize(final Coder coder) throws IllegalArgumentException {
    return (int) ByteUtils.fromLittleEndian(coder.properties, 1, 4);
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
