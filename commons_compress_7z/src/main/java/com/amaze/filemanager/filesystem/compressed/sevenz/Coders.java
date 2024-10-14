/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;
import org.apache.commons.compress.utils.FlushShieldFilterOutputStream;
import org.tukaani.xz.ARMOptions;
import org.tukaani.xz.ARMThumbOptions;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.IA64Options;
import org.tukaani.xz.PowerPCOptions;
import org.tukaani.xz.SPARCOptions;
import org.tukaani.xz.X86Options;

class Coders {
  private static final Map<SevenZMethod, CoderBase> CODER_MAP =
      new HashMap<SevenZMethod, CoderBase>() {

        private static final long serialVersionUID = 1664829131806520867L;

        {
          put(SevenZMethod.COPY, new CopyDecoder());
          put(SevenZMethod.LZMA, new LZMADecoder());
          put(SevenZMethod.LZMA2, new LZMA2Decoder());
          put(SevenZMethod.DEFLATE, new DeflateDecoder());
          put(SevenZMethod.DEFLATE64, new Deflate64Decoder());
          put(SevenZMethod.BZIP2, new BZIP2Decoder());
          put(SevenZMethod.AES256SHA256, new AES256SHA256Decoder());
          put(SevenZMethod.BCJ_X86_FILTER, new BCJDecoder(new X86Options()));
          put(SevenZMethod.BCJ_PPC_FILTER, new BCJDecoder(new PowerPCOptions()));
          put(SevenZMethod.BCJ_IA64_FILTER, new BCJDecoder(new IA64Options()));
          put(SevenZMethod.BCJ_ARM_FILTER, new BCJDecoder(new ARMOptions()));
          put(SevenZMethod.BCJ_ARM_THUMB_FILTER, new BCJDecoder(new ARMThumbOptions()));
          put(SevenZMethod.BCJ_SPARC_FILTER, new BCJDecoder(new SPARCOptions()));
          put(SevenZMethod.DELTA_FILTER, new DeltaDecoder());
        }
      };

  static CoderBase findByMethod(final SevenZMethod method) {
    return CODER_MAP.get(method);
  }

  static InputStream addDecoder(
      final String archiveName,
      final InputStream is,
      final long uncompressedLength,
      final Coder coder,
      final byte[] password,
      final int maxMemoryLimitInKb)
      throws IOException {
    final CoderBase cb = findByMethod(SevenZMethod.byId(coder.decompressionMethodId));
    if (cb == null) {
      throw new IOException(
          "Unsupported compression method "
              + Arrays.toString(coder.decompressionMethodId)
              + " used in "
              + archiveName);
    }
    return cb.decode(archiveName, is, uncompressedLength, coder, password, maxMemoryLimitInKb);
  }

  static OutputStream addEncoder(
      final OutputStream out, final SevenZMethod method, final Object options) throws IOException {
    final CoderBase cb = findByMethod(method);
    if (cb == null) {
      throw new IOException("Unsupported compression method " + method);
    }
    return cb.encode(out, options);
  }

  static class CopyDecoder extends CoderBase {
    @Override
    InputStream decode(
        final String archiveName,
        final InputStream in,
        final long uncompressedLength,
        final Coder coder,
        final byte[] password,
        final int maxMemoryLimitInKb)
        throws IOException {
      return in;
    }

    @Override
    OutputStream encode(final OutputStream out, final Object options) {
      return out;
    }
  }

  static class BCJDecoder extends CoderBase {
    private final FilterOptions opts;

    BCJDecoder(final FilterOptions opts) {
      this.opts = opts;
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
        return opts.getInputStream(in);
      } catch (final AssertionError e) {
        throw new IOException(
            "BCJ filter used in "
                + archiveName
                + " needs XZ for Java > 1.4 - see "
                + "https://commons.apache.org/proper/commons-compress/limitations.html#7Z",
            e);
      }
    }

    @SuppressWarnings("resource")
    @Override
    OutputStream encode(final OutputStream out, final Object options) {
      return new FlushShieldFilterOutputStream(
          opts.getOutputStream(new FinishableWrapperOutputStream(out)));
    }
  }

  static class DeflateDecoder extends CoderBase {
    private static final byte[] ONE_ZERO_BYTE = new byte[1];

    DeflateDecoder() {
      super(Number.class);
    }

    @SuppressWarnings("resource") // caller must close the InputStream
    @Override
    InputStream decode(
        final String archiveName,
        final InputStream in,
        final long uncompressedLength,
        final Coder coder,
        final byte[] password,
        final int maxMemoryLimitInKb)
        throws IOException {
      final Inflater inflater = new Inflater(true);
      // Inflater with nowrap=true has this odd contract for a zero padding
      // byte following the data stream; this used to be zlib's requirement
      // and has been fixed a long time ago, but the contract persists so
      // we comply.
      // https://docs.oracle.com/javase/7/docs/api/java/util/zip/Inflater.html#Inflater(boolean)
      final InflaterInputStream inflaterInputStream =
          new InflaterInputStream(
              new SequenceInputStream(in, new ByteArrayInputStream(ONE_ZERO_BYTE)), inflater);
      return new DeflateDecoderInputStream(inflaterInputStream, inflater);
    }

    @Override
    OutputStream encode(final OutputStream out, final Object options) {
      final int level = numberOptionOrDefault(options, 9);
      final Deflater deflater = new Deflater(level, true);
      final DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(out, deflater);
      return new DeflateDecoderOutputStream(deflaterOutputStream, deflater);
    }

    static class DeflateDecoderInputStream extends InputStream {

      final InflaterInputStream inflaterInputStream;
      Inflater inflater;

      public DeflateDecoderInputStream(
          final InflaterInputStream inflaterInputStream, final Inflater inflater) {
        this.inflaterInputStream = inflaterInputStream;
        this.inflater = inflater;
      }

      @Override
      public int read() throws IOException {
        return inflaterInputStream.read();
      }

      @Override
      public int read(final byte[] b, final int off, final int len) throws IOException {
        return inflaterInputStream.read(b, off, len);
      }

      @Override
      public int read(final byte[] b) throws IOException {
        return inflaterInputStream.read(b);
      }

      @Override
      public void close() throws IOException {
        try {
          inflaterInputStream.close();
        } finally {
          inflater.end();
        }
      }
    }

    static class DeflateDecoderOutputStream extends OutputStream {

      final DeflaterOutputStream deflaterOutputStream;
      Deflater deflater;

      public DeflateDecoderOutputStream(
          final DeflaterOutputStream deflaterOutputStream, final Deflater deflater) {
        this.deflaterOutputStream = deflaterOutputStream;
        this.deflater = deflater;
      }

      @Override
      public void write(final int b) throws IOException {
        deflaterOutputStream.write(b);
      }

      @Override
      public void write(final byte[] b) throws IOException {
        deflaterOutputStream.write(b);
      }

      @Override
      public void write(final byte[] b, final int off, final int len) throws IOException {
        deflaterOutputStream.write(b, off, len);
      }

      @Override
      public void close() throws IOException {
        try {
          deflaterOutputStream.close();
        } finally {
          deflater.end();
        }
      }
    }
  }

  static class Deflate64Decoder extends CoderBase {
    Deflate64Decoder() {
      super(Number.class);
    }

    @SuppressWarnings("resource") // caller must close the InputStream
    @Override
    InputStream decode(
        final String archiveName,
        final InputStream in,
        final long uncompressedLength,
        final Coder coder,
        final byte[] password,
        final int maxMemoryLimitInKb)
        throws IOException {
      return new Deflate64CompressorInputStream(in);
    }
  }

  static class BZIP2Decoder extends CoderBase {
    BZIP2Decoder() {
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
      return new BZip2CompressorInputStream(in);
    }

    @Override
    OutputStream encode(final OutputStream out, final Object options) throws IOException {
      final int blockSize =
          numberOptionOrDefault(options, BZip2CompressorOutputStream.MAX_BLOCKSIZE);
      return new BZip2CompressorOutputStream(out, blockSize);
    }
  }
}
