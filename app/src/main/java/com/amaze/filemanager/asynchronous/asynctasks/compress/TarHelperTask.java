/*
 * TarHelperTask.java
 *
 * Copyright (C) 2017-2018 Emmanuel Messulam<emmanuelbendavid@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.content.Context;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

public class TarHelperTask extends CompressedHelperTask {

    private String filePath, relativePath;

    public TarHelperTask(String filePath, String relativePath, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        TarArchiveInputStream tarInputStream = null;
        try {
            tarInputStream = new TarArchiveInputStream(new FileInputStream(filePath));

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                String name = entry.getName();
                if (!CompressedHelper.isEntryPathValid(name)) {
                    continue;
                }
                if (name.endsWith(SEPARATOR)) name = name.substring(0, name.length() - 1);

                boolean isInBaseDir = relativePath.equals("") && !name.contains(SEPARATOR);
                boolean isInRelativeDir = name.contains(SEPARATOR)
                        && name.substring(0, name.lastIndexOf(SEPARATOR)).equals(relativePath);

                if (isInBaseDir || isInRelativeDir) {
                    elements.add(new CompressedObjectParcelable(entry.getName(),
                            entry.getLastModifiedDate().getTime(), entry.getSize(), entry.isDirectory()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
