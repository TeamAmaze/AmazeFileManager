package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.amaze.filemanager.database.models.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ning on 5/28/2018.
 */

public class SortHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "explorer.db";
    public static final String TABLE_SORT = "sort";

    public static final String COLUMN_SORT_ID = "_id";
    public static final String COLUMN_SORT_PATH = "path";
    public static final String COLUMN_SORT_TYPE = "type";

    private Context context;

    @Sort.TYPE
    public static int getSortType(Context context, String path) {
        SortHandler sortHandler = new SortHandler(context);
        Sort sort = sortHandler.findEntry(path);
        if (sort == null) {
            String globalSortType = PreferenceManager.getDefaultSharedPreferences(context).getString("sortby", String.valueOf(Sort.SORT_TYPE_NAME));
            int sortType = Sort.SORT_TYPE_NAME;
            try {
                sortType = Integer.parseInt(globalSortType);
            } catch (NumberFormatException ignored) {
            }
            return sortType;
        }
        return sort.getSortType();
    }

    public SortHandler(Context context) {
        super(context, DATABASE_NAME, null, TabHandler.DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE_SORT = "CREATE TABLE " + TABLE_SORT + "("
                + COLUMN_SORT_ID
                + " INTEGER PRIMARY KEY,"
                + COLUMN_SORT_PATH + " TEXT," + COLUMN_SORT_TYPE + " TEXT" + ")";

        db.execSQL(CREATE_TABLE_SORT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SORT);
        onCreate(db);
    }

    public void addEntry(Sort sort) {

        ContentValues contentValues = new ContentValues();
        //contentValues.put(COLUMN_SORT_ID, encryptedEntry.getId());
        contentValues.put(COLUMN_SORT_PATH, sort.path);
        contentValues.put(COLUMN_SORT_TYPE, sort.sortType);

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.insert(TABLE_SORT, null, contentValues);
    }

    public void clear(String path) {
        try {
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            sqLiteDatabase.delete(TABLE_SORT, COLUMN_SORT_PATH + " = ?", new String[]{path});
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void updateEntry(Sort oldSort, Sort newSort) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SORT_ID, newSort.getId());
        contentValues.put(COLUMN_SORT_PATH, newSort.getPath());
        contentValues.put(COLUMN_SORT_TYPE, newSort.getSortType());

        sqLiteDatabase.update(TABLE_SORT, contentValues, COLUMN_SORT_ID + " = ?",
                new String[]{oldSort.getId() + ""});
    }

    @Nullable
    public Sort findEntry(String path) {
        String query = "Select * FROM " + TABLE_SORT + " WHERE " + COLUMN_SORT_PATH
                + "= \"" + path + "\"";
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        Sort sort = new Sort();
        if (cursor.moveToFirst()) {
            sort.setId((cursor.getInt(0)));
            sort.setPath(cursor.getString(1));
            sort.setSortType(cursor.getInt(2));
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
                Sort sort = new Sort();
                sort.setId((cursor.getInt(0)));
                sort.setPath(cursor.getString(1));
                sort.setSortType(cursor.getInt(2));
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
