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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.utils.HFile;
import com.amaze.filemanager.utils.RootHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;


public class LoadList extends AsyncTask<String, String, ArrayList<Layoutelements>> {

    private String path;
    boolean back;
    Main ma;
    boolean customPath=false;
    int openmode=0;//0 for normal 1 for smb 2 for custom
    public LoadList(boolean back, Main ma) {
        this.back = back;
        this.ma = ma;
    }
    public LoadList(boolean back, Main ma,boolean customPath) {
        this.back = back;
        this.ma = ma;
        this.customPath=customPath;
    }
    @Override
    protected void onPreExecute() {
        if(customPath)
        ma.mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(ma.getActivity(), message[0], Toast.LENGTH_LONG).show();
    }

    @Override
    // Actual download method, run in the task thread
    protected ArrayList<Layoutelements> doInBackground(String... params) {
        // params comes from the execute() call: params[0] is the url.
        ArrayList<Layoutelements> list=null;
        path=params[0];
        ma.folder_count=0;
        ma.file_count=0;
        if(customPath){
            HFile hFile=new HFile(path);
            if(hFile.isSmb()){
                openmode=1;
                try {
                    SmbFile[] smbFile = hFile.getSmbFile().listFiles();
                    list=ma.addToSmb(smbFile);
                } catch (SmbException e) {
                    publishProgress(e.getLocalizedMessage());
                    e.printStackTrace();
                }catch (NullPointerException e) {
                    publishProgress(e.getLocalizedMessage());
                    e.printStackTrace();
                }
            } else if(hFile.isCustomPath()){
                ArrayList<String[]> arrayList=null;
            switch (Integer.parseInt(path)){
                case 0:
                    arrayList=(listImages());
                    path="0";
                    break;
                case 1:
                    arrayList=(listVideos());
                    path="1";
                    break;
                case 2:
                    arrayList=(listaudio());
                    path="2";
                    break;
                case 3:
                    arrayList=(listDocs());
                    path="3";
                    break;
                case 4:
                    arrayList=(listApks());
                    path="4";
                    break;
            }
            if(arrayList!=null)
            {
            list=ma.addTo(arrayList);
            openmode=2;
            }
            else return null;
            }
        } else {
            try {
                ArrayList<String[]> arrayList;
                if (ma.rootMode) {
                    arrayList = RootHelper.getFilesList(path, ma.rootMode, ma.showHidden, ma
                            .showSize);
                } else
                    arrayList = (RootHelper.getFilesList(ma.showSize, path, ma.showHidden));
                list = ma.addTo(arrayList);

            } catch (Exception e) {
                return null;
            }
        }
        if(list!=null)
        Collections.sort(list, new FileListSorter(ma.dsort, ma.sortby, ma.asc, ma.rootMode));
        return list;


    }

    @Override
    // Once the image is downloaded, associates it to the imageView
    protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
        if (isCancelled()) {
            bitmap = null;

        }    ma.createViews(bitmap, back, path, openmode, false);

    }
    ArrayList<String[]> listaudio(){
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media.DATA
        };

        Cursor cursor =ma.getActivity().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

         ArrayList<String[]> songs = new ArrayList<String[]>();
        if (cursor.getCount()>0 && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                String[] strings = RootHelper.addFile(new File(path), ma.showSize, ma.showHidden);
                if (strings != null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }
    ArrayList<String[]> listImages(){
        ArrayList<String[]> songs = new ArrayList<String[]>();
        final String[] projection = { MediaStore.Images.Media.DATA };
        final Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor.getCount()>0 && cursor.moveToFirst()) {
            do {
                String path=cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                String[] strings=RootHelper.addFile(new File(path), ma.showSize, ma.showHidden);
                if(strings!=null) songs.add(strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }
    ArrayList<String[]> listVideos(){
        ArrayList<String[]> songs = new ArrayList<String[]>();
        final String[] projection = { MediaStore.Images.Media.DATA };
        final Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Video.Media
                        .EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);
        if (cursor.getCount()>0 && cursor.moveToFirst()) {
            do {
                String path=cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                 String[] strings=RootHelper.addFile(new File(path), ma.showSize, ma.showHidden);
                    if(strings!=null) songs.add(strings);
                } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }
    ArrayList<String[]> listApks(){
        ArrayList<String[]> songs = new ArrayList<String[]>();
        Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Files
                        .getContentUri("external"), null,
                null,
                null, null);
        if (cursor.getCount()>0 && cursor.moveToFirst()) {
            do {
                String path=cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if(path!=null && path.endsWith(".apk"))
                { String[] strings=RootHelper.addFile(new File(path), ma.showSize, ma.showHidden);
                  if(strings!=null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }
    ArrayList<String[]> listDocs(){
        ArrayList<String[]> songs = new ArrayList<String[]>();
        Cursor cursor = ma.getActivity().getContentResolver().query(MediaStore.Files
                        .getContentUri("external"), null,
                null,
                null, null);
        String[] types=new String[]{".pdf",".xml",".html",".asm", ".text/x-asm",".def",".in",".rc",
                ".list",".log",".pl",".prop",".properties",".rc",
                ".doc",".docx",".msg",".odt",".pages",".rtf",".txt",".wpd",".wps"};
        if (cursor.getCount()>0 && cursor.moveToFirst()) {
            do {
                String path=cursor.getString(cursor.getColumnIndex
                        (MediaStore.Files.FileColumns.DATA));
                if(path!=null && contains(types, path))
                {
                    String[] strings=RootHelper.addFile(new File(path), ma.showSize, ma.showHidden);
                if(strings!=null) songs.add(strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }
boolean contains(String[] types,String path){
    for(String string:types){
        if(path.endsWith(string))return true;
    }return false;
}
}
