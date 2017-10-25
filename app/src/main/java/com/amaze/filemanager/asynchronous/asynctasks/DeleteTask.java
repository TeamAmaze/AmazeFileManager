/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.ZipExplorerFragment;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.cloudrail.si.interfaces.CloudStorage;

import java.util.ArrayList;

public class DeleteTask extends AsyncTask<ArrayList<HybridFileParcelable>, String, Boolean> {

    private ArrayList<HybridFileParcelable> files;
    private Context cd;
    private boolean rootMode;
    private ZipExplorerFragment zipExplorerFragment;
    private DataUtils dataUtils = DataUtils.getInstance();

    public DeleteTask(ContentResolver c, Context cd) {
        this.cd = cd;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
    }

    public DeleteTask(ContentResolver c, Context cd, ZipExplorerFragment zipExplorerFragment) {
        this.cd = cd;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
        this.zipExplorerFragment = zipExplorerFragment;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Toast.makeText(cd, values[0], Toast.LENGTH_SHORT).show();
    }

    protected Boolean doInBackground(ArrayList<HybridFileParcelable>... p1) {
        files = p1[0];
        boolean b = true;
        if(files.size()==0)return true;

        if (files.get(0).isOtgFile()) {
            for (HybridFileParcelable a : files) {

                DocumentFile documentFile = OTGUtil.getDocumentFile(a.getPath(), cd, false);
                 b = documentFile.delete();
            }
        } else if (files.get(0).isDropBoxFile()) {
            CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
            for (HybridFileParcelable baseFile : files) {
                try {
                    cloudStorageDropbox.delete(CloudUtil.stripPath(OpenMode.DROPBOX, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else if (files.get(0).isBoxFile()) {
            CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
            for (HybridFileParcelable baseFile : files) {
                try {
                    cloudStorageBox.delete(CloudUtil.stripPath(OpenMode.BOX, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else if (files.get(0).isGoogleDriveFile()) {
            CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
            for (HybridFileParcelable baseFile : files) {
                try {
                    cloudStorageGdrive.delete(CloudUtil.stripPath(OpenMode.GDRIVE, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else if (files.get(0).isOneDriveFile()) {
            CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
            for (HybridFileParcelable baseFile : files) {
                try {
                    cloudStorageOnedrive.delete(CloudUtil.stripPath(OpenMode.ONEDRIVE, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else {

            for(HybridFileParcelable a : files)
                try {
                    (a).delete(cd, rootMode);
                } catch (RootNotPermittedException e) {
                    e.printStackTrace();
                    b = false;
                }
        }

        // delete file from media database
        if(!files.get(0).isSmb()) {
            try {
                for (HybridFileParcelable f : files) {
                    delete(cd,f.getPath());
                }
            } catch (Exception e) {
                for (HybridFileParcelable f : files) {
                    FileUtils.scanFile(f.getPath(), cd);
                }
            }
        }

        // delete file entry from encrypted database
        for (HybridFileParcelable file : files) {
            if (file.getName().endsWith(CryptUtil.CRYPT_EXTENSION)) {
                CryptHandler handler = new CryptHandler(cd);
                handler.clear(file.getPath());
            }
        }

        return b;
    }

    @Override
    public void onPostExecute(Boolean b) {

        Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
        String path = files.get(0).getParent(cd);
        intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, path);
        cd.sendBroadcast(intent);

        if (!b) {
            Toast.makeText(cd, cd.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
        } else if (zipExplorerFragment ==null) {
            Toast.makeText(cd, cd.getResources().getString(R.string.done), Toast.LENGTH_SHORT).show();
        }

        if (zipExplorerFragment !=null) {
            zipExplorerFragment.files.clear();
        }
    }

    private void delete(final Context context, final String file) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[] {
                file
        };
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        // Delete the entry from the media database. This will actually delete media files.
        contentResolver.delete(filesUri, where, selectionArgs);
    }
}



