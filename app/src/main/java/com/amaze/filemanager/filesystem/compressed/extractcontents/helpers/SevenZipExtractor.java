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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache;
import com.amaze.filemanager.file_operations.utils.UpdatePosition;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZArchiveEntry;
import com.amaze.filemanager.filesystem.compressed.sevenz.SevenZFile;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;

import android.content.Context;

import androidx.annotation.NonNull;

public class SevenZipExtractor extends Extractor {

  public SevenZipExtractor(
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
    SevenZFile sevenzFile =
        (ArchivePasswordCache.getInstance().containsKey(filePath))
            ? new SevenZFile(
                new File(filePath), ArchivePasswordCache.getInstance().get(filePath).toCharArray())
            : new SevenZFile(new File(filePath));

    ArrayList<SevenZArchiveEntry> arrayList = new ArrayList<>();

    // iterating archive elements to find file names that are to be extracted
    for (SevenZArchiveEntry entry : sevenzFile.getEntries()) {
      if (filter.shouldExtract(entry.getName(), entry.isDirectory())) {
        // Entry to be extracted is at least the entry path (may be more, when it is a directory)
        arrayList.add(entry);
        totalBytes += entry.getSize();
      }
    }

    listener.onStart(totalBytes, arrayList.get(0).getName());

    SevenZArchiveEntry entry;
    while ((entry = sevenzFile.getNextEntry()) != null) {
      if (!arrayList.contains(entry)) {
        continue;
      }
      if (!listener.isCancelled()) {
        listener.onUpdate(entry.getName());
        extractEntry(context, sevenzFile, entry, outputPath);
      }
    }
    sevenzFile.close();
    listener.onFinish();
  }

  private void extractEntry(
      @NonNull final Context context,
      SevenZFile sevenzFile,
      SevenZArchiveEntry entry,
      String outputDir)
      throws IOException {
    String name = entry.getName();

    if (entry.isDirectory()) {
      FileUtil.mkdir(new File(outputDir, name), context);
      return;
    }
    File outputFile = new File(outputDir, name);
    if (!outputFile.getParentFile().exists()) {
      FileUtil.mkdir(outputFile.getParentFile(), context);
    }

    BufferedOutputStream outputStream =
        new BufferedOutputStream(FileUtil.getOutputStream(outputFile, context));

    byte[] content = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
    long progress = 0;
    try {
      while (progress < entry.getSize()) {
        int length;
        int bytesLeft = Long.valueOf(entry.getSize() - progress).intValue();
        length =
            sevenzFile.read(
                content,
                0,
                bytesLeft > GenericCopyUtil.DEFAULT_BUFFER_SIZE
                    ? GenericCopyUtil.DEFAULT_BUFFER_SIZE
                    : bytesLeft);
        outputStream.write(content, 0, length);
        updatePosition.updatePosition(length);
        progress += length;
      }
    } finally {
      outputStream.close();
      outputFile.setLastModified(entry.getLastModifiedDate().getTime());
    }
  }
}
