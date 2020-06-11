/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amaze.filemanager.database.models.Sort;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;

/** Created by Ning on 5/28/2018. */
public class SortHandler extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "explorer.db";
  public static final String TABLE_SORT = "sort";

  public static final String COLUMN_SORT_PATH = "path";
  public static final String COLUMN_SORT_TYPE = "type";

  public static int getSortType(Context context, String path) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    final Set<String> onlyThisFloders =
        sharedPref.getStringSet(PREFERENCE_SORTBY_ONLY_THIS, new HashSet<>());
    final boolean onlyThis = onlyThisFloders.contains(path);
    final int globalSortby = Integer.parseInt(sharedPref.getString("sortby", "0"));
    if (!onlyThis) {
      return globalSortby;
    }
    SortHandler sortHandler = new SortHandler(context);
    Sort sort = sortHandler.findEntry(path);
    if (sort == null) {
      return globalSortby;
    }
    return sort.type;
  }

  public SortHandler(Context context) {
    super(context, DATABASE_NAME, null, TabHandler.DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String CREATE_TABLE_SORT =
        "CREATE TABLE "
            + TABLE_SORT
            + "("
            + COLUMN_SORT_PATH
            + " TEXT PRIMARY KEY,"
            + COLUMN_SORT_TYPE
            + " INTEGER"
            + ")";

    db.execSQL(CREATE_TABLE_SORT);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    onCreate(db);
  }

  public void addEntry(Sort sort) {
    if (TextUtils.isEmpty(sort.path)) {
      return;
    }
    ContentValues contentValues = new ContentValues();
    contentValues.put(COLUMN_SORT_PATH, sort.path);
    contentValues.put(COLUMN_SORT_TYPE, sort.type);

    SQLiteDatabase sqLiteDatabase = getWritableDatabase();
    sqLiteDatabase.insert(TABLE_SORT, null, contentValues);
  }

  public void clear(String path) {
    try {
      SQLiteDatabase sqLiteDatabase = getWritableDatabase();
      sqLiteDatabase.delete(TABLE_SORT, COLUMN_SORT_PATH + " = ?", new String[] {path});
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
  }

  public void updateEntry(Sort oldSort, Sort newSort) {
    if (TextUtils.isEmpty(oldSort.path) || TextUtils.isEmpty(newSort.path)) {
      return;
    }

    SQLiteDatabase sqLiteDatabase = getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put(COLUMN_SORT_PATH, newSort.path);
    contentValues.put(COLUMN_SORT_TYPE, newSort.type);

    sqLiteDatabase.update(
        TABLE_SORT, contentValues, COLUMN_SORT_PATH + " = ?", new String[] {oldSort.path});
  }

  @Nullable
  public Sort findEntry(String path) {
    if (TextUtils.isEmpty(path)) {
      return null;
    }

    String query =
        "Select * FROM " + TABLE_SORT + " WHERE " + COLUMN_SORT_PATH + "= \"" + path + "\"";
    SQLiteDatabase sqLiteDatabase = getReadableDatabase();
    Cursor cursor = sqLiteDatabase.rawQuery(query, null);
    Sort sort;
    if (cursor.moveToFirst()) {
      sort = new Sort(cursor.getString(0), cursor.getInt(1));
      cursor.close();
    } else {
      sort = null;
    }
    return sort;
  }

  public List<Sort> getAllEntries() {
    List<Sort> entryList = new ArrayList<>();
    // Select all query
    String query = "Select * FROM " + TABLE_SORT;

    SQLiteDatabase sqLiteDatabase = getReadableDatabase();
    Cursor cursor = null;
    try {
      cursor = sqLiteDatabase.rawQuery(query, null);
      // Looping through all rows and adding them to list
      boolean hasNext = cursor.moveToFirst();
      while (hasNext) {
        Sort sort = new Sort(cursor.getString(0), cursor.getInt(1));
        entryList.add(sort);
        hasNext = cursor.moveToNext();
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return entryList;
  }
}
