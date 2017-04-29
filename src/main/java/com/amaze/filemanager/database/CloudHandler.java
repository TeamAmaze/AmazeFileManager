package com.amaze.filemanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.utils.CloudUtil;
import com.amaze.filemanager.utils.CryptUtil;
import com.amaze.filemanager.utils.OpenMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vishal on 18/4/17.
 */

public class CloudHandler extends SQLiteOpenHelper {

    protected static final String TABLE_CLOUD_PERSIST = "cloud";

    protected static final String COLUMN_CLOUD_ID = "_id";
    protected static final String COLUMN_CLOUD_SERVICE = "service";
    protected static final String COLUMN_CLOUD_PERSIST = "persist";

    public static final String CLOUD_PREFIX_BOX = "box:/";
    public static final String CLOUD_PREFIX_DROPBOX = "dropbox:/";
    public static final String CLOUD_PREFIX_GOOGLE_DRIVE = "gdrive:/";
    public static final String CLOUD_PREFIX_ONE_DRIVE = "onedrive:/";

    public static final String CLOUD_NAME_GOOGLE_DRIVE = "GOOGLE DRIVE";
    public static final String CLOUD_NAME_DROPBOX = "DROPBOX";
    public static final String CLOUD_NAME_ONE_DRIVE = "ONE DRIVE";
    public static final String CLOUD_NAME_BOX = "BOX";


    private Context context;

    public CloudHandler(Context context) {
        super(context, TabHandler.DATABASE_NAME, null, TabHandler.DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE_CLOUD = "CREATE TABLE " + TABLE_CLOUD_PERSIST + "("
                + COLUMN_CLOUD_ID
                + " INTEGER PRIMARY KEY,"
                + COLUMN_CLOUD_SERVICE + " INTEGER," + COLUMN_CLOUD_PERSIST + " TEXT" + ")";

        db.execSQL(CREATE_TABLE_CLOUD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLOUD_PERSIST);
        onCreate(db);
    }

    public void addEntry(CloudEntry cloudEntry) throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        ContentValues contentValues = new ContentValues();
        //contentValues.put(COLUMN_ENCRYPTED_ID, encryptedEntry.getId());
        contentValues.put(COLUMN_CLOUD_SERVICE, cloudEntry.getServiceType().ordinal());

        try {

            contentValues.put(COLUMN_CLOUD_PERSIST, CryptUtil.encryptPassword(context,
                    cloudEntry.getPersistData()));
        } catch (Exception e) {
            e.printStackTrace();
            // failed to encrypt, revert back to plain
            contentValues.put(COLUMN_CLOUD_PERSIST, cloudEntry.getPersistData());
        }

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.insert(TABLE_CLOUD_PERSIST, null, contentValues);
        sqLiteDatabase.close();
    }

    public void clear(OpenMode serviceType) {
        try {
            SQLiteDatabase sqLiteDatabase = getWritableDatabase();

            sqLiteDatabase.delete(TABLE_CLOUD_PERSIST, COLUMN_CLOUD_SERVICE + " = ?",
                    new String[]{serviceType.ordinal() + ""});
            sqLiteDatabase.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public void updateEntry(OpenMode serviceType, CloudEntry newCloudEntry)
            throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_CLOUD_ID, newCloudEntry.getId());
        contentValues.put(COLUMN_CLOUD_SERVICE, newCloudEntry.getServiceType().ordinal());
        try {

            contentValues.put(COLUMN_CLOUD_PERSIST, CryptUtil.encryptPassword(context,
                    newCloudEntry.getPersistData()));
        } catch (Exception e) {
            e.printStackTrace();
            // failed to encrypt, revert back to plain
            contentValues.put(COLUMN_CLOUD_PERSIST, newCloudEntry.getPersistData());
        }

        sqLiteDatabase.update(TABLE_CLOUD_PERSIST, contentValues, COLUMN_CLOUD_SERVICE + " = ?",
                new String[]{serviceType.ordinal() + ""});

        sqLiteDatabase.close();
    }

    public CloudEntry findEntry(OpenMode serviceType) throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        String query = "Select * FROM " + TABLE_CLOUD_PERSIST + " WHERE " + COLUMN_CLOUD_SERVICE
                        + "= \"" + serviceType.ordinal() + "\"";
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);
        CloudEntry cloudEntry = new CloudEntry();
        if (cursor.moveToFirst()) {
            cursor.moveToFirst();
            cloudEntry.setId((cursor.getInt(0)));
            cloudEntry.setServiceType(serviceType);
            try {
                cloudEntry.setPersistData(CryptUtil.decryptPassword(context, cursor.getString(2)));
            } catch (Exception e) {
                e.printStackTrace();
                cloudEntry.setPersistData(cursor.getString(2));
                // we're getting plain text, just in case it works,
                // if this doesn't restore the cloud storage state, it'll automatically be updated later
            }

            cursor.close();
        } else {
            cloudEntry = null;
        }
        sqLiteDatabase.close();
        return cloudEntry;
    }

    public List<CloudEntry> getAllEntries() throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        List<CloudEntry> entryList = new ArrayList<>();

        // Select all query
        String query = "Select * FROM " + TABLE_CLOUD_PERSIST;

        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = sqLiteDatabase.rawQuery(query, null);
            // Looping through all rows and adding them to list
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                do {
                    CloudEntry cloudEntry = new CloudEntry();
                    cloudEntry.setId((cursor.getInt(0)));
                    cloudEntry.setServiceType(OpenMode.getOpenMode(cursor.getInt(1)));
                    try {
                        cloudEntry.setPersistData(CryptUtil.decryptPassword(context, cursor.getString(2)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        cloudEntry.setPersistData(cursor.getString(2));
                        // we're getting plain text, just in case it works,
                        // if this doesn't restore the cloud storage state, it'll automatically be updated later
                    }

                    entryList.add(cloudEntry);
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
}
