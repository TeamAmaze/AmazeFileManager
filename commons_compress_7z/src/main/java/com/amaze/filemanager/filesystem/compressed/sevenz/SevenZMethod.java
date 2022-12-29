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

import java.util.Arrays;

/**
 * The (partially) supported compression/encryption methods used in 7z archives.
 *
 * <p>All methods with a _FILTER suffix are used as preprocessors with the goal of creating a better
 * compression ratio with the compressor that comes next in the chain of methods. 7z will in general
 * only allow them to be used together with a "real" compression method but Commons Compress doesn't
 * enforce this.
 *
 * <p>The BCJ_ filters work on executable files for the given platform and convert relative
 * addresses to absolute addresses in CALL instructions. This means they are only useful when
 * applied to executables of the chosen platform.
 */
public enum SevenZMethod {
  /** no compression at all */
  COPY(new byte[] {(byte) 0x00}),
  /** LZMA - only supported when reading */
  LZMA(new byte[] {(byte) 0x03, (byte) 0x01, (byte) 0x01}),
  /** LZMA2 */
  LZMA2(new byte[] {(byte) 0x21}),
  /** Deflate */
  DEFLATE(new byte[] {(byte) 0x04, (byte) 0x01, (byte) 0x08}),
  /**
   * Deflate64
   *
   * @since 1.16
   */
  DEFLATE64(new byte[] {(byte) 0x04, (byte) 0x01, (byte) 0x09}),
  /** BZIP2 */
  BZIP2(new byte[] {(byte) 0x04, (byte) 0x02, (byte) 0x02}),
  /**
   * AES encryption with a key length of 256 bit using SHA256 for hashes - only supported when
   * reading
   */
  AES256SHA256(new byte[] {(byte) 0x06, (byte) 0xf1, (byte) 0x07, (byte) 0x01}),
  /**
   * BCJ x86 platform version 1.
   *
   * @since 1.8
   */
  BCJ_X86_FILTER(new byte[] {0x03, 0x03, 0x01, 0x03}),
  /**
   * BCJ PowerPC platform.
   *
   * @since 1.8
   */
  BCJ_PPC_FILTER(new byte[] {0x03, 0x03, 0x02, 0x05}),
  /**
   * BCJ I64 platform.
   *
   * @since 1.8
   */
  BCJ_IA64_FILTER(new byte[] {0x03, 0x03, 0x04, 0x01}),
  /**
   * BCJ ARM platform.
   *
   * @since 1.8
   */
  BCJ_ARM_FILTER(new byte[] {0x03, 0x03, 0x05, 0x01}),
  /**
   * BCJ ARM Thumb platform.
   *
   * @since 1.8
   */
  BCJ_ARM_THUMB_FILTER(new byte[] {0x03, 0x03, 0x07, 0x01}),
  /**
   * BCJ Sparc platform.
   *
   * @since 1.8
   */
  BCJ_SPARC_FILTER(new byte[] {0x03, 0x03, 0x08, 0x05}),
  /**
   * Delta filter.
   *
   * @since 1.8
   */
  DELTA_FILTER(new byte[] {0x03});

  private final byte[] id;

  SevenZMethod(final byte[] id) {
    this.id = id;
  }

  byte[] getId() {
    final int idLength = id.length;
    final byte[] copy = new byte[idLength];
    System.arraycopy(id, 0, copy, 0, idLength);
    return copy;
  }

  static SevenZMethod byId(final byte[] id) {
    for (final SevenZMethod m : SevenZMethod.class.getEnumConstants()) {
      if (Arrays.equals(m.id, id)) {
        return m;
      }
    }
    return null;
  }
}
