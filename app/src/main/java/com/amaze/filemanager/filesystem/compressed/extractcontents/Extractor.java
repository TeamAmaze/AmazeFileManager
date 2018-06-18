/*
 * Extractor.java
 *
 * Copyright (C) 2018 Emmanuel Messulam<emmanuelbendavid@gmail.com>,
 * Raymond Lai <airwave209gt@gmail.com> and Contributors.
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

package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.content.Context;
import android.support.annotation.NonNull;

import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarExtractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class Extractor {

    protected Context context;
    protected String filePath, outputPath;
    protected OnUpdate listener;
    protected List<String> invalidArchiveEntries;

    public Extractor(Context context, String filePath, String outputPath,
                     Extractor.OnUpdate listener) {
        this.context = context;
        this.filePath = filePath;
        this.outputPath = outputPath;
        this.listener = listener;
        this.invalidArchiveEntries = new ArrayList<>();
    }

    public void extractFiles(String[] files) throws IOException {
        HashSet<String> filesToExtract = new HashSet<>(files.length);
        Collections.addAll(filesToExtract, files);

        extractWithFilter((relativePath, isDir) -> {
            if(filesToExtract.contains(relativePath)) {
                if(!isDir) filesToExtract.remove(relativePath);
                return true;
            } else {// header to be extracted is at least the entry path (may be more, when it is a directory)
                for (String path : filesToExtract) {
                    if(relativePath.startsWith(path)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void extractEverything() throws IOException {
        extractWithFilter((relativePath, isDir) -> true);
    }

    public List<String> getInvalidArchiveEntries(){
        return invalidArchiveEntries;
    }

    protected abstract void extractWithFilter(@NonNull Filter filter) throws IOException;

    protected interface Filter {
        boolean shouldExtract(String relativePath, boolean isDirectory);
    }

    public interface OnUpdate {
        void onStart(long totalBytes, String firstEntryName);
        void onUpdate(String entryPath);
        void onFinish();
        boolean isCancelled();
    }

    protected String fixEntryName(String entryName){
        if(entryName.indexOf('\\') > 0) {
            return entryName.replaceAll("\\\\", CompressedHelper.SEPARATOR);
        } else {
            return entryName;
        }
    }
}
