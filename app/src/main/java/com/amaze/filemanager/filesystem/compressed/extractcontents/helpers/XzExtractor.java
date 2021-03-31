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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import com.amaze.filemanager.file_operations.utils.UpdatePosition;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;

import android.content.Context;

import androidx.annotation.NonNull;

public class XzExtractor extends Extractor {

  public XzExtractor(
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
    ArrayList<TarArchiveEntry> archiveEntries = new ArrayList<>();
    TarArchiveInputStream inputStream =
        new TarArchiveInputStream(new XZCompressorInputStream(new FileInputStream(filePath)));

    TarArchiveEntry tarArchiveEntry;

    while ((tarArchiveEntry = inputStream.getNextTarEntry()) != null) {
      if (filter.shouldExtract(tarArchiveEntry.getName(), tarArchiveEntry.isDirectory())) {
        archiveEntries.add(tarArchiveEntry);
        totalBytes += tarArchiveEntry.getSize();
      }
    }

    listener.onStart(totalBytes, archiveEntries.get(0).getName());

    inputStream.close();
    inputStream =
        new TarArchiveInputStream(new XZCompressorInputStream(new FileInputStream(filePath)));

    for (TarArchiveEntry entry : archiveEntries) {
      if (!listener.isCancelled()) {
        listener.onUpdate(entry.getName());
        // TAR is sequential, you need to walk all the way to the file you want
        while (entry.hashCode() != inputStream.getNextTarEntry().hashCode()) ;
        extractEntry(context, inputStream, entry, outputPath);
      }
    }
    inputStream.close();

    listener.onFinish();
  }

  private void extractEntry(
      @NonNull final Context context,
      TarArchiveInputStream inputStream,
      TarArchiveEntry entry,
      String outputDir)
      throws IOException {
    if (entry.isDirectory()) {
      FileUtil.mkdir(new File(outputDir, entry.getName()), context);
      return;
    }

    File outputFile = new File(outputDir, entry.getName());
    if (!outputFile.getParentFile().exists()) {
      FileUtil.mkdir(outputFile.getParentFile(), context);
    }

    BufferedOutputStream outputStream =
        new BufferedOutputStream(FileUtil.getOutputStream(outputFile, context));
    try {
      int len;
      byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
      while ((len = inputStream.read(buf)) != -1) {
        outputStream.write(buf, 0, len);
        updatePosition.updatePosition(len);
      }
    } finally {
      outputStream.close();
      outputFile.setLastModified(entry.getModTime().getTime());
    }
  }
}
