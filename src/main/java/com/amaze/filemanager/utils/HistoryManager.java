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

import java.io.File;
import java.util.ArrayList;

public class HistoryManager {
    SQLiteDatabase db;
    Context c;
    String dbname;
    String[] a;
    public HistoryManager(Context c ,String dbname) {
        this.c = c;
        this.dbname=dbname;
        open();
        String sd = Environment.getExternalStorageDirectory() + "/";
        a = new String[]{sd + Environment.DIRECTORY_DCIM, sd + Environment.DIRECTORY_DOWNLOADS, sd + Environment.DIRECTORY_MOVIES, sd + Environment.DIRECTORY_MUSIC, sd + Environment.DIRECTORY_PICTURES};
    }
    public void make(String table){
        for(String d:a){
            addPath(new File(d).getName(),d,table,1);
        }
    }
    public void initializeTable(String table,int mode){
        if(mode==0)
            db.execSQL("CREATE TABLE IF NOT EXISTS " + table + " (PATH VARCHAR)");
        else
            db.execSQL("CREATE TABLE IF NOT EXISTS " + table + " (NAME VARCHAR,PATH VARCHAR)");

    }
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
    public boolean rename(final String oldname, final String oldpath, final String path, final String name, final String table){
        new Thread(new Runnable() {
            @Override
            public void run() {

                ArrayList<String[]> arrayList=readTableSecondary(table);

                for(int i=0;i<arrayList.size();i++){
                    if(arrayList.get(i)[1].equals(oldpath) && arrayList.get(i)[0].equals(oldname)){
                        try {
                            removePath(oldname,oldpath,table );
                            addPath(name,path,table,1);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }).start();
        return false;
    }
    public ArrayList<String> readTable(String table) {
        Cursor c = db.rawQuery("SELECT * FROM " + table, null);
        c.moveToLast();
        ArrayList<String> paths = new ArrayList<String>();
        do {
            try {
                paths.add(c.getString(c.getColumnIndex("PATH")));
            } catch (Exception e) {
            }
        } while (c.moveToPrevious());
        return paths;
    }
    public ArrayList<String[]> readTableSecondary(String table) {
        Cursor c = db.rawQuery("SELECT * FROM " + table, null);
        c.moveToLast();
        ArrayList<String[]> paths = new ArrayList<String[]>();
        do {
            try {
                paths.add(new String[]{c.getString(c.getColumnIndex("NAME")),c.getString(c.getColumnIndex("PATH"))});
            } catch (Exception e) {
            }
        } while (c.moveToPrevious());
        return paths;
    }
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
    public void removePath(String path,String table){
        try {
            db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "'");
        } catch (Exception e) {
        }
    }
    public void removePath(String name,String path,String table){
        try {
            db.execSQL("DELETE FROM " + table + " WHERE PATH='" + path + "' and NAME='"+name+"'");
        } catch (Exception e) {
        }
    }
    public void end() {
        db.close();
    }

    public void open() {
        db = c.openOrCreateDatabase(dbname, c.MODE_PRIVATE, null);
    }
}
