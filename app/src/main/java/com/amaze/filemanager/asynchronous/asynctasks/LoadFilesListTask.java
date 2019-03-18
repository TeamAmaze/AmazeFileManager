/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *     Emmanuel Messulam <emmanuelbendavid@gmail.com>,
 *     Marcin Zasuwa <marcinadd@gmail.com>
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
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.util.Pair;
import android.text.format.Formatter;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.FileListSorter;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class LoadFilesListTask extends AsyncTask<Void, Void, Pair<OpenMode, ArrayList<LayoutElementParcelable>>> {

    private String path;
    private MainFragment ma;
    private Context c;
    private OpenMode openmode;
    private boolean showHiddenFiles, showThumbs;
    private DataUtils dataUtils = DataUtils.getInstance();
    private OnAsyncTaskFinished<Pair<OpenMode, ArrayList<LayoutElementParcelable>>> listener;

    public LoadFilesListTask(Context c, String path, MainFragment ma, OpenMode openmode,
                             boolean showThumbs, boolean showHiddenFiles,
                             OnAsyncTaskFinished<Pair<OpenMode, ArrayList<LayoutElementParcelable>>> l) {
        this.path = path;
        this.ma = ma;
        this.openmode = openmode;
        this.c = c;
        this.showThumbs = showThumbs;
        this.showHiddenFiles = showHiddenFiles;
        this.listener = l;
    }

    @Override
    protected Pair<OpenMode, ArrayList<LayoutElementParcelable>> doInBackground(Void... p) {
        HybridFile hFile = null;

        if (openmode == OpenMode.UNKNOWN) {
            hFile = new HybridFile(OpenMode.UNKNOWN, path);
            hFile.generateMode(ma.getActivity());
            openmode = hFile.getMode();

            if (hFile.isSmb()) {
                ma.smbPath = path;
            }
        }

        if(isCancelled()) return null;

        ma.folder_count = 0;
        ma.file_count = 0;
        final ArrayList<LayoutElementParcelable> list;

        switch (openmode) {
            case SMB:
                if (hFile == null) {
                    hFile = new HybridFile(OpenMode.SMB, path);
                }

                try {
                    SmbFile[] smbFile = hFile.getSmbFile(5000).listFiles();
                    list = ma.addToSmb(smbFile, path, showHiddenFiles);
                    openmode = OpenMode.SMB;
                } catch (SmbAuthException e) {
                    if (!e.getMessage().toLowerCase().contains("denied")) {
                        ma.reauthenticateSmb();
                    }
                    return null;
                } catch (SmbException | NullPointerException e) {
                    e.printStackTrace();
                    return null;
                }
                break;
            case SFTP:
                HybridFile sftpHFile = new HybridFile(OpenMode.SFTP, path);

                list = new ArrayList<LayoutElementParcelable>();

                sftpHFile.forEachChildrenFile(c, false, file -> {
                    if (!(dataUtils.isFileHidden(file.getPath()) || file.isHidden() && !showHiddenFiles)) {
                        LayoutElementParcelable elem = createListParcelables(file);
                        if (elem != null) {
                            list.add(elem);
                        }
                    }
                });
                break;
            case CUSTOM:
                switch (Integer.parseInt(path)) {
                    case 0:
                        list = listImages();
                        break;
                    case 1:
                        list = listVideos();
                        break;
                    case 2:
                        list = listaudio();
                        break;
                    case 3:
                        list = listDocs();
                        break;
                    case 4:
                        list = listApks();
                        break;
                    case 5:
                        list = listRecent();
                        break;
                    case 6:
                        list = listRecentFiles();
                        break;
                    default:
                        throw new IllegalStateException();
                }

                break;
            case OTG:
                list = new ArrayList<>();
                listOtg(path, file -> {
                    LayoutElementParcelable elem = createListParcelables(file);
                    if(elem != null) list.add(elem);
                });
                openmode = OpenMode.OTG;
                break;
            case DROPBOX:
            case BOX:
            case GDRIVE:
            case ONEDRIVE:
                CloudStorage cloudStorage = dataUtils.getAccount(openmode);
                list = new ArrayList<>();

                try {
                    listCloud(path, cloudStorage, openmode, file -> {
                        LayoutElementParcelable elem = createListParcelables(file);
                        if(elem != null) list.add(elem);
                    });
                } catch (CloudPluginException e) {
                    e.printStackTrace();
                    AppConfig.toast(c, c.getResources().getString(R.string.failed_no_connection));
                    return new Pair<>(openmode, list);
                }
                break;
            default:
                // we're neither in OTG not in SMB, load the list based on root/general filesystem
                list = new ArrayList<>();
                RootHelper.getFiles(path, ma.getMainActivity().isRootExplorer(), showHiddenFiles,
                        mode -> openmode = mode, file -> {
                            LayoutElementParcelable elem = createListParcelables(file);
                            if(elem != null) list.add(elem);
                        });
                break;
        }

        if (list != null && !(openmode == OpenMode.CUSTOM && ((path).equals("5") || (path).equals("6")))) {
            int t = SortHandler.getSortType(ma.getContext(), path);
            int sortby;
            int asc;
            if (t <= 3) {
                sortby = t;
                asc = 1;
            } else {
                asc = -1;
                sortby = t - 4;
            }
            Collections.sort(list, new FileListSorter(ma.dsort, sortby, asc));
        }

        return new Pair<>(openmode, list);
    }

    @Override
    protected void onPostExecute(Pair<OpenMode, ArrayList<LayoutElementParcelable>> list) {
        super.onPostExecute(list);
        listener.onAsyncTaskFinished(list);
    }

    private LayoutElementParcelable createListParcelables(HybridFileParcelable baseFile) {
        if (!dataUtils.isFileHidden(baseFile.getPath())) {
            String size = "";
            long longSize= 0;

            if (baseFile.isDirectory()) {
                ma.folder_count++;
            } else {
                if (baseFile.getSize() != -1) {
                    try {
                        longSize = baseFile.getSize();
                        size = Formatter.formatFileSize(c, longSize);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                
                ma.file_count++;
            }

            LayoutElementParcelable layoutElement = new LayoutElementParcelable(baseFile.getName(),
                    baseFile.getPath(), baseFile.getPermission(), baseFile.getLink(), size,
                    longSize, false, baseFile.getDate() + "", baseFile.isDirectory(),
                    showThumbs, baseFile.getMode());
            return layoutElement;
        }

        return null;
    }

    private ArrayList<LayoutElementParcelable> listImages() {
        ArrayList<LayoutElementParcelable> images = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = c.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        if (cursor == null)
            return images;
        else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), showHiddenFiles);
                if (strings != null) {
                    LayoutElementParcelable parcelable = createListParcelables(strings);
                    if(parcelable != null) images.add(parcelable);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return images;
    }

    private ArrayList<LayoutElementParcelable> listVideos() {
        ArrayList<LayoutElementParcelable> videos = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = c.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        if (cursor == null)
            return videos;
        else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), showHiddenFiles);
                if (strings != null) {
                    LayoutElementParcelable parcelable = createListParcelables(strings);
                    if(parcelable != null) videos.add(parcelable);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return videos;
    }

    private ArrayList<LayoutElementParcelable> listaudio() {
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

        ArrayList<LayoutElementParcelable> songs = new ArrayList<>();
        if (cursor == null)
            return songs;
        else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), showHiddenFiles);
                if (strings != null) {
                    LayoutElementParcelable parcelable = createListParcelables(strings);
                    if(parcelable != null) songs.add(parcelable);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    private ArrayList<LayoutElementParcelable> listDocs() {
        ArrayList<LayoutElementParcelable> docs = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = c.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                projection, null, null, null);
        String[] types = new String[]{".pdf", ".xml", ".html", ".asm", ".text/x-asm", ".def", ".in", ".rc",
                ".list", ".log", ".pl", ".prop", ".properties", ".rc",
                ".doc", ".docx", ".msg", ".odt", ".pages", ".rtf", ".txt", ".wpd", ".wps"};
        if (cursor == null)
            return docs;
        else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if (path != null && Arrays.asList(types).contains(path)) {
                    HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), showHiddenFiles);
                    if (strings != null) {
                        LayoutElementParcelable parcelable = createListParcelables(strings);
                        if(parcelable != null) docs.add(parcelable);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        Collections.sort(docs, (lhs, rhs) -> -1 * Long.valueOf(lhs.date).compareTo(rhs.date));
        if (docs.size() > 20)
            for (int i = docs.size() - 1; i > 20; i--) {
                docs.remove(i);
            }
        return docs;
    }

    private ArrayList<LayoutElementParcelable> listApks() {
        ArrayList<LayoutElementParcelable> apks = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA};

        Cursor cursor = c.getContentResolver()
                .query(MediaStore.Files.getContentUri("external"), projection, null, null, null);
        if (cursor == null)
            return apks;
        else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if (path != null && path.endsWith(".apk")) {
                    HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), showHiddenFiles);
                    if (strings != null) {
                        LayoutElementParcelable parcelable = createListParcelables(strings);
                        if(parcelable != null) apks.add(parcelable);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return apks;
    }

    private ArrayList<LayoutElementParcelable> listRecent() {
        UtilsHandler utilsHandler = new UtilsHandler(c);
        final LinkedList<String> paths = utilsHandler.getHistoryLinkedList();
        ArrayList<LayoutElementParcelable> songs = new ArrayList<>();
        for (String f : paths) {
            if (!f.equals("/")) {
                HybridFileParcelable hybridFileParcelable = RootHelper.generateBaseFile(new File(f), showHiddenFiles);
                if (hybridFileParcelable != null) {
                    hybridFileParcelable.generateMode(ma.getActivity());
                    if (!hybridFileParcelable.isSmb() && !hybridFileParcelable.isDirectory() && hybridFileParcelable.exists()) {
                        LayoutElementParcelable parcelable = createListParcelables(hybridFileParcelable);
                        if (parcelable != null) songs.add(parcelable);
                    }
                }
            }
        }
        return songs;
    }

    private ArrayList<LayoutElementParcelable> listRecentFiles() {
        ArrayList<LayoutElementParcelable> recentFiles = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED};
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
        Date d = c.getTime();
        Cursor cursor = this.c.getContentResolver().query(MediaStore.Files
                        .getContentUri("external"), projection,
                null,
                null, null);
        if (cursor == null) return recentFiles;
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                File f = new File(path);
                if (d.compareTo(new Date(f.lastModified())) != 1 && !f.isDirectory()) {
                    HybridFileParcelable strings = RootHelper.generateBaseFile(new File(path), showHiddenFiles);
                    if (strings != null) {
                        LayoutElementParcelable parcelable = createListParcelables(strings);
                        if(parcelable != null) recentFiles.add(parcelable);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        Collections.sort(recentFiles, (lhs, rhs) -> -1 * Long.valueOf(lhs.date).compareTo(rhs.date));
        if (recentFiles.size() > 20)
            for (int i = recentFiles.size() - 1; i > 20; i--) {
                recentFiles.remove(i);
            }
        return recentFiles;
    }

    /**
     * Lists files from an OTG device
     *
     * @param path the path to the directory tree, starts with prefix {@link com.amaze.filemanager.utils.OTGUtil#PREFIX_OTG}
     *             Independent of URI (or mount point) for the OTG
     */
    private void listOtg(String path, OnFileFound fileFound) {
        OTGUtil.getDocumentFiles(path, c, fileFound);
    }

    private void listCloud(String path, CloudStorage cloudStorage, OpenMode openMode,
                           OnFileFound fileFoundCallback) throws CloudPluginException {
        if (!CloudSheetFragment.isCloudProviderAvailable(c)) {
            throw new CloudPluginException();
        }

        CloudUtil.getCloudFiles(path, cloudStorage, openMode, fileFoundCallback);
    }
}
