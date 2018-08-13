/*
 * ZipHelperTask.java
 *
 * Copyright (C) 2014-2018 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import android.net.Uri;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.application.AppConfig;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipHelperTask extends CompressedHelperTask {

    private WeakReference<Context> context;
    private Uri fileLocation;
    private String relativeDirectory;

    /**
     * AsyncTask to load ZIP file items.
     * @param realFileDirectory the location of the zip file
     * @param dir relativeDirectory to access inside the zip file
     */
    public ZipHelperTask(Context c, String realFileDirectory, String dir, boolean goback,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goback, l);
        context = new WeakReference<>(c);
        fileLocation = Uri.parse(realFileDirectory);
        relativeDirectory = dir;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        try {
            ArrayList<CompressedObjectParcelable> wholelist = new ArrayList<>();
            if (new File(fileLocation.getPath()).canRead()) {
                ZipFile zipfile = new ZipFile(fileLocation.getPath());
                for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    if (!CompressedHelper.isEntryPathValid(entry.getName())) {
                        AppConfig.toast(context.get(), context.get().getString(R.string.multiple_invalid_archive_entries));
                        continue;
                    }
                    wholelist.add(new CompressedObjectParcelable(entry.getName(), entry.getTime(), entry.getSize(), entry.isDirectory()));
                }
            } else {
                ZipInputStream zipfile1 = new ZipInputStream(context.get().getContentResolver().openInputStream(fileLocation));
                for (ZipEntry entry = zipfile1.getNextEntry(); entry != null; entry = zipfile1.getNextEntry()) {
                    if (!CompressedHelper.isEntryPathValid(entry.getName())){
                        AppConfig.toast(context.get(), context.get().getString(R.string.multiple_invalid_archive_entries));
                        continue;
                    }
                    wholelist.add(new CompressedObjectParcelable(entry.getName(), entry.getTime(), entry.getSize(), entry.isDirectory()));
                }
            }

            ArrayList<String> strings = new ArrayList<>();

            for (CompressedObjectParcelable entry : wholelist) {
                File file = new File(entry.path);
                if (relativeDirectory == null || relativeDirectory.trim().length() == 0) {
                    String y = entry.path;
                    if (y.startsWith("/"))
                        y = y.substring(1, y.length());
                    if (file.getParent() == null || file.getParent().length() == 0 || file.getParent().equals("/")) {
                        if (!strings.contains(y)) {
                            elements.add(new CompressedObjectParcelable(y, entry.date, entry.size, entry.directory));
                            strings.add(y);
                        }
                    } else {
                        String path = y.substring(0, y.indexOf("/") + 1);
                        if (!strings.contains(path)) {
                            CompressedObjectParcelable zipObj = new CompressedObjectParcelable(path, entry.date, entry.size, true);
                            strings.add(path);
                            elements.add(zipObj);
                        }
                    }
                } else {
                    String y = entry.path;
                    if (entry.path.startsWith("/"))
                        y = y.substring(1, y.length());

                    if (file.getParent() != null && (file.getParent().equals(relativeDirectory) || file.getParent().equals("/" + relativeDirectory))) {
                        if (!strings.contains(y)) {
                            elements.add(new CompressedObjectParcelable(y, entry.date, entry.size, entry.directory));
                            strings.add(y);
                        }
                    } else {
                        if (y.startsWith(relativeDirectory + "/") && y.length() > relativeDirectory.length() + 1) {
                            String path1 = y.substring(relativeDirectory.length() + 1, y.length());

                            int index = relativeDirectory.length() + 1 + path1.indexOf("/");
                            String path = y.substring(0, index + 1);
                            if (!strings.contains(path)) {
                                CompressedObjectParcelable zipObj = new CompressedObjectParcelable(y.substring(0, index + 1), entry.date, entry.size, true);
                                strings.add(path);
                                elements.add(zipObj);
                            }
                        }
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
