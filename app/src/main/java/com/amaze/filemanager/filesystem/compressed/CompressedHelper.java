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

package com.amaze.filemanager.filesystem.compressed;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.fileoperations.utils.UpdatePosition;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.Bzip2Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.GzipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.Iso9660Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.LzmaExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.SevenZipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarBzip2Extractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarGzExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarLzmaExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarXzExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarZstExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.XzExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZstdExtractor;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.Iso9660Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.SevenZipDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarBzip2Decompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarGzDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarLzmaDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarXzDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.TarZstDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.UnknownCompressedFileDecompressor;
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.ZipDecompressor;
import com.amaze.filemanager.utils.Utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class CompressedHelper {
  private static final Logger LOG = LoggerFactory.getLogger(CompressedHelper.class);

  /**
   * Path separator used by all Decompressors and Extractors. e.g. rar internally uses '\' but is
   * converted to "/" for the app.
   */
  public static final char SEPARATOR_CHAR = '/';

  public static final String SEPARATOR = String.valueOf(SEPARATOR_CHAR).intern();

  public static final String fileExtensionZip = "zip",
      fileExtensionJar = "jar",
      fileExtensionApk = "apk",
      fileExtensionApks = "apks";
  public static final String fileExtensionTar = "tar";
  public static final String fileExtensionGzipTarLong = "tar.gz", fileExtensionGzipTarShort = "tgz";
  public static final String fileExtensionBzip2TarLong = "tar.bz2",
      fileExtensionBzip2TarShort = "tbz";
  public static final String fileExtensionRar = "rar";
  public static final String fileExtension7zip = "7z";
  public static final String fileExtensionTarLzma = "tar.lzma";
  public static final String fileExtensionTarXz = "tar.xz";
  public static final String fileExtensionTarZst = "tar.zst";
  public static final String fileExtensionXz = "xz";
  public static final String fileExtensionLzma = "lzma";
  public static final String fileExtensionGz = "gz";
  public static final String fileExtensionBzip2 = "bz2";
  public static final String fileExtensionZst = "zst";

  public static final String fileExtensionIso = "iso";

  private static final String TAG = CompressedHelper.class.getSimpleName();

  /** To add compatibility with other compressed file types edit this method */
  @Nullable
  public static Extractor getExtractorInstance(
      @NonNull Context context,
      @NonNull File file,
      @NonNull String outputPath,
      @NonNull Extractor.OnUpdate listener,
      @NonNull UpdatePosition updatePosition) {
    Extractor extractor;
    String type = getExtension(file.getPath());

    if (isZip(type)) {
      extractor = new ZipExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (BuildConfig.FLAVOR.equals("play") && isRar(type)) {
      extractor = new RarExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isTar(type)) {
      extractor = new TarExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isGzippedTar(type)) {
      extractor = new TarGzExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isBzippedTar(type)) {
      extractor =
          new TarBzip2Extractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isXzippedTar(type)) {
      extractor = new TarXzExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isLzippedTar(type)) {
      extractor =
          new TarLzmaExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isZstdTar(type)) {
      extractor =
          new TarZstExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (is7zip(type)) {
      extractor =
          new SevenZipExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isLzma(type)) {
      extractor = new LzmaExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isXz(type)) {
      extractor = new XzExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isGzip(type)) {
      extractor = new GzipExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isBzip2(type)) {
      extractor = new Bzip2Extractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isZst(type)) {
      extractor = new ZstdExtractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else if (isIso(type)) {
      extractor =
          new Iso9660Extractor(context, file.getPath(), outputPath, listener, updatePosition);
    } else {
      if (BuildConfig.DEBUG) {
        throw new IllegalArgumentException("The compressed file has no way of opening it: " + file);
      }
      LOG.error("The compressed file has no way of opening it: " + file);
      extractor = null;
    }

    return extractor;
  }

  /** To add compatibility with other compressed file types edit this method */
  @Nullable
  public static Decompressor getCompressorInstance(@NonNull Context context, @NonNull File file) {
    Decompressor decompressor;
    String type = getExtension(file.getPath());

    if (isZip(type)) {
      decompressor = new ZipDecompressor(context);
    } else if (BuildConfig.FLAVOR.equals("play") && isRar(type)) {
      decompressor = new RarDecompressor(context);
    } else if (isTar(type)) {
      decompressor = new TarDecompressor(context);
    } else if (isGzippedTar(type)) {
      decompressor = new TarGzDecompressor(context);
    } else if (isBzippedTar(type)) {
      decompressor = new TarBzip2Decompressor(context);
    } else if (isXzippedTar(type)) {
      decompressor = new TarXzDecompressor(context);
    } else if (isLzippedTar(type)) {
      decompressor = new TarLzmaDecompressor(context);
    } else if (isZstdTar(type)) {
      decompressor = new TarZstDecompressor(context);
    } else if (is7zip(type)) {
      decompressor = new SevenZipDecompressor(context);
    } else if (isIso(type)) {
      decompressor = new Iso9660Decompressor(context);
    } else if (isXz(type) || isLzma(type) || isGzip(type) || isBzip2(type) || isZst(type)) {
      // These 4 types are only compressing one single file.
      // Hence invoking this UnknownCompressedFileDecompressor which only returns the filename
      // without the compression extension
      decompressor = new UnknownCompressedFileDecompressor(context);
    } else {
      if (BuildConfig.DEBUG) {
        throw new IllegalArgumentException("The compressed file has no way of opening it: " + file);
      }

      LOG.error("The compressed file has no way of opening it: " + file);
      decompressor = null;
    }

    if (decompressor != null) {
      decompressor.setFilePath(file.getPath());
    }

    return decompressor;
  }

  public static boolean isFileExtractable(String path) {
    String type = getExtension(path);

    return isZip(type)
        || isTar(type)
        || isRar(type)
        || isGzippedTar(type)
        || is7zip(type)
        || isBzippedTar(type)
        || isXzippedTar(type)
        || isLzippedTar(type)
        || isZstdTar(type)
        || isBzip2(type)
        || isGzip(type)
        || isLzma(type)
        || isXz(type)
        || isZst(type)
        || isIso(type);
  }

  /**
   * Gets the name of the file without compression extention. For example: "s.tar.gz" to "s" "s.tar"
   * to "s"
   */
  public static String getFileName(String compressedName) {
    compressedName = compressedName.toLowerCase();
    boolean hasFileName = compressedName.contains(".");
    if (hasFileName
        && (isZip(compressedName)
            || isTar(compressedName)
            || isRar(compressedName)
            || is7zip(compressedName)
            || isXz(compressedName)
            || isLzma(compressedName)
            || isGzip(compressedName)
            || compressedName.endsWith(fileExtensionGzipTarShort)
            || compressedName.endsWith(fileExtensionBzip2TarShort)
            || isGzip(compressedName)
            || isBzip2(compressedName)
            || isLzma(compressedName)
            || isXz(compressedName)
            || isZst(compressedName)
            || isIso(compressedName))) {
      return compressedName.substring(0, compressedName.lastIndexOf("."));
    } else if (hasFileName && isGzippedTar(compressedName)
        || isXzippedTar(compressedName)
        || isLzippedTar(compressedName)
        || isBzippedTar(compressedName)
        || isZstdTar(compressedName)) {
      return compressedName.substring(0, Utils.nthToLastCharIndex(2, compressedName, '.'));
    } else {
      return compressedName;
    }
  }

  public static final boolean isEntryPathValid(String entryPath) {
    return !entryPath.startsWith("..\\") && !entryPath.startsWith("../") && !entryPath.equals("..");
  }

  private static boolean isZip(String type) {
    return type.endsWith(fileExtensionZip)
        || type.endsWith(fileExtensionJar)
        || type.endsWith(fileExtensionApk)
        || type.endsWith(fileExtensionApks);
  }

  private static boolean isTar(String type) {
    return type.endsWith(fileExtensionTar);
  }

  private static boolean isGzippedTar(String type) {
    return type.endsWith(fileExtensionGzipTarLong) || type.endsWith(fileExtensionGzipTarShort);
  }

  private static boolean isBzippedTar(String type) {
    return type.endsWith(fileExtensionBzip2TarLong) || type.endsWith(fileExtensionBzip2TarShort);
  }

  private static boolean isRar(String type) {
    return type.endsWith(fileExtensionRar);
  }

  private static boolean is7zip(String type) {
    return type.endsWith(fileExtension7zip);
  }

  private static boolean isXzippedTar(String type) {
    return type.endsWith(fileExtensionTarXz);
  }

  private static boolean isLzippedTar(String type) {
    return type.endsWith(fileExtensionTarLzma);
  }

  private static boolean isZstdTar(String type) {
    return type.endsWith(fileExtensionTarZst);
  }

  private static boolean isXz(String type) {
    return type.endsWith(fileExtensionXz) && !isXzippedTar(type);
  }

  private static boolean isLzma(String type) {
    return type.endsWith(fileExtensionLzma) && !isLzippedTar(type);
  }

  private static boolean isGzip(String type) {
    return type.endsWith(fileExtensionGz) && !isGzippedTar(type);
  }

  private static boolean isBzip2(String type) {
    return type.endsWith(fileExtensionBzip2) && !isBzippedTar(type);
  }

  private static boolean isZst(String type) {
    return type.endsWith(fileExtensionZst) && !isZstdTar(type);
  }

  private static boolean isIso(String type) {
    return type.endsWith(fileExtensionIso);
  }

  private static String getExtension(String path) {
    return path.substring(path.indexOf('.') + 1).toLowerCase();
  }
}
