package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.exceptions.CryptException;
import com.amaze.filemanager.utils.SmbUtil;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Vishal on 29-05-2017.
 * Class handles database with tables having list of various utilities like
 * history, hidden files, list paths, grid paths, bookmarks, smb entry
 *
 * Try to use these functions from a background thread
 */

public class UtilsHandler extends SQLiteOpenHelper {

    private Context context;

    private static final String DATABASE_NAME = "utilities.db";
    private static final int DATABASE_VERSION = 1;  // increment only when making change in schema

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
        String queryHistory = "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + " ("
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

        db.execSQL(queryHistory);
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

    public void addCommonBookmarks() {
        String sd = Environment.getExternalStorageDirectory() + "/";

        String[] dirs = new String[] {
                sd + Environment.DIRECTORY_DCIM,
                sd + Environment.DIRECTORY_DOWNLOADS,
                sd + Environment.DIRECTORY_MOVIES,
                sd + Environment.DIRECTORY_MUSIC,
                sd + Environment.DIRECTORY_PICTURES
        };

        for (String dir : dirs) {

            addBookmark(new File(dir).getName(), dir);
        }
    }

    public void addHistory(String path) {
        setPath(Operation.HISTORY, path);
    }

    public void addHidden(String path) {
        setPath(Operation.HIDDEN, path);
    }

    public void addListView(String path) {
        setPath(Operation.LIST, path);
    }

    public void addGridView(String path) {
        setPath(Operation.GRID, path);
    }

    public void addBookmark(String name, String path) {
        setPath(Operation.BOOKMARKS, name, path);
    }

    public void addSmb(String name, String path) {
        setPath(Operation.SMB, name, path);
    }

    public ArrayList<String> getHistoryList() {
        return getPath(Operation.HISTORY);
    }

    public ConcurrentRadixTree<VoidValue> getHiddenFilesConcurrentRadixTree() {
        ConcurrentRadixTree<VoidValue> paths = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());

        Cursor cursor = getReadableDatabase().query(getTableForOperation(Operation.HIDDEN), null,
                null, null, null, null, null);
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            paths.put(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)), VoidValue.SINGLETON);
        }
        cursor.close();

        return paths;
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

            while (cursor.moveToNext()) {
                row.add(new String[] {
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_PATH))
                });
            }
        } finally {
            cursor.close();
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

            while (cursor.moveToNext()) {
                try {
                    row.add(new String[] {
                            cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                            SmbUtil.getSmbDecryptedPath(context, cursor.getString(cursor.getColumnIndex(COLUMN_PATH)))
                    });
                } catch (CryptException e) {
                    e.printStackTrace();

                    // failing to decrypt the path, removing entry from database
                    Toast.makeText(context,
                            context.getResources().getString(R.string.failed_smb_decrypt_path),
                            Toast.LENGTH_LONG).show();
                    removeSmbPath(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                            "");
                    continue;
                }
            }
        } finally {
            cursor.close();
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

        sqLiteDatabase.delete(TABLE_BOOKMARKS, COLUMN_NAME + " = ? AND " + COLUMN_PATH + " = ?",
                new String[] {name, path});
    }

    /**
     * Remove SMB entry
     * @param name
     * @param path the path we get from saved runtime variables is a decrypted, to remove entry,
     *             we must encrypt it's password fiend first first
     */
    public void removeSmbPath(String name, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        try {
            if (path.equals("")) {
                // we don't have a path, remove the entry with this name
                throw new CryptException();
            }

            sqLiteDatabase.delete(TABLE_SMB, COLUMN_NAME + " = ? AND " + COLUMN_PATH + " = ?",
                    new String[] {name, SmbUtil.getSmbEncryptedPath(context, path)});
        } catch (CryptException e) {
            e.printStackTrace();
            // force remove entry, we end up deleting all entries with same name

            sqLiteDatabase.delete(TABLE_SMB, COLUMN_NAME + " = ?",
                    new String[] {name});
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

    public void renameBookmark(String oldName, String oldPath, String newName, String newPath) {
        renamePath(Operation.BOOKMARKS, oldName, oldPath, newName, newPath);
    }

    public void renameSMB(String oldName, String oldPath, String newName, String newPath) {
        renamePath(Operation.SMB, oldName, oldPath, newName, newPath);
    }

    private void setPath(Operation operation, String path) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_PATH, path);

        sqLiteDatabase.insert(getTableForOperation(operation), null, contentValues);
    }

    private void setPath(Operation operation, String name, String path) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, name);
        contentValues.put(COLUMN_PATH, path);

        sqLiteDatabase.insert(getTableForOperation(operation), null, contentValues);
    }

    private ArrayList<String> getPath(Operation operation) {

        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(getTableForOperation(operation), null,
                null, null, null, null, null);
        cursor.moveToFirst();

        switch (operation) {
            case HISTORY:
            case LIST:
            case GRID:
                ArrayList<String> paths = new ArrayList<>();
                try {

                    while (cursor.moveToNext()) {
                        paths.add(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));
                    }
                } finally {
                    cursor.close();
                }
                return paths;
            default:
                return null;
        }
    }

    private void removePath(Operation operation, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        sqLiteDatabase.delete(getTableForOperation(operation), COLUMN_PATH + "=?",
                new String[] {path});
    }

    private void clearTable(Operation operation) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        sqLiteDatabase.delete(getTableForOperation(operation), COLUMN_PATH + "=?",
                new String[] { "NOT NULL" });
    }

    private void renamePath(Operation operation, String name, String path) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, name);
        contentValues.put(COLUMN_PATH, path);

        sqLiteDatabase.update(getTableForOperation(operation), contentValues,
                COLUMN_PATH + "=?", new String[] {name});
    }

    private void renamePath(Operation operation, String oldName, String oldPath,
                               String newName, String newPath) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, newName);
        contentValues.put(COLUMN_PATH, newPath);

        sqLiteDatabase.update(getTableForOperation(operation), contentValues, COLUMN_NAME
                + "=? AND " + COLUMN_PATH + "=?", new String[] {oldName, oldPath});
        return;
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
