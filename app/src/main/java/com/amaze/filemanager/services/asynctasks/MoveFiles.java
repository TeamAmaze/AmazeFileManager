/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Emmanuel Messulam<emmanuelbendavid@gmail.com>
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

import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.Futils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * AsyncTask that moves files from source to destination by trying to rename files first,
 * if they're in the same filesystem, else starting the copy service.
 * Be advised - do not start this AsyncTask directly but use {@link CopyFileCheck} instead
 */
public class MoveFiles extends AsyncTask<ArrayList<String>, Void, Boolean> {

    private ArrayList<ArrayList<BaseFile>> files;
    private MainFragment mainFrag;
    private ArrayList<String> paths;
    private Context context;
    private OpenMode mode;

    public MoveFiles(ArrayList<ArrayList<BaseFile>> files, MainFragment ma, Context context, OpenMode mode) {
        mainFrag = ma;
        this.context = context;
        this.files = files;
        this.mode = mode;
    }

    @Override
    protected Boolean doInBackground(ArrayList<String>... strings) {
        paths = strings[0];

        if (files.size() == 0) return true;

        switch (mode) {
            case SMB:
                for (int i = 0; i < paths.size(); i++) {
                    for (BaseFile f : files.get(i)) {
                        try {
                            SmbFile source = new SmbFile(f.getPath());
                            SmbFile dest = new SmbFile(paths.get(i) + "/" + f.getName());
                            source.renameTo(dest);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            return false;
                        } catch (SmbException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                }
                break;
            case FILE:
                for (int i = 0; i < paths.size(); i++) {
                    for (BaseFile f : files.get(i)) {
                        File dest = new File(paths.get(i) + "/" + f.getName());
                        File source = new File(f.getPath());
                        if (!source.renameTo(dest)) {

                            // check if we have root
                            if (BaseActivity.rootMode) {
                                try {
                                    if (!RootUtils.rename(f.getPath(), paths.get(i) + "/" + f.getName()))
                                        return false;
                                } catch (RootNotPermittedException e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            } else return false;
                        }
                    }
                }
                break;
            case DROPBOX:
            case BOX:
            case ONEDRIVE:
            case GDRIVE:
                for (int i=0; i<paths.size(); i++) {
                    for (BaseFile baseFile : files.get(i)) {

                        DataUtils dataUtils = DataUtils.getInstance();

                        CloudStorage cloudStorage = dataUtils.getAccount(mode);
                        String targetPath = paths.get(i) + "/" + baseFile.getName();
                        if (baseFile.getMode() == mode) {
                            // source and target both in same filesystem, use API method
                            try {

                                cloudStorage.move(CloudUtil.stripPath(mode, baseFile.getPath()),
                                        CloudUtil.stripPath(mode, targetPath));
                            } catch (Exception e) {
                                return false;
                            }
                        }  else {
                            // not in same filesystem, execute service
                            return false;
                        }
                    }
                }
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onPostExecute(Boolean movedCorrectly) {
        if (movedCorrectly) {
            if (mainFrag != null && mainFrag.getCurrentPath().equals(paths.get(0))) {
                // mainFrag.updateList();
                Intent intent = new Intent("loadlist");
                context.sendBroadcast(intent);
            }

            for (int i = 0; i < paths.size(); i++) {
                for (BaseFile f : files.get(i)) {
                    Futils.scanFile(f.getPath(), context);
                    Futils.scanFile(paths.get(i) + "/" + f.getName(), context);
                }
            }

            // updating encrypted db entry if any encrypted file was moved
            AppConfig.runInBackground(new Runnable() {
                @Override
                public void run() {
                    for (int i=0; i<paths.size(); i++) {
                        for (BaseFile file : files.get(i)) {
                            if (file.getName().endsWith(CryptUtil.CRYPT_EXTENSION)) {
                                try {

                                    CryptHandler cryptHandler = new CryptHandler(context);
                                    EncryptedEntry oldEntry = cryptHandler.findEntry(file.getPath());
                                    EncryptedEntry newEntry = new EncryptedEntry();
                                    newEntry.setId(oldEntry.getId());
                                    newEntry.setPassword(oldEntry.getPassword());
                                    newEntry.setPath(paths.get(i) + "/" + file.getName());
                                    cryptHandler.updateEntry(oldEntry, newEntry);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // couldn't change the entry, leave it alone
                                }
                            }
                        }
                    }
                }
            });

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
    }
}
