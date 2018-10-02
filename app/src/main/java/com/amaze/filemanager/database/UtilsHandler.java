package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.application.AppConfig;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

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
    private static final int DATABASE_VERSION = 3;  // increment only when making change in schema

    private static final String TABLE_HISTORY = "history";
    private static final String TABLE_HIDDEN = "hidden";
    private static final String TABLE_LIST = "list";
    private static final String TABLE_GRID = "grid";
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String TABLE_SMB = "smb";
    private static final String TABLE_SFTP = "sftp";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_HOST_PUBKEY = "pub_key";
    private static final String COLUMN_PRIVATE_KEY_NAME = "ssh_key_name";
    private static final String COLUMN_PRIVATE_KEY = "ssh_key";
    private final String TEMP_TABLE_PREFIX ="temp_";

    private String queryHistory = "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_PATH + " TEXT UNIQUE"
            + ");";

    private String queryHidden = "CREATE TABLE IF NOT EXISTS " + TABLE_HIDDEN + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_PATH + " TEXT UNIQUE"
            + ");";

    private String queryList = "CREATE TABLE IF NOT EXISTS " + TABLE_LIST + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_PATH + " TEXT UNIQUE"
            + ");";

    private String queryGrid = "CREATE TABLE IF NOT EXISTS " + TABLE_GRID + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_PATH + " TEXT UNIQUE"
            + ");";

    private String queryBookmarks = "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKMARKS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_NAME + " TEXT,"
            + COLUMN_PATH + " TEXT UNIQUE"
            + ");";

    private String querySmb = "CREATE TABLE IF NOT EXISTS " + TABLE_SMB + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_NAME + " TEXT,"
            + COLUMN_PATH + " TEXT UNIQUE"
            + ");";

    private static final String querySftp = "CREATE TABLE IF NOT EXISTS " + TABLE_SFTP + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_NAME + " TEXT,"
            + COLUMN_PATH + " TEXT UNIQUE,"
            + COLUMN_HOST_PUBKEY + " TEXT,"
            + COLUMN_PRIVATE_KEY_NAME + " TEXT,"
            + COLUMN_PRIVATE_KEY + " TEXT"
            + ");";

    public UtilsHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(queryHistory);
        db.execSQL(queryHidden);
        db.execSQL(queryList);
        db.execSQL(queryGrid);
        db.execSQL(queryBookmarks);
        db.execSQL(querySmb);
        db.execSQL(querySftp);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion){
            case 1:
                db.execSQL(querySftp);
            case 2:
                String backupTable = TEMP_TABLE_PREFIX + TABLE_HISTORY;
                db.execSQL(queryHistory.replace(TABLE_HISTORY, backupTable));
                db.execSQL("INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_HISTORY + " group by path;");
                db.execSQL("DROP TABLE " + TABLE_HISTORY + ";");
                db.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_HISTORY + ";");

                backupTable = TEMP_TABLE_PREFIX + TABLE_HIDDEN;
                db.execSQL(queryHidden.replace(TABLE_HIDDEN, backupTable));
                db.execSQL("INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_HIDDEN + " group by path;");
                db.execSQL("DROP TABLE " + TABLE_HIDDEN + ";");
                db.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_HIDDEN + ";");

                backupTable = TEMP_TABLE_PREFIX + TABLE_LIST;
                db.execSQL(queryList.replace(TABLE_LIST, backupTable));
                db.execSQL("INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_LIST + " group by path;");
                db.execSQL("DROP TABLE " + TABLE_LIST + ";");
                db.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_LIST + ";");

                backupTable = TEMP_TABLE_PREFIX + TABLE_GRID;
                db.execSQL(queryGrid.replace(TABLE_GRID, backupTable));
                db.execSQL("INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_GRID + " group by path;");
                db.execSQL("DROP TABLE " + TABLE_GRID + ";");
                db.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_GRID + ";");

                backupTable = TEMP_TABLE_PREFIX + TABLE_BOOKMARKS;
                db.execSQL(queryBookmarks.replace(TABLE_BOOKMARKS, backupTable));
                db.execSQL("INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_BOOKMARKS + " group by path;");
                db.execSQL("DROP TABLE " + TABLE_BOOKMARKS + ";");
                db.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_BOOKMARKS + ";");

                backupTable = TEMP_TABLE_PREFIX + TABLE_SMB;
                db.execSQL(querySmb.replace(TABLE_SMB, backupTable));
                db.execSQL("INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_SMB + " group by path;");
                db.execSQL("DROP TABLE " + TABLE_SMB + ";");
                db.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_SMB + ";");

                backupTable = TEMP_TABLE_PREFIX + TABLE_SFTP;
                db.execSQL(querySftp.replace(TABLE_SFTP, backupTable));
                db.execSQL("INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_SFTP + " group by path;");
                db.execSQL("DROP TABLE " + TABLE_SFTP + ";");
                db.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_SFTP + ";");
            default:
                break;
        }
    }

    public enum Operation {
        HISTORY,
        HIDDEN,
        LIST,
        GRID,
        BOOKMARKS,
        SMB,
        SFTP
    }

    public void saveToDatabase(OperationData operationData) {
        AppConfig.runInBackground(() -> {
            switch (operationData.type) {
                case HIDDEN:
                case HISTORY:
                case LIST:
                case GRID:
                    setPath(operationData.type, operationData.path);
                    break;
                case BOOKMARKS:
                case SMB:
                    setPath(operationData.type, operationData.name, operationData.path);
                    break;
                case SFTP:
                    addSsh(operationData.name, operationData.path, operationData.hostKey,
                            operationData.sshKeyName, operationData.sshKey);
                    break;
                default:
                    throw new IllegalStateException("Unidentified operation!");
            }
        });
    }

    public void removeFromDatabase(OperationData operationData) {
        AppConfig.runInBackground(() -> {
            switch (operationData.type) {
                case HIDDEN:
                case HISTORY:
                case LIST:
                case GRID:
                    removePath(operationData.type, operationData.path);
                    break;
                case BOOKMARKS:
                    removeBookmarksPath(operationData.name, operationData.path);
                    break;
                case SMB:
                    removeSmbPath(operationData.name, operationData.path);
                    break;
                case SFTP:
                    removeSftpPath(operationData.name, operationData.path);
                    break;
                default:
                    throw new IllegalStateException("Unidentified operation!");
            }
        });
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
            saveToDatabase(new OperationData(Operation.BOOKMARKS, new File(dir).getName(), dir));
        }
    }

    public void addSsh(String name, String path, String hostKey, String sshKeyName, String sshKey) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PATH, path);
        values.put(COLUMN_HOST_PUBKEY, hostKey);
        if(sshKey != null && !"".equals(sshKey))
        {
            values.put(COLUMN_PRIVATE_KEY_NAME, sshKeyName);
            values.put(COLUMN_PRIVATE_KEY, sshKey);
        }

        database.insert(getTableForOperation(Operation.SFTP), null, values);
    }

    public void updateSsh(String connectionName, String oldConnectionName, String path,
                          String sshKeyName, String sshKey) {

        SQLiteDatabase database = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, connectionName);
        values.put(COLUMN_PATH, path);
        if(sshKeyName != null && sshKey != null) {
            values.put(COLUMN_PRIVATE_KEY_NAME, sshKeyName);
            values.put(COLUMN_PRIVATE_KEY, sshKey);
        }

        database.update(getTableForOperation(Operation.SFTP), values, String.format("%s=?", COLUMN_NAME),
                new String[]{oldConnectionName});
    }

    public LinkedList<String> getHistoryLinkedList() {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(getTableForOperation(Operation.HISTORY), null,
                null, null, null, null, null);

        LinkedList<String> paths = new LinkedList<>();
        boolean hasNext = cursor.moveToFirst();
        while (hasNext) {
            paths.push(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));
            hasNext = cursor.moveToNext();
        }
        cursor.close();

        return paths;
    }

    public ConcurrentRadixTree<VoidValue> getHiddenFilesConcurrentRadixTree() {
        ConcurrentRadixTree<VoidValue> paths = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());

        Cursor cursor = getReadableDatabase().query(getTableForOperation(Operation.HIDDEN), null,
                null, null, null, null, null);
        boolean hasNext = cursor.moveToFirst();
        while (hasNext) {
            paths.put(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)), VoidValue.SINGLETON);
            hasNext = cursor.moveToNext();
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

        boolean hasNext = cursor.moveToFirst();
        ArrayList<String[]> row = new ArrayList<>();
        while (hasNext) {
            row.add(new String[] {
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_PATH))
            });
            hasNext = cursor.moveToNext();
        }
        cursor.close();
        return row;
    }

    public ArrayList<String[]> getSmbList() {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(getTableForOperation(Operation.SMB), null,
                null, null, null, null, null);
        boolean hasNext = cursor.moveToFirst();
        ArrayList<String[]> row = new ArrayList<>();
        while (hasNext) {
            try {
                row.add(new String[] {
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                        SmbUtil.getSmbDecryptedPath(context, cursor.getString(cursor.getColumnIndex(COLUMN_PATH)))
                });
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();

                // failing to decrypt the path, removing entry from database
                Toast.makeText(context, context.getString(R.string.failed_smb_decrypt_path), Toast.LENGTH_LONG).show();
                removeSmbPath(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                        "");
                continue;
            }
            hasNext = cursor.moveToNext();
        }
        cursor.close();
        return row;
    }

    public List<String[]> getSftpList()
    {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();

        Cursor cursor = sqLiteDatabase.query(getTableForOperation(Operation.SFTP),
                new String[]{COLUMN_NAME,COLUMN_PATH},
                null, null, null, null, COLUMN_ID);

        boolean hasNext = cursor.moveToFirst();
        ArrayList<String[]> retval = new ArrayList<String[]>();
        while(hasNext)
        {
            String path = SshClientUtils.decryptSshPathAsNecessary(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));

            if(path == null) {
                Log.e("ERROR", "Error decrypting path: " + cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));

                // failing to decrypt the path, removing entry from database
                Toast.makeText(context,
                        context.getString(R.string.failed_smb_decrypt_path),
                        Toast.LENGTH_LONG).show();
//                    removeSmbPath(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
//                            "");
                continue;
            } else {
                retval.add(new String[]{
                    cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                    path
                });
            }
            hasNext = cursor.moveToNext();
        }
        cursor.close();
        return retval;
    }

    public String getSshHostKey(String uri)
    {
        uri = SshClientUtils.encryptSshPathAsNecessary(uri);
        if(uri != null)
        {
            SQLiteDatabase sqLiteDatabase = getReadableDatabase();

            Cursor result = sqLiteDatabase.query(TABLE_SFTP, new String[]{COLUMN_HOST_PUBKEY},
                    COLUMN_PATH + " = ?", new String[]{uri},
                    null, null, null);
            if(result.moveToFirst())
            {
                String retval = result.getString(0);
                result.close();
                return retval;
            }
            else
            {
                result.close();
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    public String getSshAuthPrivateKeyName(String uri)
    {
        return getSshAuthPrivateKeyColumn(uri, COLUMN_PRIVATE_KEY_NAME);
    }

    public String getSshAuthPrivateKey(String uri)
    {
        return getSshAuthPrivateKeyColumn(uri, COLUMN_PRIVATE_KEY);
    }

    private String getSshAuthPrivateKeyColumn(String uri, String columnName) {
        //If connection is using key authentication, no need to decrypt the path at all
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor result = sqLiteDatabase.query(TABLE_SFTP, new String[]{columnName},
                COLUMN_PATH + " = ?", new String[]{uri},
                null, null, null);
        if(result.moveToFirst())
        {
            try {
                return result.getString(0);
            }
            finally {
                result.close();
            }
        }
        else {
            result.close();
            return null;
        }
    }

    private void removeBookmarksPath(String name, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        sqLiteDatabase.delete(TABLE_BOOKMARKS, COLUMN_NAME + " = ? AND " + COLUMN_PATH + " = ?",
                new String[] {name, path});
    }

    /**
     * Remove SMB entry
     * @param path the path we get from saved runtime variables is a decrypted, to remove entry,
     *             we must encrypt it's password fiend first first
     */
    private void removeSmbPath(String name, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        try {
            if (path.equals("")) {
                // we don't have a path, remove the entry with this name
                throw new IOException();
            }

            sqLiteDatabase.delete(TABLE_SMB, COLUMN_NAME + " = ? AND " + COLUMN_PATH + " = ?",
                    new String[] {name, SmbUtil.getSmbEncryptedPath(context, path)});
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            // force remove entry, we end up deleting all entries with same name

            sqLiteDatabase.delete(TABLE_SMB, COLUMN_NAME + " = ?",
                    new String[] {name});
        }
    }

    private void removeSftpPath(String name, String path) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();

        try
        {
            if (path.equals("")) {
                // we don't have a path, remove the entry with this name
                throw new IOException();
            }

            sqLiteDatabase.delete(TABLE_SFTP, COLUMN_NAME + " = ? AND " + COLUMN_PATH + " = ?",
                    new String[] {name, SshClientUtils.encryptSshPathAsNecessary(path)});

        }
        catch (IOException e)
        {
            e.printStackTrace();
            // force remove entry, we end up deleting all entries with same name
            sqLiteDatabase.delete(TABLE_SFTP, COLUMN_NAME + " = ?",
                    new String[] {name});
        }
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

        ArrayList<String> paths = new ArrayList<>();

        switch (operation) {
            case LIST:
            case GRID:
                boolean hasNext = cursor.moveToFirst();
                while (hasNext) {
                    paths.add(cursor.getString(cursor.getColumnIndex(COLUMN_PATH)));
                    hasNext = cursor.moveToNext();
                }
                cursor.close();
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

    public void clearTable(Operation table) {
        getWritableDatabase().delete(getTableForOperation(table), null, null);
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
    }

    /**
     * Return table string for corresponding {@link Operation}
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
            case SFTP:
                return TABLE_SFTP;
            default:
                return null;
        }
    }
}
