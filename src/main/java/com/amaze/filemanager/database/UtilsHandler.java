package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.amaze.filemanager.utils.SmbUtil;

import java.util.ArrayList;

/**
 * Created by Vishal on 29-05-2017.
 * Class handles database with tables having list of various utilities like
 * history, hidden files, list paths, grid paths, bookmarks, smb entry
 */

public class UtilsHandler extends SQLiteOpenHelper {

    private Context context;

    private static final String DATABASE_NAME = "utilities.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "history";
    private static final String TABLE_HIDDEN = "hidden";
    private static final String TABLE_LIST = "list";
    private static final String TABLE_GRID = "grid";
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String TABLE_SMB = "smb";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_NAME = "name";

    public UtilsHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String queryHistroy = "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_PATH + " TEXT"
                + ")";
        String queryHidden = "CREATE TABLE IF NOT EXISTS " + TABLE_HIDDEN + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_PATH + " TEXT"
                + ")";
        String queryList = "CREATE TABLE IF NOT EXISTS " + TABLE_LIST + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_PATH + " TEXT"
                + ")";
        String queryGrid = "CREATE TABLE IF NOT EXISTS " + TABLE_GRID + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_PATH + " TEXT"
                + ")";
        String queryBookmarks = "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKMARKS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PATH + " TEXT"
                + ")";
        String querySmb = "CREATE TABLE IF NOT EXISTS " + TABLE_SMB + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PATH + " TEXT"
                + ")";

        db.execSQL(queryHistroy);
        db.execSQL(queryHidden);
        db.execSQL(queryList);
        db.execSQL(queryGrid);
        db.execSQL(queryBookmarks);
        db.execSQL(querySmb);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HIDDEN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GRID);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMB);

        onCreate(db);
    }


    private enum Operation {
        HISTORY,
        HIDDEN,
        LIST,
        GRID,
        BOOKMARKS,
        SMB
    }

    public ArrayList<String> getHistoryList() {
        return getPath(Operation.HISTORY);
    }

    public ArrayList<String> getHiddenList() {
        return getPath(Operation.HIDDEN);
    }

    public ArrayList<String> getListViewList() {
        return getPath(Operation.LIST);
    }

    public ArrayList<String> getGridViewList() {
        return getPath(Operation.GRID);
    }

    public ArrayList<String[]> getBookmarksList() {

        SQLiteDatabase sqLiteDatabase = getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(getTableForOperation(Operation.BOOKMARKS), null,
                null, null, null, null, null);
        cursor.moveToFirst();

        ArrayList<String[]> row = new ArrayList<>();
        try {

            while (!cursor.isAfterLast()) {
                row.add(new String[] {
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_PATH))
                });
            }
        } finally {
            cursor.close();
            sqLiteDatabase.close();
        }
        return row;
    }

    public ArrayList<String[]> getSmbList() {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(getTableForOperation(Operation.SMB), null,
                null, null, null, null, null);
        cursor.moveToFirst();

        ArrayList<String[]> row = new ArrayList<>();
        try {

            while (!cursor.isAfterLast()) {
                row.add(new String[] {
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                        SmbUtil.getSmbDecryptedPath(context, cursor.getString(cursor.getColumnIndex(COLUMN_PATH)))
                });
            }
        } finally {
            cursor.close();
            sqLiteDatabase.close();
        }
        return row;
    }

    public void removeHistoryPath(String path) {
        removePath(Operation.HISTORY, path);
    }

    public void removeHiddenPath(String path) {
        removePath(Operation.HIDDEN, path);
    }

    public void removeListViewPath(String path) {
        removePath(Operation.LIST, path);
    }

    public void removeGridViewPath(String path) {
        removePath(Operation.GRID, path);
    }

    public void removeBookmarksPath(String name, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        try {
            sqLiteDatabase.delete(TABLE_BOOKMARKS, COLUMN_NAME + "=? AND " + COLUMN_PATH + "=?",
                    new String[] {name, path});
        } finally {
            sqLiteDatabase.close();
        }
    }

    public void removeSmbPath(String name, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        try {
            sqLiteDatabase.delete(TABLE_BOOKMARKS, COLUMN_NAME + "=? AND " + COLUMN_PATH + "=?",
                    new String[] {name, path});
        } finally {
            sqLiteDatabase.close();
        }
    }

    public void clearHistoryTable() {
        clearTable(Operation.HISTORY);
    }

    public void clearHiddenTable() {
        clearTable(Operation.HIDDEN);
    }

    public void clearListViewTable() {
        clearTable(Operation.LIST);
    }

    public void clearGridViewTable() {
        clearTable(Operation.GRID);
    }

    public void clearBookmarksTable() {
        clearTable(Operation.BOOKMARKS);
    }

    public void clearSmbTable() {
        clearTable(Operation.SMB);
    }

    private ArrayList<String> getPath(Operation operation) {

        SQLiteDatabase sqLiteDatabase = getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(getTableForOperation(operation), null,
                null, null, null, null, null);
        cursor.moveToFirst();

        switch (operation) {
            case HISTORY:
            case HIDDEN:
            case LIST:
            case GRID:
                ArrayList<String> paths = new ArrayList<>();
                try {

                    while (!cursor.isAfterLast()) {
                        paths.add(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));
                    }
                } finally {
                    cursor.close();
                    sqLiteDatabase.close();
                }
                return paths;
            default:
                return null;
        }
    }

    private void removePath(Operation operation, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        try {
            sqLiteDatabase.delete(getTableForOperation(operation), COLUMN_PATH + "=?",
                    new String[] {path});
        } finally {
            sqLiteDatabase.close();
        }
    }

    private void clearTable(Operation operation) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        try {
            sqLiteDatabase.delete(getTableForOperation(operation), COLUMN_PATH + "=?",
                    new String[] { "NOT NULL" });
        } finally {
            sqLiteDatabase.close();
        }
    }

    private boolean renamePath(Operation operation, String oldName, String oldPath,
                               String newName, String newPath) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        //contentValues.put();
        //sqLiteDatabase.update(getTableForOperation(operation), )
        return false;
    }

    /**
     * Return table string for corresponding {@link Operation}
     * @param operation
     * @return
     */
    private String getTableForOperation(Operation operation) {

        switch (operation) {
            case HISTORY:
                return TABLE_HISTORY;
            case HIDDEN:
                return TABLE_HIDDEN;
            case LIST:
                return TABLE_LIST;
            case GRID:
                return TABLE_GRID;
            case BOOKMARKS:
                return TABLE_BOOKMARKS;
            case SMB:
                return TABLE_SMB;
            default:
                return null;
        }
    }
}
