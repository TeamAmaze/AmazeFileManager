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

import com.amaze.filemanager.filesystem.operations.extract.AbstractExtractOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;

public class Extractor<T extends AbstractExtractOperation> {

    private final FunctionOperation operationCtor;
    protected List<String> invalidArchiveEntries = new ArrayList<>();

    public Extractor(FunctionOperation operationCtor) {
        this.operationCtor = operationCtor;
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
                    if(relativePath.startsWith(path) || relativePath.startsWith("/"+path)) {
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

    protected void extractWithFilter(@NonNull AbstractExtractOperation.Filter filter) throws IOException {
        AbstractExtractOperation extract = operationCtor.filter(filter);
        extract.start();
        invalidArchiveEntries.addAll(extract.getInvalidArchiveEntries());
    }

    public interface FunctionOperation<T extends AbstractExtractOperation> {
        T filter(AbstractExtractOperation.Filter f);
    }
}
