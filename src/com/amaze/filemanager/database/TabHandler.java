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
import android.database.SQLException;
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

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TAB_NO = "tab_no";
    public static final String COLUMN_LABEL = "label";
    public static final String COLUMN_PATH = "path";

    public TabHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TAB_TABLE = "CREATE TABLE " + TABLE_TAB + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_TAB_NO
                + " INTEGER," + COLUMN_LABEL + " TEXT,"
                + COLUMN_PATH + " TEXT" + ")";
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
        contentValues.put(COLUMN_LABEL, tab.getLabel());
        contentValues.put(COLUMN_PATH, tab.getPath());

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.insert(TABLE_TAB, null, contentValues);
        sqLiteDatabase.close();
    }
 public void clear(){
     try {
         boolean result = false;
         String query = "Select * FROM " + TABLE_TAB ;
         SQLiteDatabase sqLiteDatabase = getWritableDatabase();
         Cursor cursor = sqLiteDatabase.rawQuery(query, null);
         Tab tab = new Tab();

         while (cursor.moveToNext()) {
             tab.setID(Integer.parseInt(cursor.getString(0)));
             sqLiteDatabase.delete(TABLE_TAB, COLUMN_ID + " = ?",
                     new String[]{String.valueOf(tab.getID())});
             result = true;
         }
         cursor.close();
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
            tab.setID(Integer.parseInt(cursor.getString(0)));
            tab.setTab(Integer.parseInt(cursor.getString(1)));
            tab.setLabel(cursor.getString(2));
            tab.setPath(cursor.getString(3));
            cursor.close();
        } else {
            tab = null;
        }
        sqLiteDatabase.close();

        return tab;
    }

    public void updateTab(Tab tab) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_LABEL, tab.getLabel());
        contentValues.put(COLUMN_PATH, tab.getPath());

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.update(TABLE_TAB, contentValues, tab.getTab() + "=" + COLUMN_TAB_NO, null);
        sqLiteDatabase.close();
    }

    public boolean deleteTab(int tabNo) {
        boolean result = false;
        String query = "Select * FROM " + TABLE_TAB + " WHERE " + COLUMN_TAB_NO + " = \"" + tabNo + "\"";
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        Tab tab = new Tab();

        if (cursor.moveToFirst()) {
            tab.setID(Integer.parseInt(cursor.getString(0)));
            sqLiteDatabase.delete(TABLE_TAB, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(tab.getID())});
            cursor.close();
            result = true;
        }
        sqLiteDatabase.close();

        return result;
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
                tab.setID(Integer.parseInt(cursor.getString(0)));
                tab.setTab(Integer.parseInt(cursor.getString(1)));
                tab.setLabel(cursor.getString(2));
                tab.setPath(cursor.getString(3));

                //Adding them to list
                tabList.add(tab);
            } while (cursor.moveToNext());
        }

        return tabList;
    }

    public int getTabsCount() {
        String countQuery = "Select * FROM " + TABLE_TAB;
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }
}
