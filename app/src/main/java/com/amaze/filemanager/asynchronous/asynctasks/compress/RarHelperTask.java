/*
 * RarHelperTask.java
 *
 * Copyright (C) 2015-2018 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt@gmail.com> and Contributors.
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
import com.amaze.filemanager.filesystem.compressed.showcontents.helpers.RarDecompressor;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RarHelperTask extends CompressedHelperTask {

    private String fileLocation;
    private String relativeDirectory;

    /**
     * AsyncTask to load RAR file items.
     * @param realFileDirectory the location of the zip file
     * @param dir relativeDirectory to access inside the zip file
     */
    public RarHelperTask(String realFileDirectory, String dir, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        fileLocation = realFileDirectory;
        relativeDirectory = dir;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        try {
            Archive zipfile = new Archive(new File(fileLocation));
            String relativeDirDiffSeparator = relativeDirectory.replace(CompressedHelper.SEPARATOR, "\\");

            for (FileHeader rarArchive : zipfile.getFileHeaders()) {
                String name = rarArchive.getFileNameString();//This uses \ as separator, not /
                if (!CompressedHelper.isEntryPathValid(name)) {
                    continue;
                }
                boolean isInBaseDir = (relativeDirDiffSeparator == null || relativeDirDiffSeparator.equals("")) && !name.contains("\\");
                boolean isInRelativeDir = relativeDirDiffSeparator != null && name.contains("\\")
                        && name.substring(0, name.lastIndexOf("\\")).equals(relativeDirDiffSeparator);

                if (isInBaseDir || isInRelativeDir) {
                    elements.add(new CompressedObjectParcelable(RarDecompressor.convertName(rarArchive), 0, rarArchive.getDataSize(), rarArchive.isDirectory()));
                }
            }
        } catch (RarException | IOException e) {
            e.printStackTrace();
        }
    }

}

