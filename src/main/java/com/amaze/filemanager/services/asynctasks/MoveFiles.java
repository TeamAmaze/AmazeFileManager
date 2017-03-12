/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>
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

package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static com.amaze.filemanager.activities.BaseActivity.rootMode;

public class MoveFiles extends AsyncTask<ArrayList<String>,Void,Boolean> {
    private ArrayList<ArrayList<BaseFile>> files;
    private Main main;
    private ArrayList<String> paths;
    private Context context;
    private OpenMode mode;

    public MoveFiles(ArrayList<ArrayList<BaseFile>> files, Main ma, Context context, OpenMode mode) {
        main = ma;
        this.context = context;
        this.files = files;
        this.mode = mode;
    }

    @Override
    protected Boolean doInBackground(ArrayList<String>... strings) {
        paths = strings[0];
        boolean movedCorrectly = true;

        if (files.size() == 0) return true;

        if (mode != OpenMode.FILE) return false;

        for (int i = 0; i < paths.size(); i++) {
            for (BaseFile f : files.get(i)) {
                File dest = new File(paths.get(i) + "/" + f.getName());
                File source = new File(f.getPath());
                if (!source.renameTo(dest)) {
                    movedCorrectly = false;
                }
            }
        }

        return movedCorrectly;
    }

    @Override
    public void onPostExecute(Boolean movedCorrectly) {
        if (movedCorrectly) {
            if (main != null && main.CURRENT_PATH.equals(paths.get(0)))
                    main.updateList();

            for (int i = 0; i < paths.size(); i++) {
                for (BaseFile f : files.get(i)) {
                    Futils.scanFile(f.getPath(), context);
                    Futils.scanFile(paths.get(i) + "/" + f.getName(), context);
                }
            }
        } else {
            for (int i = 0; i < paths.size(); i++) {
                Intent intent = new Intent(context, CopyService.class);
                intent.putExtra(CopyService.TAG_COPY_SOURCES, files.get(i));
                intent.putExtra(CopyService.TAG_COPY_TARGET, paths.get(i));
                intent.putExtra(CopyService.TAG_COPY_MOVE, true);
                intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, mode.ordinal());

                ServiceWatcherUtil.runService(context, intent);
            }
        }

        //final folder cleaning
        Collections.reverse(files);
        for (ArrayList<BaseFile> folder : files) {
            BaseFile folderPath = new BaseFile(folder.get(0).getParent());

            try {
                if (folderPath.listFiles(rootMode).size() == 0)
                    folderPath.delete(context, rootMode);
            } catch (RootNotPermittedException e) {
                e.printStackTrace();
            }
        }
    }
}
