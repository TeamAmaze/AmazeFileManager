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

import com.amaze.filemanager.database.models.Tab;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vishal on 9/17/2014.
 */
public class TabHandler extends SQLiteOpenHelper {

    protected static final int DATABASE_VERSION = 5;
    protected static final String DATABASE_NAME = "explorer.db";
    protected static final String TABLE_TAB = "tab";

    protected static final String COLUMN_TAB_NO = "tab_no";
    protected static final String COLUMN_LABEL = "label";
    protected static final String COLUMN_PATH = "path";
    protected static final String COLUMN_HOME = "home";

    protected static final String TABLE_ENCRYPTED = "encrypted";

    protected static final String COLUMN_ENCRYPTED_ID = "_id";
    protected static final String COLUMN_ENCRYPTED_PATH = "path";
    protected static final String COLUMN_ENCRYPTED_PASSWORD = "password";

    private Context context;

    public TabHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TAB_TABLE = "CREATE TABLE " + TABLE_TAB + "("
                + COLUMN_TAB_NO + " INTEGER PRIMARY KEY,"
                + COLUMN_PATH + " TEXT,"
                + COLUMN_HOME + " TEXT" +
                ")";

        String CREATE_TABLE_ENCRYPTED = "CREATE TABLE " + TABLE_ENCRYPTED + "("
                + COLUMN_ENCRYPTED_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ENCRYPTED_PATH + " TEXT,"
                + COLUMN_ENCRYPTED_PASSWORD + " TEXT"
                + ")";

        String CREATE_TABLE_CLOUD = "CREATE TABLE " + CloudHandler.TABLE_CLOUD_PERSIST + "("
                + CloudHandler.COLUMN_CLOUD_ID
                + " INTEGER PRIMARY KEY,"
                + CloudHandler.COLUMN_CLOUD_SERVICE + " INTEGER,"
                + CloudHandler.COLUMN_CLOUD_PERSIST + " TEXT" + ")";

        db.execSQL(CREATE_TAB_TABLE);
        db.execSQL(CREATE_TABLE_ENCRYPTED);
        db.execSQL(CREATE_TABLE_CLOUD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TAB);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ENCRYPTED);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CloudHandler.TABLE_CLOUD_PERSIST);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addTab(Tab tab) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TAB_NO, tab.getTab());
        contentValues.put(COLUMN_PATH, tab.getPath());
        contentValues.put(COLUMN_HOME, tab.getHome());
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.insert(TABLE_TAB, null, contentValues);
    }

    public void clear() {
        try {
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            sqLiteDatabase.delete(TABLE_TAB, COLUMN_TAB_NO + " = ?", new String[]{"" + 1});
            sqLiteDatabase.delete(TABLE_TAB, COLUMN_TAB_NO + " = ?", new String[]{"" + 2});
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
            tab.setTab((cursor.getInt(0)));
            tab.setPath(cursor.getString(1));
            tab.setHome(cursor.getString(2));
            cursor.close();
        } else {
            tab = null;
        }
        return tab;
    }

    public List<Tab> getAllTabs() {

        List<Tab> tabList = new ArrayList<>();
        // Select all query
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(TABLE_TAB, null, null, null, null, null, null);
       // cursor.moveToFirst();
        try {

            // Looping through all rows and adding them to list
            if (cursor.moveToFirst() && cursor.getCount() > 0) {

                while (cursor.moveToNext()) {
                    Tab tab = new Tab();
                    tab.setTab((cursor.getInt(0)));
                    tab.setPath(cursor.getString(1));
                    tab.setHome(cursor.getString(2));
                    //Adding them to list
                    tabList.add(tab);
                }
            }
        } finally {
            cursor.close();
        }

        return tabList;
    }
}
