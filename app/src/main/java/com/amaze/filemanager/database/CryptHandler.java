package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.utils.files.CryptUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vishal on 15/4/17.
 */

public class CryptHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "explorer.db";
    private static final String TABLE_ENCRYPTED = "encrypted";

    private static final String COLUMN_ENCRYPTED_ID = "_id";
    private static final String COLUMN_ENCRYPTED_PATH = "path";
    private static final String COLUMN_ENCRYPTED_PASSWORD = "password";

    private Context context;

    public CryptHandler(Context context) {
        super(context, DATABASE_NAME, null, TabHandler.DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE_ENCRYPTED = "CREATE TABLE " + TABLE_ENCRYPTED + "("
                + COLUMN_ENCRYPTED_ID
                + " INTEGER PRIMARY KEY,"
                + COLUMN_ENCRYPTED_PATH + " TEXT," + COLUMN_ENCRYPTED_PASSWORD + " TEXT" + ")";

        db.execSQL(CREATE_TABLE_ENCRYPTED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENCRYPTED);
        onCreate(db);
    }

    public void addEntry(EncryptedEntry encryptedEntry) throws GeneralSecurityException, IOException {

        ContentValues contentValues = new ContentValues();
        //contentValues.put(COLUMN_ENCRYPTED_ID, encryptedEntry.getId());
        contentValues.put(COLUMN_ENCRYPTED_PATH, encryptedEntry.getPath());
        contentValues.put(COLUMN_ENCRYPTED_PASSWORD, CryptUtil.encryptPassword(context,
                encryptedEntry.getPassword()));

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.insert(TABLE_ENCRYPTED, null, contentValues);
    }

    public void clear(String path) {
        try {
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();
            sqLiteDatabase.delete(TABLE_ENCRYPTED, COLUMN_ENCRYPTED_PATH + " = ?", new String[]{path});
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void updateEntry(EncryptedEntry oldEncryptedEntry, EncryptedEntry newEncryptedEntry) throws GeneralSecurityException, IOException  {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ENCRYPTED_ID, newEncryptedEntry.getId());
        contentValues.put(COLUMN_ENCRYPTED_PATH, newEncryptedEntry.getPath());
        contentValues.put(COLUMN_ENCRYPTED_PASSWORD, CryptUtil.encryptPassword(context,
                newEncryptedEntry.getPassword()));

        sqLiteDatabase.update(TABLE_ENCRYPTED, contentValues, COLUMN_ENCRYPTED_ID + " = ?",
                new String[]{oldEncryptedEntry.getId() + ""});
    }

    public EncryptedEntry findEntry(String path) throws GeneralSecurityException, IOException {
        String query = "Select * FROM " + TABLE_ENCRYPTED + " WHERE " + COLUMN_ENCRYPTED_PATH
                + "= \"" + path + "\"";
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        EncryptedEntry encryptedEntry = new EncryptedEntry();
        if (cursor.moveToFirst()) {
            encryptedEntry.setId((cursor.getInt(0)));
            encryptedEntry.setPath(cursor.getString(1));
            encryptedEntry.setPassword(CryptUtil.decryptPassword(context, cursor.getString(2)));
            cursor.close();
        } else {
            encryptedEntry = null;
        }
        return encryptedEntry;
    }

    public List<EncryptedEntry> getAllEntries() throws GeneralSecurityException, IOException {
        List<EncryptedEntry> entryList = new ArrayList<EncryptedEntry>();
        // Select all query
        String query = "Select * FROM " + TABLE_ENCRYPTED;

        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = sqLiteDatabase.rawQuery(query, null);
            // Looping through all rows and adding them to list
            boolean hasNext = cursor.moveToFirst();
            while(hasNext) {
                EncryptedEntry encryptedEntry = new EncryptedEntry();
                encryptedEntry.setId((cursor.getInt(0)));
                encryptedEntry.setPath(cursor.getString(1));
                encryptedEntry.setPassword(CryptUtil.decryptPassword(context,
                        cursor.getString(2)));

                entryList.add(encryptedEntry);

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
