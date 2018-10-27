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

package com.amaze.filemanager.asynchronous.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.interfaces.SMS;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * AsyncTask that moves files from source to destination by trying to rename files first,
 * if they're in the same filesystem, else starting the copy service.
 * Be advised - do not start this AsyncTask directly but use {@link PrepareCopyTask} instead
 */
public class MoveFiles extends AsyncTask<ArrayList<String>, String, Boolean> {

    private ArrayList<ArrayList<HybridFileParcelable>> files;
    private MainFragment mainFrag;
    private ArrayList<String> paths;
    private Context context;
    private OpenMode mode;
    private long totalBytes = 0l;
    private long destinationSize = 0l;

    public MoveFiles(ArrayList<ArrayList<HybridFileParcelable>> files, MainFragment ma, Context context, OpenMode mode) {
        mainFrag = ma;
        this.context = context;
        this.files = files;
        this.mode = mode;
    }

    @Override
    protected Boolean doInBackground(ArrayList<String>... strings) {
        paths = strings[0];

        if (files.size() == 0) return true;

        for (ArrayList<HybridFileParcelable> filesCurrent : files) {
            totalBytes += FileUtils.getTotalBytes(filesCurrent, context);
        }
        HybridFile destination = new HybridFile(mode, paths.get(0));
        destinationSize = destination.getUsableSpace();

        switch (mode) {
            case SMB:
                for (int i = 0; i < paths.size(); i++) {
                    for (HybridFileParcelable f : files.get(i)) {
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
                    for (HybridFileParcelable f : files.get(i)) {
                        File dest = new File(paths.get(i) + "/" + f.getName());
                        File source = new File(f.getPath());
                        if (!source.renameTo(dest)) {

                            // check if we have root
                            if (mainFrag.getMainActivity().isRootExplorer()) {
                                try {
                                    if (!RootUtils.rename(f.getPath(), paths.get(i) + "/" + f.getName()))
                                        return false;
                                } catch (ShellNotRunningException e) {
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
                    for (HybridFileParcelable baseFile : files.get(i)) {

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
                Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);

                intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, paths.get(0));
                context.sendBroadcast(intent);
            }

            for (int i = 0; i < paths.size(); i++) {
                for (HybridFileParcelable f : files.get(i)) {
                    FileUtils.scanFile(f.getFile(), context);
                    FileUtils.scanFile(new File(paths.get(i) + "/" + f.getName()), context);
                }
            }

            // updating encrypted db entry if any encrypted file was moved
            AppConfig.runInBackground(() -> {
                for (int i = 0; i < paths.size(); i++) {
                    for (HybridFileParcelable file : files.get(i)) {
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
            });

        } else {

            if (destinationSize < totalBytes) {
                // destination don't have enough space; return
                Toast.makeText(context, context.getResources().getString(R.string.in_safe), Toast.LENGTH_LONG).show();
                return;
            }

            for (int i = 0; i < paths.size(); i++) {
                Intent intent = new Intent(context, CopyService.class);
                intent.putExtra(CopyService.TAG_COPY_SOURCES, files.get(i));
                intent.putExtra(CopyService.TAG_COPY_TARGET, paths.get(i));
                intent.putExtra(CopyService.TAG_COPY_MOVE, true);
                intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, mode.ordinal());
                intent.putExtra(CopyService.TAG_IS_ROOT_EXPLORER, mainFrag.getMainActivity().isRootExplorer());

                ServiceWatcherUtil.runService(context, intent);
            }
        }
    }

    /**
     * Maintains a list of filesystems supporting the move/rename implementation.
     * Please update to return your {@link OpenMode} type if it is supported here
     * @return
     */
    public static HashSet<OpenMode> getOperationSupportedFileSystem() {
        HashSet<OpenMode> hashSet = new HashSet<>();
        hashSet.add(OpenMode.SMB);
        hashSet.add(OpenMode.FILE);
        hashSet.add(OpenMode.DROPBOX);
        hashSet.add(OpenMode.BOX);
        hashSet.add(OpenMode.GDRIVE);
        hashSet.add(OpenMode.ONEDRIVE);
        return hashSet;
    }
}
