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

import static com.amaze.filemanager.database.UtilitiesDatabase.DATABASE_VERSION;

import com.amaze.filemanager.database.daos.BookmarkEntryDao;
import com.amaze.filemanager.database.daos.GridEntryDao;
import com.amaze.filemanager.database.daos.HiddenEntryDao;
import com.amaze.filemanager.database.daos.HistoryEntryDao;
import com.amaze.filemanager.database.daos.ListEntryDao;
import com.amaze.filemanager.database.daos.SftpEntryDao;
import com.amaze.filemanager.database.daos.SmbEntryDao;
import com.amaze.filemanager.database.models.utilities.Bookmark;
import com.amaze.filemanager.database.models.utilities.Grid;
import com.amaze.filemanager.database.models.utilities.Hidden;
import com.amaze.filemanager.database.models.utilities.History;
import com.amaze.filemanager.database.models.utilities.List;
import com.amaze.filemanager.database.models.utilities.SftpEntry;
import com.amaze.filemanager.database.models.utilities.SmbEntry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Repository for {@link Bookmark}, {@link Grid}, {@link Hidden}, {@link History}, {@link List},
 * {@link SmbEntry}, {@link SftpEntry} objects in utilities.db in Amaze.
 *
 * @see RoomDatabase
 */
@Database(
    entities = {
      Bookmark.class,
      Grid.class,
      Hidden.class,
      History.class,
      List.class,
      SmbEntry.class,
      SftpEntry.class
    },
    version = DATABASE_VERSION,
    exportSchema = false)
public abstract class UtilitiesDatabase extends RoomDatabase {

  private static final String DATABASE_NAME = "utilities.db";
  protected static final int DATABASE_VERSION = 5;

  public static final String TABLE_HISTORY = "history";
  public static final String TABLE_HIDDEN = "hidden";
  public static final String TABLE_LIST = "list";
  public static final String TABLE_GRID = "grid";
  public static final String TABLE_BOOKMARKS = "bookmarks";
  public static final String TABLE_SMB = "smb";
  public static final String TABLE_SFTP = "sftp";

  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_PATH = "path";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_HOST_PUBKEY = "pub_key";
  public static final String COLUMN_PRIVATE_KEY_NAME = "ssh_key_name";
  public static final String COLUMN_PRIVATE_KEY = "ssh_key";

  private static final String TEMP_TABLE_PREFIX = "temp_";

  private static final String queryHistory =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_HISTORY
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY,"
          + COLUMN_PATH
          + " TEXT UNIQUE"
          + ");";

  private static final String queryHidden =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_HIDDEN
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY,"
          + COLUMN_PATH
          + " TEXT UNIQUE"
          + ");";

  private static final String queryList =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_LIST
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY,"
          + COLUMN_PATH
          + " TEXT UNIQUE"
          + ");";

  private static final String queryGrid =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_GRID
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY,"
          + COLUMN_PATH
          + " TEXT UNIQUE"
          + ");";

  private static final String queryBookmarks =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_BOOKMARKS
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY,"
          + COLUMN_NAME
          + " TEXT,"
          + COLUMN_PATH
          + " TEXT UNIQUE"
          + ");";

  private static final String querySmb =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_SMB
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY,"
          + COLUMN_NAME
          + " TEXT,"
          + COLUMN_PATH
          + " TEXT UNIQUE"
          + ");";

  private static final String querySftp =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_SFTP
          + " ("
          + COLUMN_ID
          + " INTEGER PRIMARY KEY,"
          + COLUMN_NAME
          + " TEXT,"
          + COLUMN_PATH
          + " TEXT UNIQUE,"
          + COLUMN_HOST_PUBKEY
          + " TEXT,"
          + COLUMN_PRIVATE_KEY_NAME
          + " TEXT,"
          + COLUMN_PRIVATE_KEY
          + " TEXT"
          + ");";

  private static final Migration MIGRATION_1_2 =
      new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.execSQL(
              "CREATE TABLE IF NOT EXISTS "
                  + TABLE_SFTP
                  + " ("
                  + COLUMN_ID
                  + " INTEGER PRIMARY KEY,"
                  + COLUMN_NAME
                  + " TEXT,"
                  + COLUMN_PATH
                  + " TEXT UNIQUE,"
                  + COLUMN_HOST_PUBKEY
                  + " TEXT,"
                  + COLUMN_PRIVATE_KEY_NAME
                  + " TEXT,"
                  + COLUMN_PRIVATE_KEY
                  + " TEXT"
                  + ");");
        }
      };

  private static final Migration MIGRATION_2_3 =
      new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          String backupTable = TEMP_TABLE_PREFIX + TABLE_HISTORY;
          database.execSQL(queryHistory.replace(TABLE_HISTORY, backupTable));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_HISTORY + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_HISTORY + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_HISTORY + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_HIDDEN;
          database.execSQL(queryHidden.replace(TABLE_HIDDEN, backupTable));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_HIDDEN + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_HIDDEN + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_HIDDEN + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_LIST;
          database.execSQL(queryList.replace(TABLE_LIST, backupTable));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_LIST + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_LIST + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_LIST + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_GRID;
          database.execSQL(queryGrid.replace(TABLE_GRID, backupTable));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_GRID + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_GRID + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_GRID + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_BOOKMARKS;
          database.execSQL(queryBookmarks.replace(TABLE_BOOKMARKS, backupTable));
          database.execSQL(
              "INSERT INTO "
                  + backupTable
                  + " SELECT * FROM "
                  + TABLE_BOOKMARKS
                  + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_BOOKMARKS + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_BOOKMARKS + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_SMB;
          database.execSQL(querySmb.replace(TABLE_SMB, backupTable));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_SMB + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_SMB + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_SMB + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_SFTP;
          database.execSQL(querySftp.replace(TABLE_SFTP, backupTable));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_SFTP + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_SFTP + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_SFTP + ";");
        }
      };

  private static final Migration MIGRATION_3_4 =
      new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          String backupTable = TEMP_TABLE_PREFIX + TABLE_HISTORY;
          database.execSQL(
              queryHistory
                  .replace(TABLE_HISTORY, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_HISTORY + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_HISTORY + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_HISTORY + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_HIDDEN;
          database.execSQL(
              queryHidden
                  .replace(TABLE_HIDDEN, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_HIDDEN + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_HIDDEN + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_HIDDEN + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_LIST;
          database.execSQL(
              queryList
                  .replace(TABLE_LIST, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_LIST + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_LIST + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_LIST + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_GRID;
          database.execSQL(
              queryGrid
                  .replace(TABLE_GRID, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_GRID + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_GRID + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_GRID + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_BOOKMARKS;
          database.execSQL(
              queryBookmarks
                  .replace(TABLE_BOOKMARKS, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO "
                  + backupTable
                  + " SELECT * FROM "
                  + TABLE_BOOKMARKS
                  + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_BOOKMARKS + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_BOOKMARKS + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_SMB;
          database.execSQL(
              querySmb
                  .replace(TABLE_SMB, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_SMB + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_SMB + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_SMB + ";");

          backupTable = TEMP_TABLE_PREFIX + TABLE_SFTP;
          database.execSQL(
              querySftp
                  .replace(TABLE_SFTP, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO " + backupTable + " SELECT * FROM " + TABLE_SFTP + " group by path;");
          database.execSQL("DROP TABLE " + TABLE_SFTP + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_SFTP + ";");
        }
      };

  private static final Migration MIGRATION_4_5 =
      new Migration(4, DATABASE_VERSION) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          String backupTable = TEMP_TABLE_PREFIX + TABLE_BOOKMARKS;
          database.execSQL(
              queryBookmarks
                  .replace(TABLE_BOOKMARKS, backupTable)
                  .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,"));
          database.execSQL(
              "INSERT INTO "
                  + backupTable
                  + "("
                  + COLUMN_NAME
                  + ","
                  + COLUMN_PATH
                  + ") SELECT DISTINCT("
                  + COLUMN_NAME
                  + "), "
                  + COLUMN_PATH
                  + " FROM "
                  + TABLE_BOOKMARKS);
          database.execSQL("DROP TABLE " + TABLE_BOOKMARKS + ";");
          database.execSQL("ALTER TABLE " + backupTable + " RENAME TO " + TABLE_BOOKMARKS + ";");

          database.execSQL(
              "CREATE UNIQUE INDEX 'bookmarks_idx' ON "
                  + TABLE_BOOKMARKS
                  + "("
                  + COLUMN_NAME
                  + ", "
                  + COLUMN_PATH
                  + ");");
        }
      };

  protected abstract HiddenEntryDao hiddenEntryDao();

  protected abstract GridEntryDao gridEntryDao();

  protected abstract ListEntryDao listEntryDao();

  protected abstract HistoryEntryDao historyEntryDao();

  protected abstract BookmarkEntryDao bookmarkEntryDao();

  protected abstract SmbEntryDao smbEntryDao();

  protected abstract SftpEntryDao sftpEntryDao();

  public static UtilitiesDatabase initialize(@NonNull Context context) {
    return Room.databaseBuilder(context, UtilitiesDatabase.class, DATABASE_NAME)
        .allowMainThreadQueries()
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build();
  }
}
