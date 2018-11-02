package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.amaze.filemanager.database.models.Folder;

/**
 * Created by llrraa on 10/28/2018. To store folder size.
 */

public class FolderHandler extends SQLiteOpenHelper {
    protected static final String DATABASE_NAME = "folders.db";
    protected static final String TABLE_FOLDER = "folders1";

    public static final String COLUMN_FOLDER_PATH = "path";
    public static final String COLUMN_FOLDER_SIZE = "size";
    public static final String COLUMN_FOLDER_TIME = "time";

    public FolderHandler(Context context) {
        super(context, DATABASE_NAME, null, TabHandler.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_SORT = "CREATE TABLE IF NOT EXISTS " + TABLE_FOLDER + "("
                + COLUMN_FOLDER_PATH
                + " TEXT PRIMARY KEY,"
                + COLUMN_FOLDER_SIZE + " INTEGER," + COLUMN_FOLDER_TIME + " INTEGER" + ")";
         db.execSQL(CREATE_TABLE_SORT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_FOLDER);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    public void addEntry(Folder folder) {
        if (TextUtils.isEmpty(folder.path)) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_FOLDER_PATH, folder.path);
        contentValues.put(COLUMN_FOLDER_SIZE, folder.size);
        contentValues.put(COLUMN_FOLDER_TIME, folder.time);

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.insert(TABLE_FOLDER, null, contentValues);
    }


    public void updateEntry(Folder oldFolder, Folder newFolder) {
        if (TextUtils.isEmpty(oldFolder.path) || TextUtils.isEmpty(newFolder.path)) {
            return;
        }

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_FOLDER_PATH, newFolder.path);
        contentValues.put(COLUMN_FOLDER_SIZE, newFolder.size);
        contentValues.put(COLUMN_FOLDER_TIME, newFolder.time);

        sqLiteDatabase.update(TABLE_FOLDER, contentValues, COLUMN_FOLDER_PATH + " = ?",
                new String[]{oldFolder.path});
    }


    public void updateEntry(Folder newFolder) {
        Folder oldFolder = findEntry(newFolder.path);
        if (oldFolder == null) {
            addEntry(newFolder);
        } else {
            updateEntry(oldFolder, newFolder);
        }

    }

        @Nullable
    public Folder findEntry(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }

        String query = "Select * FROM " + TABLE_FOLDER + " WHERE " + COLUMN_FOLDER_PATH
                + "= \"" + path + "\"";
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        Folder folder;
        if (cursor.moveToFirst()) {
            folder = new Folder(cursor.getString(0), cursor.getLong(1),  cursor.getLong(2));
            cursor.close();
        } else {
            folder = null;
        }
        return folder;
    }


}
