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

/**
 * Collects options for reading 7z archives.
 *
 * @since 1.19 @Immutable
 */
public class SevenZFileOptions {
  private static final int DEFAUL_MEMORY_LIMIT_IN_KB = Integer.MAX_VALUE;
  private static final boolean DEFAULT_USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES = false;
  private static final boolean DEFAULT_TRY_TO_RECOVER_BROKEN_ARCHIVES = false;

  private final int maxMemoryLimitInKb;
  private final boolean useDefaultNameForUnnamedEntries;
  private final boolean tryToRecoverBrokenArchives;

  private SevenZFileOptions(
      final int maxMemoryLimitInKb,
      final boolean useDefaultNameForUnnamedEntries,
      final boolean tryToRecoverBrokenArchives) {
    this.maxMemoryLimitInKb = maxMemoryLimitInKb;
    this.useDefaultNameForUnnamedEntries = useDefaultNameForUnnamedEntries;
    this.tryToRecoverBrokenArchives = tryToRecoverBrokenArchives;
  }

  /**
   * The default options.
   *
   * <ul>
   *   <li>no memory limit
   *   <li>don't modify the name of unnamed entries
   * </ul>
   */
  public static final SevenZFileOptions DEFAULT =
      new SevenZFileOptions(
          DEFAUL_MEMORY_LIMIT_IN_KB,
          DEFAULT_USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES,
          DEFAULT_TRY_TO_RECOVER_BROKEN_ARCHIVES);

  /**
   * Obtains a builder for SevenZFileOptions.
   *
   * @return a builder for SevenZFileOptions.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the maximum amount of memory to use for parsing the archive and during extraction.
   *
   * <p>Not all codecs will honor this setting. Currently only lzma and lzma2 are supported.
   *
   * @return the maximum amount of memory to use for extraction
   */
  public int getMaxMemoryLimitInKb() {
    return maxMemoryLimitInKb;
  }

  /**
   * Gets whether entries without a name should get their names set to the archive's default file
   * name.
   *
   * @return whether entries without a name should get their names set to the archive's default file
   *     name
   */
  public boolean getUseDefaultNameForUnnamedEntries() {
    return useDefaultNameForUnnamedEntries;
  }

  /**
   * Whether {@link SevenZFile} shall try to recover from a certain type of broken archive.
   *
   * @return whether SevenZFile shall try to recover from a certain type of broken archive.
   * @since 1.21
   */
  public boolean getTryToRecoverBrokenArchives() {
    return tryToRecoverBrokenArchives;
  }

  /**
   * Mutable builder for the immutable {@link SevenZFileOptions}.
   *
   * @since 1.19
   */
  public static class Builder {
    private int maxMemoryLimitInKb = DEFAUL_MEMORY_LIMIT_IN_KB;
    private boolean useDefaultNameForUnnamedEntries = DEFAULT_USE_DEFAULTNAME_FOR_UNNAMED_ENTRIES;
    private boolean tryToRecoverBrokenArchives = DEFAULT_TRY_TO_RECOVER_BROKEN_ARCHIVES;

    /**
     * Sets the maximum amount of memory to use for parsing the archive and during extraction.
     *
     * <p>Not all codecs will honor this setting. Currently only lzma and lzma2 are supported.
     *
     * @param maxMemoryLimitInKb limit of the maximum amount of memory to use
     * @return the reconfigured builder
     */
    public Builder withMaxMemoryLimitInKb(final int maxMemoryLimitInKb) {
      this.maxMemoryLimitInKb = maxMemoryLimitInKb;
      return this;
    }

    /**
     * Sets whether entries without a name should get their names set to the archive's default file
     * name.
     *
     * @param useDefaultNameForUnnamedEntries if true the name of unnamed entries will be set to the
     *     archive's default name
     * @return the reconfigured builder
     */
    public Builder withUseDefaultNameForUnnamedEntries(
        final boolean useDefaultNameForUnnamedEntries) {
      this.useDefaultNameForUnnamedEntries = useDefaultNameForUnnamedEntries;
      return this;
    }

    /**
     * Sets whether {@link SevenZFile} will try to revover broken archives where the CRC of the
     * file's metadata is 0.
     *
     * <p>This special kind of broken archive is encountered when mutli volume archives are closed
     * prematurely. If you enable this option SevenZFile will trust data that looks as if it could
     * contain metadata of an archive and allocate big amounts of memory. It is strongly recommended
     * to not enable this option without setting {@link #withMaxMemoryLimitInKb} at the same time.
     *
     * @param tryToRecoverBrokenArchives if true SevenZFile will try to recover archives that are
     *     broken in the specific way
     * @return the reconfigured builder
     * @since 1.21
     */
    public Builder withTryToRecoverBrokenArchives(final boolean tryToRecoverBrokenArchives) {
      this.tryToRecoverBrokenArchives = tryToRecoverBrokenArchives;
      return this;
    }

    /**
     * Create the {@link SevenZFileOptions}.
     *
     * @return configured {@link SevenZFileOptions}.
     */
    public SevenZFileOptions build() {
      return new SevenZFileOptions(
          maxMemoryLimitInKb, useDefaultNameForUnnamedEntries, tryToRecoverBrokenArchives);
    }
  }
}
