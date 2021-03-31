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

package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache;
import com.amaze.filemanager.file_operations.utils.UpdatePosition;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;

import android.content.Context;

import androidx.annotation.NonNull;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

public class ZipExtractor extends Extractor {

  public ZipExtractor(
      @NonNull Context context,
      @NonNull String filePath,
      @NonNull String outputPath,
      @NonNull OnUpdate listener,
      @NonNull UpdatePosition updatePosition) {
    super(context, filePath, outputPath, listener, updatePosition);
  }

  @Override
  protected void extractWithFilter(@NonNull Filter filter) throws IOException {
    long totalBytes = 0;
    List<FileHeader> entriesToExtract = new ArrayList<>();
    try {
      ZipFile zipfile = new ZipFile(filePath);
      if (ArchivePasswordCache.getInstance().containsKey(filePath)) {
        zipfile.setPassword(ArchivePasswordCache.getInstance().get(filePath).toCharArray());
      }

      // iterating archive elements to find file names that are to be extracted
      for (Object obj : zipfile.getFileHeaders()) {
        FileHeader fileHeader = (FileHeader) obj;

        if (CompressedHelper.isEntryPathValid(fileHeader.getFileName())) {
          if (filter.shouldExtract(fileHeader.getFileName(), fileHeader.isDirectory())) {
            entriesToExtract.add(fileHeader);
            totalBytes += fileHeader.getUncompressedSize();
          }
        } else {
          invalidArchiveEntries.add(fileHeader.getFileName());
        }
      }

      listener.onStart(totalBytes, entriesToExtract.get(0).getFileName());

      for (FileHeader entry : entriesToExtract) {
        if (!listener.isCancelled()) {
          listener.onUpdate(entry.getFileName());
          extractEntry(context, zipfile, entry, outputPath);
        }
      }
      listener.onFinish();
    } catch (ZipException e) {
      throw new IOException(e);
    }
  }

  /**
   * Method extracts {@link FileHeader} from {@link ZipFile}
   *
   * @param zipFile zip file from which entriesToExtract are to be extracted
   * @param entry zip entry that is to be extracted
   * @param outputDir output directory
   */
  private void extractEntry(
      @NonNull final Context context, ZipFile zipFile, FileHeader entry, String outputDir)
      throws IOException {
    final File outputFile = new File(outputDir, fixEntryName(entry.getFileName()));

    if (!outputFile.getCanonicalPath().startsWith(outputDir)) {
      throw new IOException("Incorrect ZipEntry path!");
    }

    if (entry.isDirectory()) {
      // zip entry is a directory, return after creating new directory
      FileUtil.mkdir(outputFile, context);
      return;
    }

    if (!outputFile.getParentFile().exists()) {
      // creating directory if not already exists
      FileUtil.mkdir(outputFile.getParentFile(), context);
    }

    BufferedInputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
    BufferedOutputStream outputStream =
        new BufferedOutputStream(FileUtil.getOutputStream(outputFile, context));

    try {
      int len;
      byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
      while ((len = inputStream.read(buf)) != -1) {
        if (!listener.isCancelled()) {
          outputStream.write(buf, 0, len);
          updatePosition.updatePosition(len);
        } else break;
      }
    } finally {
      outputStream.close();
      inputStream.close();
      outputFile.setLastModified(entry.getLastModifiedTimeEpoch());
    }
  }
}
