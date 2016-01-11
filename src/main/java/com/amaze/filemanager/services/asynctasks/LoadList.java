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

package com.amaze.filemanager.services.asynctasks;

import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.Toast;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.utils.BaseFile;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.HFile;
import com.amaze.filemanager.utils.RootHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


public class LoadList extends AsyncTask<String, String, ArrayList<Layoutelements>> {

    private String path;
    boolean back;
    Main ma;
    int openmode = 0;//0 for normal 1 for smb 2 for custom 3 for drive

    public LoadList(boolean back, Main ma, int openmode) {
        this.back = back;
        this.ma = ma;
        this.openmode = openmode;
    }

    @Override
    protected void onPreExecute() {
        if (openmode != 0)
            ma.mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(ma.getActivity(), message[0], Toast.LENGTH_LONG).show();
    }

    boolean grid;

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<Layoutelements> doInBackground(String... params) {
        // params comes from the execute() call: params[0] is the url.
        ArrayList<Layoutelements> list = null;
        path = params[0];
        grid = ma.checkforpath(path);
        ma.folder_count = 0;
        ma.file_count = 0;
        if (openmode == -1) {
            HFile hFile = new HFile(path);
            if (hFile.isDirectory() && !hFile.isSmb()) {
                openmode = (0);
            } else if (hFile.isSmb()){
                openmode = (1);
                ma.smbPath=path;
            }
            else if (hFile.isCustomPath())
                openmode = (2);
            else if (android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches()) {
                openmode = (3);
            }
        }
        if (openmode == 1) {
            HFile hFile = new HFile(path);
            try {
                SmbFile[] smbFile = hFile.getSmbFile().listFiles();
                list = ma.addToSmb(smbFile,path);
            } catch (SmbException e) {
                publishProgress(e.getLocalizedMessage());
                e.printStackTrace();
            } catch (NullPointerException e) {
                publishProgress(e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else if (openmode == 2) {

            ArrayList<BaseFile> arrayList = null;
            switch (Integer.parseInt(path)) {
                case 0:
                    arrayList = (listImages());
                    path = "0";
                    break;
                case 1:
                    arrayList = (listVideos());
                    path = "1";
                    break;
                case 2:
                    arrayList = (listaudio());
                    path = "2";
                    break;
                case 3:
                    arrayList = (listDocs());
                    path = "3";
                    break;
                case 4:
                    arrayList = (listApks());
                    path = "4";
                    break;
                case 5:
                    arrayList = listRecent();
                    path = "5";
                    break;
                case 6:
                    arrayList = listRecentFiles();
                    path = "6";

            }
            if (arrayList != null)
                list = ma.addTo(arrayList);
            else return new ArrayList<>();
        } else if (openmode == 3) {


        } else {
            try {
                ArrayList<BaseFile> arrayList;
                if (ma.ROOT_MODE) {
                    arrayList = RootHelper.getFilesList(path, ma.ROOT_MODE, ma.SHOW_HIDDEN);
                } else
                    arrayList = (RootHelper.getFilesList(path, ma.SHOW_HIDDEN));
                list = ma.addTo(arrayList);

            } catch (Exception e) {
                return null;
            }
        }
        if (list != null && !(openmode == 2 && ((path).equals("5") || (path).equals("6"))))
            Collections.sort(list, new FileListSorter(ma.dsort, ma.sortby, ma.asc, ma.ROOT_MODE));
        return list;


    }

    @Override
    // Once the image is downloaded, associates it to the imageView
    protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
        if (isCancelled()) {
            bitmap = null;

        }
        ma.createViews(bitmap, back, path, openmode, false, grid);

    }

    ArrayList<BaseFile> listaudio() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media.DATA
        };

        Cursor cursor = ma.getActivity().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        ArrayList<BaseFile> songs = new ArrayList<>();
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                BaseFile strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    ArrayList<BaseFile> listImages() {
        ArrayList<BaseFile> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                BaseFile strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    ArrayList<BaseFile> listVideos() {
        ArrayList<BaseFile> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Images.Media.DATA};
        final Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Video.Media
                        .EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                BaseFile strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    ArrayList<BaseFile> listRecentFiles() {
        ArrayList<BaseFile> songs = new ArrayList<BaseFile>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED};
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
        Date d = c.getTime();
        Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Files
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
                    BaseFile strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                    if (strings != null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        Collections.sort(songs, new Comparator<BaseFile>() {
            @Override
            public int compare(BaseFile lhs, BaseFile rhs) {
                return -1 * Long.valueOf(lhs.getDate()).compareTo(Long.valueOf(rhs.getDate()));

            }
        });
        if (songs.size() > 20)
            for (int i = songs.size() - 1; i > 20; i--) {
                songs.remove(i);
            }
        return songs;
    }

    ArrayList<BaseFile> listApks() {
        ArrayList<BaseFile> songs = new ArrayList<BaseFile>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA};

        Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Files
                        .getContentUri("external"), projection,
                null,
                null, null);
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if (path != null && path.endsWith(".apk")) {
                    BaseFile strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                    if (strings != null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    ArrayList<BaseFile> listRecent() {
        final ArrayList<String> paths = ma.MAIN_ACTIVITY.history.readTable(ma.MAIN_ACTIVITY.HISTORY);
        ArrayList<BaseFile> songs = new ArrayList<>();
        for (String f : paths) {
            if (!f.equals("/")) {
                BaseFile a = RootHelper.generateBaseFile(new File(f), ma.SHOW_HIDDEN);
                if (a != null && !(a).isDirectory())
                    songs.add(a);
            }
        }
        return songs;
    }

    ArrayList<BaseFile> listDocs() {
        ArrayList<BaseFile> songs = new ArrayList<>();
        final String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Files
                        .getContentUri("external"), projection,
                null,
                null, null);
        String[] types = new String[]{".pdf", ".xml", ".html", ".asm", ".text/x-asm", ".def", ".in", ".rc",
                ".list", ".log", ".pl", ".prop", ".properties", ".rc",
                ".doc", ".docx", ".msg", ".odt", ".pages", ".rtf", ".txt", ".wpd", ".wps"};
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if (path != null && contains(types, path)) {
                    BaseFile strings = RootHelper.generateBaseFile(new File(path), ma.SHOW_HIDDEN);
                    if (strings != null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    boolean contains(String[] types, String path) {
        for (String string : types) {
            if (path.endsWith(string)) return true;
        }
        return false;
    }
}
