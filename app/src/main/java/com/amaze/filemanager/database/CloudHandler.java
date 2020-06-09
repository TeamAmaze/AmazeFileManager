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

import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.database.models.CloudEntry;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.files.CryptUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Created by vishal on 18/4/17. */
public class CloudHandler extends SQLiteOpenHelper {

  protected static final String TABLE_CLOUD_PERSIST = "cloud";

  protected static final String COLUMN_CLOUD_ID = "_id";
  protected static final String COLUMN_CLOUD_SERVICE = "service";
  protected static final String COLUMN_CLOUD_PERSIST = "persist";

  public static final String CLOUD_PREFIX_BOX = "box:/";
  public static final String CLOUD_PREFIX_DROPBOX = "dropbox:/";
  public static final String CLOUD_PREFIX_GOOGLE_DRIVE = "gdrive:/";
  public static final String CLOUD_PREFIX_ONE_DRIVE = "onedrive:/";

  public static final String CLOUD_NAME_GOOGLE_DRIVE = "Google Drive";
  public static final String CLOUD_NAME_DROPBOX = "Dropbox";
  public static final String CLOUD_NAME_ONE_DRIVE = "One Drive";
  public static final String CLOUD_NAME_BOX = "Box";

  private Context context;

  public CloudHandler(Context context) {
    super(context, TabHandler.DATABASE_NAME, null, TabHandler.DATABASE_VERSION);
    this.context = context;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    String CREATE_TAB_TABLE =
        "CREATE TABLE "
            + TabHandler.TABLE_TAB
            + "("
            + TabHandler.COLUMN_TAB_NO
            + " INTEGER PRIMARY KEY,"
            + TabHandler.COLUMN_PATH
            + " TEXT,"
            + TabHandler.COLUMN_HOME
            + " TEXT"
            + ")";

    String CREATE_TABLE_ENCRYPTED =
        "CREATE TABLE "
            + TabHandler.TABLE_ENCRYPTED
            + "("
            + TabHandler.COLUMN_ENCRYPTED_ID
            + " INTEGER PRIMARY KEY,"
            + TabHandler.COLUMN_ENCRYPTED_PATH
            + " TEXT,"
            + TabHandler.COLUMN_ENCRYPTED_PASSWORD
            + " TEXT"
            + ")";

    String CREATE_TABLE_CLOUD =
        "CREATE TABLE "
            + CloudHandler.TABLE_CLOUD_PERSIST
            + "("
            + CloudHandler.COLUMN_CLOUD_ID
            + " INTEGER PRIMARY KEY,"
            + CloudHandler.COLUMN_CLOUD_SERVICE
            + " INTEGER,"
            + CloudHandler.COLUMN_CLOUD_PERSIST
            + " TEXT"
            + ")";

    db.execSQL(CREATE_TAB_TABLE);
    db.execSQL(CREATE_TABLE_ENCRYPTED);
    db.execSQL(CREATE_TABLE_CLOUD);
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TabHandler.TABLE_TAB);
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TabHandler.TABLE_ENCRYPTED);
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CloudHandler.TABLE_CLOUD_PERSIST);
    onCreate(sqLiteDatabase);
  }

  public void addEntry(CloudEntry cloudEntry) throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();

    ContentValues contentValues = new ContentValues();
    // contentValues.put(COLUMN_ENCRYPTED_ID, encryptedEntry.getId());
    contentValues.put(COLUMN_CLOUD_SERVICE, cloudEntry.getServiceType().ordinal());

    try {

      contentValues.put(
          COLUMN_CLOUD_PERSIST, CryptUtil.encryptPassword(context, cloudEntry.getPersistData()));
    } catch (Exception e) {
      e.printStackTrace();
      // failed to encrypt, revert back to plain
      contentValues.put(COLUMN_CLOUD_PERSIST, cloudEntry.getPersistData());
    }

    SQLiteDatabase sqLiteDatabase = getWritableDatabase();
    sqLiteDatabase.insert(TABLE_CLOUD_PERSIST, null, contentValues);
  }

  public void clear(OpenMode serviceType) {
    try {
      SQLiteDatabase sqLiteDatabase = getWritableDatabase();

      sqLiteDatabase.delete(
          TABLE_CLOUD_PERSIST,
          COLUMN_CLOUD_SERVICE + " = ?",
          new String[] {serviceType.ordinal() + ""});
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
  }

  public void updateEntry(OpenMode serviceType, CloudEntry newCloudEntry)
      throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();

    SQLiteDatabase sqLiteDatabase = getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put(COLUMN_CLOUD_ID, newCloudEntry.getId());
    contentValues.put(COLUMN_CLOUD_SERVICE, newCloudEntry.getServiceType().ordinal());
    try {

      contentValues.put(
          COLUMN_CLOUD_PERSIST, CryptUtil.encryptPassword(context, newCloudEntry.getPersistData()));
    } catch (Exception e) {
      e.printStackTrace();
      // failed to encrypt, revert back to plain
      contentValues.put(COLUMN_CLOUD_PERSIST, newCloudEntry.getPersistData());
    }

    sqLiteDatabase.update(
        TABLE_CLOUD_PERSIST,
        contentValues,
        COLUMN_CLOUD_SERVICE + " = ?",
        new String[] {serviceType.ordinal() + ""});
  }

  public CloudEntry findEntry(OpenMode serviceType) throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();

    String query =
        "Select * FROM "
            + TABLE_CLOUD_PERSIST
            + " WHERE "
            + COLUMN_CLOUD_SERVICE
            + "= \""
            + serviceType.ordinal()
            + "\"";
    SQLiteDatabase sqLiteDatabase = getReadableDatabase();
    Cursor cursor = sqLiteDatabase.rawQuery(query, null);
    CloudEntry cloudEntry = new CloudEntry();
    if (cursor.moveToFirst()) {
      cloudEntry.setId((cursor.getInt(0)));
      cloudEntry.setServiceType(serviceType);
      try {
        cloudEntry.setPersistData(CryptUtil.decryptPassword(context, cursor.getString(2)));
      } catch (Exception e) {
        e.printStackTrace();
        cloudEntry.setPersistData("");
        return cloudEntry;
      }

      cursor.close();
    } else {
      cloudEntry = null;
    }
    return cloudEntry;
  }

  public List<CloudEntry> getAllEntries() throws CloudPluginException {

    if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw new CloudPluginException();

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
            cloudEntry.setPersistData("");
            entryList.add(cloudEntry);
            continue;
          }

          entryList.add(cloudEntry);
        } while (cursor.moveToNext());
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return entryList;
  }
}
