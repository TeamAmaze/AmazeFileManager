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

package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vishal on 9/17/2014.
 */
public class TabHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "explorer.db";
    private static final String TABLE_TAB = "tab";

    public static final String COLUMN_TAB_NO = "tab_no";
    public static final String COLUMN_LABEL = "label";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_HOME = "home";
    public TabHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TAB_TABLE = "CREATE TABLE " + TABLE_TAB + "("
                +  COLUMN_TAB_NO
                + " INTEGER PRIMARY KEY,"
                + COLUMN_PATH + " TEXT,"+COLUMN_HOME+" TEXT" + ")";
        sqLiteDatabase.execSQL(CREATE_TAB_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TAB);
        onCreate(sqLiteDatabase);
    }

    public void addTab(Tab tab) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TAB_NO, tab.getTab());
        contentValues.put(COLUMN_PATH, tab.getPath());
        contentValues.put(COLUMN_HOME, tab.getHome());
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.insert(TABLE_TAB, null, contentValues);
        sqLiteDatabase.close();
    }
 public void clear(){
     try {
         SQLiteDatabase sqLiteDatabase = getWritableDatabase();
         sqLiteDatabase.delete(TABLE_TAB,COLUMN_TAB_NO+" = ?",new String[]{""+1});
         sqLiteDatabase.delete(TABLE_TAB, COLUMN_TAB_NO + " = ?", new String[]{"" + 2});
     sqLiteDatabase.close();
     } catch (NumberFormatException e) {
         e.printStackTrace();
     }


 }
    public Tab findTab(int tabNo) {
        String query = "Select * FROM " + TABLE_TAB + " WHERE " + COLUMN_TAB_NO + "= \"" + tabNo + "\"";
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        Tab tab = new Tab();
        if (cursor.moveToFirst()) {
            cursor.moveToFirst();
            tab.setTab(Integer.parseInt(cursor.getString(0)));
            tab.setPath(cursor.getString(1));
            tab.setHome(cursor.getString(2));
            cursor.close();
        } else {
            tab = null;
        }
        sqLiteDatabase.close();
        return tab;
    }

    public List<Tab> getAllTabs() {
        List<Tab> tabList = new ArrayList<Tab>();
        // Select all query
        String query = "Select * FROM " + TABLE_TAB;

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        // Looping through all rows and adding them to list
        if (cursor.moveToFirst()) {
            do {
                Tab tab = new Tab();
                tab.setTab((cursor.getInt(cursor.getColumnIndex(COLUMN_TAB_NO))));
                tab.setPath(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));
                tab.setHome(cursor.getString(cursor.getColumnIndex(COLUMN_HOME)));
                //Adding them to list
                tabList.add(tab);
            } while (cursor.moveToNext());
        }sqLiteDatabase.close();
        return tabList;
    }
    public void close(){
        try {
            getWritableDatabase().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
