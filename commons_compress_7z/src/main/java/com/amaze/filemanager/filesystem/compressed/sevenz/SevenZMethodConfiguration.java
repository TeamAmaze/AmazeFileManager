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

import java.util.Objects;

/**
 * Combines a SevenZMethod with configuration options for the method.
 *
 * <p>The exact type and interpretation of options depends on the method being configured. Currently
 * supported are:
 *
 * <table>
 * <caption>Options</caption>
 * <tr><th>Method</th><th>Option Type</th><th>Description</th></tr>
 * <tr><td>BZIP2</td><td>Number</td><td>Block Size - an number between 1 and 9</td></tr>
 * <tr><td>DEFLATE</td><td>Number</td><td>Compression Level - an number between 1 and 9</td></tr>
 * <tr><td>LZMA2</td><td>Number</td><td>Dictionary Size - a number between 4096 and 768 MiB (768 &lt;&lt; 20)</td></tr>
 * <tr><td>LZMA2</td><td>org.tukaani.xz.LZMA2Options</td><td>Whole set of LZMA2 options.</td></tr>
 * <tr><td>DELTA_FILTER</td><td>Number</td><td>Delta Distance - a number between 1 and 256</td></tr>
 * </table>
 *
 * @Immutable
 *
 * @since 1.8
 */
public class SevenZMethodConfiguration {
  private final SevenZMethod method;
  private final Object options;

  /**
   * Doesn't configure any additional options.
   *
   * @param method the method to use
   */
  public SevenZMethodConfiguration(final SevenZMethod method) {
    this(method, null);
  }

  /**
   * Specifies and method plus configuration options.
   *
   * @param method the method to use
   * @param options the options to use
   * @throws IllegalArgumentException if the method doesn't understand the options specified.
   */
  public SevenZMethodConfiguration(final SevenZMethod method, final Object options) {
    this.method = method;
    this.options = options;
    if (options != null && !Coders.findByMethod(method).canAcceptOptions(options)) {
      throw new IllegalArgumentException(
          "The " + method + " method doesn't support options of type " + options.getClass());
    }
  }

  /**
   * The specified method.
   *
   * @return the method
   */
  public SevenZMethod getMethod() {
    return method;
  }

  /**
   * The specified options.
   *
   * @return the options
   */
  public Object getOptions() {
    return options;
  }

  @Override
  public int hashCode() {
    return method == null ? 0 : method.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final SevenZMethodConfiguration other = (SevenZMethodConfiguration) obj;
    return Objects.equals(method, other.method) && Objects.equals(options, other.options);
  }
}
