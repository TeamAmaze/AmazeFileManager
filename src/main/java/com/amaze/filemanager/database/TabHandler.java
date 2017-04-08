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
    private static final String TABLE_ENCRYPTED = "encrypted";

    private static final String COLUMN_TAB_NO = "tab_no";
    private static final String COLUMN_LABEL = "label";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_HOME = "home";

    private static final String COLUMN_ENCRYPTED_ID = "_id";
    private static final String COLUMN_ENCRYPTED_PATH = "path";
    private static final String COLUMN_ENCRYPTED_PASSWORD = "password";

    public TabHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TAB_TABLE = "CREATE TABLE " + TABLE_TAB + "("
                + COLUMN_TAB_NO
                + " INTEGER PRIMARY KEY,"
                + COLUMN_PATH + " TEXT," + COLUMN_HOME + " TEXT" + ")";
        String CREATE_TABLE_ENCRYPTED = "CREATE TABLE " + TABLE_ENCRYPTED + "("
                + COLUMN_ENCRYPTED_ID
                + " INTEGER PRIMARY KEY,"
                + COLUMN_ENCRYPTED_PATH + " TEXT," + COLUMN_ENCRYPTED_PASSWORD + " TEXT" + ")";
        sqLiteDatabase.execSQL(CREATE_TAB_TABLE);
        sqLiteDatabase.execSQL(CREATE_TABLE_ENCRYPTED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TAB);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ENCRYPTED);
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

    public void clear() {
        try {
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            sqLiteDatabase.delete(TABLE_TAB, COLUMN_TAB_NO + " = ?", new String[]{"" + 1});
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
            tab.setTab((cursor.getInt(0)));
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
        Cursor cursor = null;
        try {
            cursor = sqLiteDatabase.rawQuery(query, null);
            // Looping through all rows and adding them to list
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                do {
                    Tab tab = new Tab();
                    tab.setTab((cursor.getInt(0)));
                    tab.setPath(cursor.getString(1));
                    tab.setHome(cursor.getString(2));
                    //Adding them to list
                    tabList.add(tab);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        sqLiteDatabase.close();

        return tabList;
    }

    public void close() {
        try {
            getWritableDatabase().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class EncryptCRUD {

        public void addEntry(EncryptedEntry encryptedEntry) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_ENCRYPTED_ID, encryptedEntry.getId());
            contentValues.put(COLUMN_ENCRYPTED_PATH, encryptedEntry.getPath());
            contentValues.put(COLUMN_ENCRYPTED_PASSWORD, encryptedEntry.getPassword());
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            sqLiteDatabase.insert(TABLE_ENCRYPTED, null, contentValues);
            sqLiteDatabase.close();
        }

        public void clear(String path) {
            try {
                SQLiteDatabase sqLiteDatabase = getWritableDatabase();
                sqLiteDatabase.delete(TABLE_ENCRYPTED, COLUMN_ENCRYPTED_PATH + " = ?s", new String[]{path});
                sqLiteDatabase.close();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        public void updateEntryPassword(EncryptedEntry oldEncryptedEntry, EncryptedEntry newEncryptedEntry) {
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_ENCRYPTED_ID, newEncryptedEntry.getId());
            contentValues.put(COLUMN_ENCRYPTED_PATH, newEncryptedEntry.getPath());
            contentValues.put(COLUMN_ENCRYPTED_PASSWORD, newEncryptedEntry.getPassword());
            sqLiteDatabase.update(TABLE_ENCRYPTED, contentValues, COLUMN_ENCRYPTED_PATH + " = ?s",
                    new String[]{oldEncryptedEntry.getPath()});
            sqLiteDatabase.close();
        }

        public EncryptedEntry findEntry(String path) {
            String query = "Select * FROM " + TABLE_ENCRYPTED + " WHERE " + COLUMN_ENCRYPTED_PATH
                    + "= \"" + path + "\"";
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery(query, null);
            EncryptedEntry encryptedEntry = new EncryptedEntry();
            if (cursor.moveToFirst()) {
                cursor.moveToFirst();
                encryptedEntry.setId((cursor.getInt(0)));
                encryptedEntry.setPath(cursor.getString(1));
                encryptedEntry.setPassword(cursor.getString(2));
                cursor.close();
            } else {
                encryptedEntry = null;
            }
            sqLiteDatabase.close();
            return encryptedEntry;
        }

        public List<EncryptedEntry> getAllEntries() {
            List<EncryptedEntry> entryList = new ArrayList<EncryptedEntry>();
            // Select all query
            String query = "Select * FROM " + TABLE_ENCRYPTED;

            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            Cursor cursor = null;
            try {
                cursor = sqLiteDatabase.rawQuery(query, null);
                // Looping through all rows and adding them to list
                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    do {
                        EncryptedEntry encryptedEntry = new EncryptedEntry();
                        encryptedEntry.setId((cursor.getInt(0)));
                        encryptedEntry.setPath(cursor.getString(1));
                        encryptedEntry.setPassword(cursor.getString(2));
                        //Adding them to list
                        entryList.add(encryptedEntry);
                    } while (cursor.moveToNext());
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            sqLiteDatabase.close();

            return entryList;
        }

        public void close() {
            try {
                getWritableDatabase().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
