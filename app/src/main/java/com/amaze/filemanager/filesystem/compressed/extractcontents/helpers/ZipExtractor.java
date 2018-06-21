/*
 * ZipExtractor.java
 *
 * Copyright (C) 2018 Emmanuel Messulam<emmanuelbendavid@gmail.com>,
 * Raymond Lai <airwave209gt@gmail.com>.
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

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipExtractor extends Extractor {

    public ZipExtractor(Context context, String filePath, String outputPath, OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        long totalBytes = 0;
        List<ZipEntry> entriesToExtract = new ArrayList<>();
        ZipFile zipfile = new ZipFile(filePath);

        // iterating archive elements to find file names that are to be extracted
        for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements(); ) {
            ZipEntry zipEntry = e.nextElement();

            if(CompressedHelper.isEntryPathValid(zipEntry.getName())) {
                if (filter.shouldExtract(zipEntry.getName(), zipEntry.isDirectory())) {
                    entriesToExtract.add(zipEntry);
                    totalBytes += zipEntry.getSize();
                }
            } else {
                invalidArchiveEntries.add(zipEntry.getName());
            }
        }

        listener.onStart(totalBytes, entriesToExtract.get(0).getName());

        for (ZipEntry entry : entriesToExtract) {
            if (!listener.isCancelled()) {
                listener.onUpdate(entry.getName());
                extractEntry(context, zipfile, entry, outputPath);
            }
        }
        listener.onFinish();
    }
    
    /**
     * Method extracts {@link ZipEntry} from {@link ZipFile}
     *
     * @param zipFile   zip file from which entriesToExtract are to be extracted
     * @param entry     zip entry that is to be extracted
     * @param outputDir output directory
     */
    private void extractEntry(@NonNull final Context context, ZipFile zipFile, ZipEntry entry,
                              String outputDir) throws IOException {
        final File outputFile = new File(outputDir, fixEntryName(entry.getName()));

        if (!outputFile.getCanonicalPath().startsWith(outputDir)){
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
        BufferedOutputStream outputStream = new BufferedOutputStream(FileUtil.getOutputStream(outputFile, context));

        try {
            int len;
            byte buf[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
            while ((len = inputStream.read(buf)) != -1) {
                if (!listener.isCancelled()) {
                    outputStream.write(buf, 0, len);
                    ServiceWatcherUtil.position += len;
                } else break;
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

}
