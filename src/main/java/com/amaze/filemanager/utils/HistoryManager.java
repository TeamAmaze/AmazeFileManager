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

package com.amaze.filemanager.utils;


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.amaze.filemanager.ui.dialogs.SmbConnectDialog;

import java.io.File;
import java.util.ArrayList;

public class HistoryManager {
    private SQLiteDatabase db;
    private Context c;
    private String dbname;
    String[] dirs;
    public HistoryManager(Context c ,String dbname) {
        this.c = c;
        this.dbname=dbname;
        open();
        String sd = Environment.getExternalStorageDirectory() + "/";
        dirs = new String[] {
                sd + Environment.DIRECTORY_DCIM,
                sd + Environment.DIRECTORY_DOWNLOADS,
                sd + Environment.DIRECTORY_MOVIES,
                sd + Environment.DIRECTORY_MUSIC,
                sd + Environment.DIRECTORY_PICTURES
        };
    }
    public void make(String table){
        for(String d: dirs){
            addPath(new File(d).getName(),d,table,1);
        }
    }
    public void initializeTable(String table,int mode){
        if(mode==0)
            db.execSQL("CREATE TABLE IF NOT EXISTS " + table + " (PATH VARCHAR)");
        else
            db.execSQL("CREATE TABLE IF NOT EXISTS " + table + " (NAME VARCHAR,PATH VARCHAR)");

    }
    //single column
    public boolean rename(String path,String name,String table){
    ArrayList<String[]> arrayList=readTableSecondary(table);

        for(int i=0;i<arrayList.size();i++){
            if(arrayList.get(i)[1].equals(path)){
                try {
                    db.execSQL("update "+table+" set name='"+name+"' where path='"+path+"';");
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public ArrayList<String> readTable(String table) {
        Cursor c = db.rawQuery("SELECT * FROM " + table, null);
        c.moveToLast();
        ArrayList<String> paths = new ArrayList<>();
        do {
            try {
                paths.add(c.getString(c.getColumnIndex("PATH")));
            } catch (Exception e) {
            }
        } while (c.moveToPrevious());
        return paths;
    }

    public void removePath(String path,String table){
        try {
            db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
        } catch (Exception e) {
        }
    }
    //common
    public void clear(String table){
        db.execSQL("DELETE FROM "+table+" WHERE PATH is NOT NULL");
    }

    public void addPath(String name,String path,String table,int mode) {

        try {
            try {
                db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
            } catch (Exception e) {
            }
            if(mode==0)
                db.execSQL("INSERT INTO " + table + " VALUES" + "('" + path + "');");
            else
                db.execSQL("INSERT INTO " + table + " VALUES" + "('"+name+"','" + path + "');");
        } catch (Exception e) {
                open();
            try {
                db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
                if(mode==0)
                    db.execSQL("INSERT INTO " + table + " VALUES" + "('" + path + "');");
                else
                    db.execSQL("INSERT INTO " + table + " VALUES" + "('"+name+"','" + path + "');");
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
    public void end() {
        db.close();
    }

    public void open() {
        db = c.openOrCreateDatabase(dbname, c.MODE_PRIVATE, null);
    }

    //double columns
    public void removePath(String name,String path,String table){
        try {
            if (table.equals(DataUtils.SMB)) {

                // we need to encrypt the path back in order to get a valid match from database entry
                db.execSQL("DELETE FROM " + table + " WHERE PATH='" +
                        SmbConnectDialog.getSmbEncryptedPath(this.c, path) + "' and NAME='"+name+"'");
            } else {

                db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "' and NAME='"+name+"'");
            }
        } catch (SQLException e) {
        e.printStackTrace();
        }
    }
    public boolean rename(final String oldname, final String oldpath, final String path, final String name, final String table){
        try {
            removePath(oldname,oldpath,table);
            db.execSQL("INSERT INTO " + table + " VALUES" + "('"+name+"','" + path + "');");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public ArrayList<String[]> readTableSecondary(String table) {
        Cursor c = db.rawQuery("SELECT * FROM " + table, null);
        c.moveToLast();
        ArrayList<String[]> paths = new ArrayList<>();
        do {
            try {
                // decrypt path from smb table first!
                paths.add(new String[] {
                        c.getString(c.getColumnIndex("NAME")),
                        table.equals(DataUtils.SMB) ?
                                SmbConnectDialog.getSmbDecryptedPath(this.c, c.getString(c.getColumnIndex("PATH"))) :
                                c.getString(c.getColumnIndex("PATH"))
                });
            } catch (Exception e) {
            }
        } while (c.moveToPrevious());

        return paths;
    }
}
