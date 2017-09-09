/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *     Emmanuel Messulam <emmanuelbendavid@gmail.com>
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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.ui.LayoutElementParcelable;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.FileListSorter;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class LoadFilesListTask extends AsyncTask<String, String, ArrayList<LayoutElementParcelable>> {

    private UtilitiesProviderInterface utilsProvider;
    private String path;
    private boolean back;
    private MainFragment ma;
    private Context c;
    private OpenMode openmode;
    private boolean grid;
    private DataUtils dataUtils = DataUtils.getInstance();

    public LoadFilesListTask(Context c, UtilitiesProviderInterface utilsProvider, boolean back,
                             MainFragment ma, OpenMode openmode) {
        this.utilsProvider = utilsProvider;
        this.back = back;
        this.ma = ma;
        this.openmode = openmode;
        this.c = c;
    }

    @Override
    protected void onPreExecute() {
        ma.mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected ArrayList<LayoutElementParcelable> doInBackground(String... params) {// params[0] is the url.
        ArrayList<LayoutElementParcelable> list = null;
        path = params[0];
        grid = ma.checkPathIsGrid(path);
        ma.folder_count = 0;
        ma.file_count = 0;
        if (openmode == OpenMode.UNKNOWN) {
            HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, path);
            hFile.generateMode(ma.getActivity());

            if (hFile.isLocal()) {
                openmode = OpenMode.FILE;
            } else if (hFile.isSmb()) {
                openmode = OpenMode.SMB;
                ma.smbPath = path;
            } else if (hFile.isOtgFile()) {
                openmode = OpenMode.OTG;
            } else if (hFile.isBoxFile()) {
                openmode = OpenMode.BOX;
            } else if (hFile.isDropBoxFile()) {
                openmode = OpenMode.DROPBOX;
            } else if (hFile.isGoogleDriveFile()) {
                openmode = OpenMode.GDRIVE;
            } else if (hFile.isOneDriveFile()) {
                openmode = OpenMode.ONEDRIVE;
            } else if (hFile.isCustomPath())
                openmode = OpenMode.CUSTOM;
            else if (android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches()) {
                openmode = OpenMode.ROOT;
            }
        }

        switch (openmode) {
            case SMB:
                HybridFile hFile = new HybridFile(OpenMode.SMB, path);
                try {
                    SmbFile[] smbFile = hFile.getSmbFile(5000).listFiles();
                    list = ma.addToSmb(smbFile, path);
                    openmode = OpenMode.SMB;
                } catch (SmbAuthException e) {
                    if (!e.getMessage().toLowerCase().contains("denied"))
                        ma.reauthenticateSmb();
                    publishProgress(e.getLocalizedMessage());
                } catch (SmbException | NullPointerException e) {
                    publishProgress(e.getLocalizedMessage());
                    e.printStackTrace();
                }
                break;
            case CUSTOM:
                ArrayList<HybridFileParcelable> arrayList = null;
                switch (Integer.parseInt(path)) {
                    case 0:
                        arrayList = listImages();
                        break;
                    case 1:
                        arrayList = listVideos();
                        break;
                    case 2:
                        arrayList = listaudio();
                        break;
                    case 3:
                        arrayList = listDocs();
                        break;
                    case 4:
                        arrayList = listApks();
                        break;
                    case 5:
                        arrayList = listRecent();
                        break;
                    case 6:
                        arrayList = listRecentFiles();
                        break;
                }

                path = String.valueOf(Integer.parseInt(path));

                if (arrayList != null)
                    list = addTo(arrayList);
                else return new ArrayList<>();
                break;
            case OTG:
                list = addTo(listOtg(path));
                openmode = OpenMode.OTG;
                break;
            case DROPBOX:

                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);

                try {
                    list = addTo(listCloud(path, cloudStorageDropbox, OpenMode.DROPBOX));
                } catch (CloudPluginException e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
                break;
            case BOX:
                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);

                try {
                    list = addTo(listCloud(path, cloudStorageBox, OpenMode.BOX));
                } catch (CloudPluginException e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
                break;
            case GDRIVE:
                CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);

                try {
                    list = addTo(listCloud(path, cloudStorageGDrive, OpenMode.GDRIVE));
                } catch (CloudPluginException e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
                break;
            case ONEDRIVE:
                CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

                try {
                    list = addTo(listCloud(path, cloudStorageOneDrive, OpenMode.ONEDRIVE));
                } catch (CloudPluginException e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
                break;
            default:
                // we're neither in OTG not in SMB, load the list based on root/general filesystem
                try {
                    ArrayList<HybridFileParcelable> arrayList1;
                    arrayList1 = RootHelper.getFilesList(path, ThemedActivity.rootMode, ma.SHOW_HIDDEN,
                            new RootHelper.GetModeCallBack() {
                                @Override
                                public void getMode(OpenMode mode) {
                                    openmode = mode;
                                }
                            });
                    list = addTo(arrayList1);

                } catch (RootNotPermittedException e) {
                    //AppConfig.toast(c, c.getString(R.string.rootfailure));
                    return null;
                }
                break;
        }

        if (list != null && !(openmode == OpenMode.CUSTOM && ((path).equals("5") || (path).equals("6")))) {
            Collections.sort(list, new FileListSorter(ma.dsort, ma.sortby, ma.asc));
        }

        return list;
    }

    @Override
    protected void onPostExecute(ArrayList<LayoutElementParcelable> list) {
        if (isCancelled()) {
            list = null;
        }

        ma.createViews(list, back, path, openmode, false, grid);
        ma.mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(c, message[0], Toast.LENGTH_SHORT).show();
    }

    private ArrayList<LayoutElementParcelable> addTo(ArrayList<HybridFileParcelable> baseFiles) {
        ArrayList<LayoutElementParcelable> items = new ArrayList<>();

        for (int i = 0; i < baseFiles.size(); i++) {
            HybridFileParcelable baseFile = baseFiles.get(i);
            //File f = new File(ele.getPath());
            String size = "";
            if (!dataUtils.getHiddenfiles().contains(baseFile.getPath())) {
                if (baseFile.isDirectory()) {
                    size = "";

                    Bitmap lockBitmap = BitmapFactory.decodeResource(ma.getResources(), R.drawable.ic_folder_lock_white_36dp);
                    BitmapDrawable lockBitmapDrawable = new BitmapDrawable(ma.getResources(), lockBitmap);

                    LayoutElementParcelable layoutElement = FileUtils.newElement(
                            baseFile.getName().endsWith(CryptUtil.CRYPT_EXTENSION) ? lockBitmapDrawable:ma.folder,
                                    baseFile.getPath(), baseFile.getPermission(), baseFile.getLink(), size, 0, true, false,
                                    baseFile.getDate() + "");
                    layoutElement.setMode(baseFile.getMode());
                    items.add(layoutElement);
                    ma.folder_count++;
                } else {
                    long longSize = 0;
                    try {
                        if (baseFile.getSize() != -1) {
                            longSize = baseFile.getSize();
                            size = Formatter.formatFileSize(c, longSize);
                        } else {
                            size = "";
                            longSize = 0;
                        }
                    } catch (NumberFormatException e) {
                        //e.printStackTrace();
                    }
                    try {
                        LayoutElementParcelable layoutElement = FileUtils.newElement(Icons.loadMimeIcon(
                                baseFile.getPath(), !ma.IS_LIST, ma.getResources()), baseFile.getPath(), baseFile.getPermission(),
                                baseFile.getLink(), size, longSize, false, false, baseFile.getDate() + "");
                        layoutElement.setMode(baseFile.getMode());
                        items.add(layoutElement);
                        ma.file_count++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return items;
    }

    private ArrayList<HybridFileParcelable> listaudio() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media.DATA
        };

        Cursor cursor = c.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        ArrayList<HybridFileParcelable> songs = new ArrayList<>();
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    private ArrayList<HybridFileParcelable> listImages() {
        ArrayList<HybridFileParcelable> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = c.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    private ArrayList<HybridFileParcelable> listVideos() {
        ArrayList<HybridFileParcelable> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = c.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    private ArrayList<HybridFileParcelable> listRecentFiles() {
        ArrayList<HybridFileParcelable> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED};
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
        Date d = c.getTime();
        Cursor cursor = this.c.getContentResolver().query(MediaStore.Files
                        .getContentUri("external"), projection,
                null,
                null, null);
        if (cursor == null) return songs;
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                File f = new File(path);
                if (d.compareTo(new Date(f.lastModified())) != 1 && !f.isDirectory()) {
                    HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                    if (strings != null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        Collections.sort(songs, new Comparator<HybridFileParcelable>() {
            @Override
            public int compare(HybridFileParcelable lhs, HybridFileParcelable rhs) {
                return -1 * Long.valueOf(lhs.getDate()).compareTo(rhs.getDate());

            }
        });
        if (songs.size() > 20)
            for (int i = songs.size() - 1; i > 20; i--) {
                songs.remove(i);
            }
        return songs;
    }

    private ArrayList<HybridFileParcelable> listApks() {
        ArrayList<HybridFileParcelable> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA};

        Cursor cursor = c.getContentResolver()
                .query(MediaStore.Files.getContentUri("external"), projection, null, null, null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if (path != null && path.endsWith(".apk")) {
                    HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                    if (strings != null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    private ArrayList<HybridFileParcelable> listRecent() {
        UtilsHandler utilsHandler = new UtilsHandler(c);
        final ArrayList<String> paths = utilsHandler.getHistoryList();
        ArrayList<HybridFileParcelable> songs = new ArrayList<>();
        for (String f : paths) {
            if (!f.equals("/")) {
                HybridFileParcelable a = RootHelper.generateBaseFile(new File(f), ma.SHOW_HIDDEN);
                a.generateMode(ma.getActivity());
                if (a != null && !a.isSmb() && !(a).isDirectory() && a.exists())
                    songs.add(a);
            }
        }
        return songs;
    }

    private ArrayList<HybridFileParcelable> listDocs() {
        ArrayList<HybridFileParcelable> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = c.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                projection, null, null, null);
        String[] types = new String[]{".pdf", ".xml", ".html", ".asm", ".text/x-asm", ".def", ".in", ".rc",
                ".list", ".log", ".pl", ".prop", ".properties", ".rc",
                ".doc", ".docx", ".msg", ".odt", ".pages", ".rtf", ".txt", ".wpd", ".wps"};
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if (path != null && contains(types, path)) {
                    HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                    if (strings != null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    /**
     * Lists files from an OTG device
     *
     * @param path the path to the directory tree, starts with prefix {@link com.amaze.filemanager.utils.OTGUtil#PREFIX_OTG}
     *             Independent of URI (or mount point) for the OTG
     * @return a list of files loaded
     */
    private ArrayList<HybridFileParcelable> listOtg(String path) {

        return OTGUtil.getDocumentFilesList(path, c);
    }

    private boolean contains(String[] types, String path) {
        for (String string : types) {
            if (path.endsWith(string)) return true;
        }
        return false;
    }

    private ArrayList<HybridFileParcelable> listCloud(String path, CloudStorage cloudStorage, OpenMode openMode)
            throws CloudPluginException {
        if (!CloudSheetFragment.isCloudProviderAvailable(c))
            throw new CloudPluginException();

        return CloudUtil.listFiles(path, cloudStorage, openMode);
    }
}
