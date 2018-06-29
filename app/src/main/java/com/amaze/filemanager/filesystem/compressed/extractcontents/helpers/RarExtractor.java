/*
 * RarExtractor.java
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
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class RarExtractor extends Extractor {

    public RarExtractor(Context context, String filePath, String outputPath, OnUpdate listener) {
        super(context, filePath, outputPath, listener);
    }

    @Override
    protected void extractWithFilter(@NonNull Filter filter) throws IOException {
        try {
            long totalBytes = 0;
            Archive rarFile = new Archive(new File(filePath));
            ArrayList<FileHeader> arrayList = new ArrayList<>();

            // iterating archive elements to find file names that are to be extracted
            for (FileHeader header : rarFile.getFileHeaders()) {
                if(CompressedHelper.isEntryPathValid(header.getFileNameString())) {
                    if (filter.shouldExtract(header.getFileNameString(), header.isDirectory())) {
                        // header to be extracted is at least the entry path (may be more, when it is a directory)
                        arrayList.add(header);
                        totalBytes += header.getFullUnpackSize();
                    }
                } else {
                    invalidArchiveEntries.add(header.getFileNameString());
                }
            }

            listener.onStart(totalBytes, arrayList.get(0).getFileNameString());

            for (FileHeader entry : arrayList) {
                if (!listener.isCancelled()) {
                    listener.onUpdate(entry.getFileNameString());
                    extractEntry(context, rarFile, entry, outputPath);
                }
            }
            listener.onFinish();
        } catch (RarException e) {
            throw new IOException(e);
        }
    }

    private void extractEntry(@NonNull final Context context, Archive zipFile, FileHeader entry, String outputDir)
            throws RarException, IOException {
        String name = fixEntryName(entry.getFileNameString()).replaceAll("\\\\", CompressedHelper.SEPARATOR);
        File outputFile = new File(outputDir, name);

        if (!outputFile.getCanonicalPath().startsWith(outputDir)){
            throw new IOException("Incorrect RAR FileHeader path!");
        }

        if (entry.isDirectory()) {
            FileUtil.mkdir(outputFile, context);
            return;
        }

        if (!outputFile.getParentFile().exists()) {
            FileUtil.mkdir(outputFile.getParentFile(), context);
        }
        //	Log.i("Amaze", "Extracting: " + entry);
        BufferedInputStream inputStream = new BufferedInputStream(
                zipFile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(
                FileUtil.getOutputStream(outputFile, context));
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
