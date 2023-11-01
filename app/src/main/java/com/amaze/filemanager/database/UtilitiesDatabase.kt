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

package com.amaze.filemanager.database

import android.content.Context
import android.text.TextUtils
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.daos.BookmarkEntryDao
import com.amaze.filemanager.database.daos.GridEntryDao
import com.amaze.filemanager.database.daos.HiddenEntryDao
import com.amaze.filemanager.database.daos.HistoryEntryDao
import com.amaze.filemanager.database.daos.ListEntryDao
import com.amaze.filemanager.database.daos.SftpEntryDao
import com.amaze.filemanager.database.daos.SmbEntryDao
import com.amaze.filemanager.database.models.utilities.Bookmark
import com.amaze.filemanager.database.models.utilities.Grid
import com.amaze.filemanager.database.models.utilities.Hidden
import com.amaze.filemanager.database.models.utilities.History
import com.amaze.filemanager.database.models.utilities.SftpEntry
import com.amaze.filemanager.database.models.utilities.SmbEntry
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.AT
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.COLON
import com.amaze.filemanager.utils.PasswordUtil.decryptPassword
import com.amaze.filemanager.utils.PasswordUtil.encryptPassword
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Repository for [Bookmark], [Grid], [Hidden], [History], [List],
 * [SmbEntry], [SftpEntry] objects in utilities.db in Amaze.
 *
 * @see RoomDatabase
 */
@Database(
    entities = [
        Bookmark::class,
        Grid::class,
        Hidden::class,
        History::class,
        com.amaze.filemanager.database.models.utilities.List::class,
        SmbEntry::class,
        SftpEntry::class
    ],
    version = UtilitiesDatabase.DATABASE_VERSION,
    exportSchema = false
)
@Suppress("StringLiteralDuplication", "ComplexMethod", "LongMethod")
abstract class UtilitiesDatabase : RoomDatabase() {

    /**
     * Returns DAO for [Hidden] objects.
     */
    abstract fun hiddenEntryDao(): HiddenEntryDao

    /**
     * Returns DAO for [Grid] objects.
     */
    abstract fun gridEntryDao(): GridEntryDao

    /**
     * Returns DAO for [com.amaze.filemanager.database.models.utilities.List] objects.
     */
    abstract fun listEntryDao(): ListEntryDao

    /**
     * Returns DAO for [History] objects.
     */
    abstract fun historyEntryDao(): HistoryEntryDao

    /**
     * Returns DAO for [Bookmark] objects.
     */
    abstract fun bookmarkEntryDao(): BookmarkEntryDao

    /**
     * Returns DAO for [SmbEntry] objects.
     */
    abstract fun smbEntryDao(): SmbEntryDao

    /**
     * Returns DAO for [SftpEntry] objects.
     */
    abstract fun sftpEntryDao(): SftpEntryDao

    companion object {
        private val logger = LoggerFactory.getLogger(UtilitiesDatabase::class.java)
        private const val DATABASE_NAME = "utilities.db"
        const val DATABASE_VERSION = 6
        const val TABLE_HISTORY = "history"
        const val TABLE_HIDDEN = "hidden"
        const val TABLE_LIST = "list"
        const val TABLE_GRID = "grid"
        const val TABLE_BOOKMARKS = "bookmarks"
        const val TABLE_SMB = "smb"
        const val TABLE_SFTP = "sftp"
        const val COLUMN_ID = "_id"
        const val COLUMN_PATH = "path"
        const val COLUMN_NAME = "name"
        const val COLUMN_HOST_PUBKEY = "pub_key"
        const val COLUMN_PRIVATE_KEY_NAME = "ssh_key_name"
        const val COLUMN_PRIVATE_KEY = "ssh_key"

        @VisibleForTesting
        var overrideDatabaseBuilder: ((Context) -> Builder<UtilitiesDatabase>)? = null

        private const val TEMP_TABLE_PREFIX = "temp_"
        private const val queryHistory = (
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_HISTORY +
                " (" +
                COLUMN_ID +
                " INTEGER PRIMARY KEY," +
                COLUMN_PATH +
                " TEXT UNIQUE" +
                ");"
            )
        private const val queryHidden = (
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_HIDDEN +
                " (" +
                COLUMN_ID +
                " INTEGER PRIMARY KEY," +
                COLUMN_PATH +
                " TEXT UNIQUE" +
                ");"
            )
        private const val queryList = (
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_LIST +
                " (" +
                COLUMN_ID +
                " INTEGER PRIMARY KEY," +
                COLUMN_PATH +
                " TEXT UNIQUE" +
                ");"
            )
        private const val queryGrid = (
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_GRID +
                " (" +
                COLUMN_ID +
                " INTEGER PRIMARY KEY," +
                COLUMN_PATH +
                " TEXT UNIQUE" +
                ");"
            )
        private const val queryBookmarks = (
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_BOOKMARKS +
                " (" +
                COLUMN_ID +
                " INTEGER PRIMARY KEY," +
                COLUMN_NAME +
                " TEXT," +
                COLUMN_PATH +
                " TEXT UNIQUE" +
                ");"
            )
        private const val querySmb = (
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_SMB +
                " (" +
                COLUMN_ID +
                " INTEGER PRIMARY KEY," +
                COLUMN_NAME +
                " TEXT," +
                COLUMN_PATH +
                " TEXT UNIQUE" +
                ");"
            )
        private const val querySftp = (
            "CREATE TABLE IF NOT EXISTS " +
                TABLE_SFTP +
                " (" +
                COLUMN_ID +
                " INTEGER PRIMARY KEY," +
                COLUMN_NAME +
                " TEXT," +
                COLUMN_PATH +
                " TEXT UNIQUE," +
                COLUMN_HOST_PUBKEY +
                " TEXT," +
                COLUMN_PRIVATE_KEY_NAME +
                " TEXT," +
                COLUMN_PRIVATE_KEY +
                " TEXT" +
                ");"
            )

        internal val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS " +
                        TABLE_SFTP +
                        " (" +
                        COLUMN_ID +
                        " INTEGER PRIMARY KEY," +
                        COLUMN_NAME +
                        " TEXT," +
                        COLUMN_PATH +
                        " TEXT UNIQUE," +
                        COLUMN_HOST_PUBKEY +
                        " TEXT," +
                        COLUMN_PRIVATE_KEY_NAME +
                        " TEXT," +
                        COLUMN_PRIVATE_KEY +
                        " TEXT" +
                        ");"
                )
            }
        }

        internal val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                var backupTable = TEMP_TABLE_PREFIX + TABLE_HISTORY
                database.execSQL(queryHistory.replace(TABLE_HISTORY, backupTable))
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_HISTORY group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_HISTORY;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_HISTORY;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_HIDDEN
                database.execSQL(queryHidden.replace(TABLE_HIDDEN, backupTable))
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_HIDDEN group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_HIDDEN;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_HIDDEN;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_LIST
                database.execSQL(queryList.replace(TABLE_LIST, backupTable))
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_LIST group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_LIST;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_LIST;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_GRID
                database.execSQL(queryGrid.replace(TABLE_GRID, backupTable))
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_GRID group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_GRID;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_GRID;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_BOOKMARKS
                database.execSQL(queryBookmarks.replace(TABLE_BOOKMARKS, backupTable))
                database.execSQL(
                    "INSERT INTO " +
                        backupTable +
                        " SELECT * FROM " +
                        TABLE_BOOKMARKS +
                        " group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_BOOKMARKS;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_BOOKMARKS;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_SMB
                database.execSQL(querySmb.replace(TABLE_SMB, backupTable))
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_SMB group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_SMB;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_SMB;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_SFTP
                database.execSQL(querySftp.replace(TABLE_SFTP, backupTable))
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_SFTP group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_SFTP;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_SFTP;")
            }
        }

        internal val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                var backupTable = TEMP_TABLE_PREFIX + TABLE_HISTORY
                database.execSQL(
                    queryHistory
                        .replace(TABLE_HISTORY, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_HISTORY group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_HISTORY;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_HISTORY;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_HIDDEN
                database.execSQL(
                    queryHidden
                        .replace(TABLE_HIDDEN, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_HIDDEN group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_HIDDEN;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_HIDDEN;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_LIST
                database.execSQL(
                    queryList
                        .replace(TABLE_LIST, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_LIST group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_LIST;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_LIST;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_GRID
                database.execSQL(
                    queryGrid
                        .replace(TABLE_GRID, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_GRID group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_GRID;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_GRID;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_BOOKMARKS
                database.execSQL(
                    queryBookmarks
                        .replace(TABLE_BOOKMARKS, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO " +
                        backupTable +
                        " SELECT * FROM " +
                        TABLE_BOOKMARKS +
                        " group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_BOOKMARKS;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_BOOKMARKS;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_SMB
                database.execSQL(
                    querySmb
                        .replace(TABLE_SMB, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_SMB group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_SMB;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_SMB;")
                backupTable = TEMP_TABLE_PREFIX + TABLE_SFTP
                database.execSQL(
                    querySftp
                        .replace(TABLE_SFTP, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO $backupTable SELECT * FROM $TABLE_SFTP group by path;"
                )
                database.execSQL("DROP TABLE $TABLE_SFTP;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_SFTP;")
            }
        }

        internal val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val backupTable = TEMP_TABLE_PREFIX + TABLE_BOOKMARKS
                database.execSQL(
                    queryBookmarks
                        .replace(TABLE_BOOKMARKS, backupTable)
                        .replace("PRIMARY KEY,", "PRIMARY KEY NOT NULL,")
                )
                database.execSQL(
                    "INSERT INTO " +
                        backupTable +
                        "(" +
                        COLUMN_NAME +
                        "," +
                        COLUMN_PATH +
                        ") SELECT DISTINCT(" +
                        COLUMN_NAME +
                        "), " +
                        COLUMN_PATH +
                        " FROM " +
                        TABLE_BOOKMARKS
                )
                database.execSQL("DROP TABLE $TABLE_BOOKMARKS;")
                database.execSQL("ALTER TABLE $backupTable RENAME TO $TABLE_BOOKMARKS;")
                database.execSQL(
                    "CREATE UNIQUE INDEX 'bookmarks_idx' ON " +
                        TABLE_BOOKMARKS +
                        "(" +
                        COLUMN_NAME +
                        ", " +
                        COLUMN_PATH +
                        ");"
                )
            }
        }

        private fun migratePasswordInUris(
            database: SupportSQLiteDatabase,
            tableName: String
        ): List<String> {
            val updateSqls: MutableList<String> = ArrayList()
            val cursor =
                database.query("SELECT $COLUMN_NAME, $COLUMN_PATH FROM $tableName")
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                val oldPath = cursor.getString(1)
                if (oldPath.contains(AT)) {
                    val userCredentials =
                        oldPath.substring(oldPath.indexOf("://") + 3, oldPath.lastIndexOf(AT))
                    if (userCredentials.contains(":")) {
                        val password = userCredentials.substring(
                            userCredentials.lastIndexOf(COLON) + 1
                        )
                        if (!TextUtils.isEmpty(password)) {
                            try {
                                val oldPassword = decryptPassword(
                                    AppConfig.getInstance(),
                                    password,
                                    Base64.DEFAULT
                                )
                                val newPassword = encryptPassword(
                                    AppConfig.getInstance(),
                                    oldPassword,
                                    Base64.URL_SAFE
                                )
                                val newPath = oldPath.replace(password, newPassword!!)
                                updateSqls.add(
                                    "UPDATE " +
                                        tableName +
                                        " SET PATH = '" +
                                        newPath +
                                        "' WHERE " +
                                        COLUMN_NAME +
                                        "='" +
                                        name +
                                        "' AND " +
                                        COLUMN_PATH +
                                        "='" +
                                        oldPath +
                                        "'"
                                )
                            } catch (e: GeneralSecurityException) {
                                logger.error("Error migrating database records")
                            } catch (e: IOException) {
                                logger.error("Error migrating database records")
                            }
                        }
                    }
                }
            }
            cursor.close()
            return updateSqls
        }

        internal val MIGRATION_5_6: Migration = object : Migration(5, DATABASE_VERSION) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val updateSqls: MutableList<String> = ArrayList()
                updateSqls.addAll(migratePasswordInUris(database, TABLE_SMB))
                updateSqls.addAll(migratePasswordInUris(database, TABLE_SFTP))
                for (updateSql in updateSqls) {
                    database.execSQL(updateSql)
                }
            }
        }

        /**
         * Initialize the database. Optionally, may provide a custom way to create the database
         * with supplied [Context].
         */
        @JvmStatic
        fun initialize(context: Context): UtilitiesDatabase {
            val builder: Builder<UtilitiesDatabase> =
                overrideDatabaseBuilder?.invoke(context) ?: Room.databaseBuilder(
                    context,
                    UtilitiesDatabase::class.java,
                    DATABASE_NAME
                )
            return builder
                .allowMainThreadQueries()
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                .build()
        }
    }
}
