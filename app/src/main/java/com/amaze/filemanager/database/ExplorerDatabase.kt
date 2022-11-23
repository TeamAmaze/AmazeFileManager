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
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amaze.filemanager.database.daos.CloudEntryDao
import com.amaze.filemanager.database.daos.EncryptedEntryDao
import com.amaze.filemanager.database.daos.SortDao
import com.amaze.filemanager.database.daos.TabDao
import com.amaze.filemanager.database.models.explorer.CloudEntry
import com.amaze.filemanager.database.models.explorer.EncryptedEntry
import com.amaze.filemanager.database.models.explorer.Sort
import com.amaze.filemanager.database.models.explorer.Tab

/**
 * Repository for [Tab], [Sort], [EncryptedEntry], [CloudEntry] in
 * explorer.db in Amaze.
 *
 * @see RoomDatabase
 */
@Database(
    entities = [Tab::class, Sort::class, EncryptedEntry::class, CloudEntry::class],
    version = ExplorerDatabase.DATABASE_VERSION
)
@Suppress("StringLiteralDuplication", "ComplexMethod", "LongMethod")
abstract class ExplorerDatabase : RoomDatabase() {

    /**
     * Returns DAO for [Tab] objects.
     */
    abstract fun tabDao(): TabDao

    /**
     * Returns DAO for [Sort] objects.
     */
    abstract fun sortDao(): SortDao

    /**
     * Returns DAO for [EncryptedEntry] objects.
     */
    abstract fun encryptedEntryDao(): EncryptedEntryDao

    /**
     * Returns DAO for [CloudEntry] objects.
     */
    abstract fun cloudEntryDao(): CloudEntryDao

    companion object {
        private const val DATABASE_NAME = "explorer.db"
        const val DATABASE_VERSION = 11
        const val TABLE_TAB = "tab"
        const val TABLE_CLOUD_PERSIST = "cloud"
        const val TABLE_ENCRYPTED = "encrypted"
        const val TABLE_SORT = "sort"
        const val COLUMN_TAB_NO = "tab_no"
        const val COLUMN_PATH = "path"
        const val COLUMN_HOME = "home"
        const val COLUMN_ENCRYPTED_ID = "_id"
        const val COLUMN_ENCRYPTED_PATH = "path"
        const val COLUMN_ENCRYPTED_PASSWORD = "password"
        const val COLUMN_CLOUD_ID = "_id"
        const val COLUMN_CLOUD_SERVICE = "service"
        const val COLUMN_CLOUD_PERSIST = "persist"
        const val COLUMN_SORT_PATH = "path"
        const val COLUMN_SORT_TYPE = "type"

        @VisibleForTesting
        var overrideDatabaseBuilder: ((Context) -> Builder<ExplorerDatabase>)? = null

        private const val TEMP_TABLE_PREFIX = "temp_"

        // 1->2: add encrypted table (66f08f34)
        internal val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val CREATE_TABLE_ENCRYPTED = (
                    "CREATE TABLE " +
                        TABLE_ENCRYPTED +
                        "(" +
                        COLUMN_ENCRYPTED_ID +
                        " INTEGER PRIMARY KEY," +
                        COLUMN_ENCRYPTED_PATH +
                        " TEXT," +
                        COLUMN_ENCRYPTED_PASSWORD +
                        " TEXT" +
                        ")"
                    )
                database.execSQL(CREATE_TABLE_ENCRYPTED)
            }
        }

        // 2->3: add cloud table (8a5ced1b)
        internal val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val CREATE_TABLE_CLOUD = (
                    "CREATE TABLE " +
                        TABLE_CLOUD_PERSIST +
                        "(" +
                        COLUMN_CLOUD_ID +
                        " INTEGER PRIMARY KEY," +
                        COLUMN_CLOUD_SERVICE +
                        " INTEGER," +
                        COLUMN_CLOUD_PERSIST +
                        " TEXT" +
                        ")"
                    )
                database.execSQL(CREATE_TABLE_CLOUD)
            }
        }

        // 3->4: same as 2->3 (765140f6)
        internal val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) = Unit
        }

        // 4->5: same as 3->4, same as 2->3 (37357436)
        internal val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) = Unit
        }

        // 5->6: add sort table (fe7c0aba)
        internal val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE " +
                        TABLE_SORT +
                        "(" +
                        COLUMN_SORT_PATH +
                        " TEXT PRIMARY KEY," +
                        COLUMN_SORT_TYPE +
                        " INTEGER" +
                        ")"
                )
            }
        }
        internal val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE " +
                        TEMP_TABLE_PREFIX +
                        TABLE_TAB +
                        "(" +
                        COLUMN_TAB_NO +
                        " INTEGER PRIMARY KEY NOT NULL, " +
                        COLUMN_PATH +
                        " TEXT, " +
                        COLUMN_HOME +
                        " TEXT)"
                )
                database.execSQL(
                    "INSERT INTO " +
                        TEMP_TABLE_PREFIX +
                        TABLE_TAB +
                        "(" +
                        COLUMN_TAB_NO +
                        "," +
                        COLUMN_PATH +
                        "," +
                        COLUMN_HOME +
                        ")" +
                        " SELECT " +
                        COLUMN_TAB_NO +
                        "," +
                        COLUMN_PATH +
                        "," +
                        COLUMN_HOME +
                        " FROM " +
                        TABLE_TAB
                )
                database.execSQL("DROP TABLE $TABLE_TAB")
                database.execSQL(
                    "ALTER TABLE $TEMP_TABLE_PREFIX$TABLE_TAB RENAME TO $TABLE_TAB"
                )
                database.execSQL(
                    "CREATE TABLE " +
                        TEMP_TABLE_PREFIX +
                        TABLE_SORT +
                        "(" +
                        COLUMN_SORT_PATH +
                        " TEXT PRIMARY KEY NOT NULL, " +
                        COLUMN_SORT_TYPE +
                        " INTEGER NOT NULL)"
                )
                database.execSQL(
                    "INSERT INTO $TEMP_TABLE_PREFIX$TABLE_SORT SELECT * FROM $TABLE_SORT"
                )
                database.execSQL("DROP TABLE $TABLE_SORT")
                database.execSQL(
                    "ALTER TABLE $TEMP_TABLE_PREFIX$TABLE_SORT RENAME TO $TABLE_SORT"
                )
                database.execSQL(
                    "CREATE TABLE " +
                        TEMP_TABLE_PREFIX +
                        TABLE_ENCRYPTED +
                        "(" +
                        COLUMN_ENCRYPTED_ID +
                        " INTEGER PRIMARY KEY NOT NULL," +
                        COLUMN_ENCRYPTED_PATH +
                        " TEXT," +
                        COLUMN_ENCRYPTED_PASSWORD +
                        " TEXT)"
                )
                database.execSQL(
                    "INSERT INTO " +
                        TEMP_TABLE_PREFIX +
                        TABLE_ENCRYPTED +
                        " SELECT * FROM " +
                        TABLE_ENCRYPTED
                )
                database.execSQL("DROP TABLE $TABLE_ENCRYPTED")
                database.execSQL(
                    "ALTER TABLE " +
                        TEMP_TABLE_PREFIX +
                        TABLE_ENCRYPTED +
                        " RENAME TO " +
                        TABLE_ENCRYPTED
                )
                database.execSQL(
                    "CREATE TABLE " +
                        TEMP_TABLE_PREFIX +
                        TABLE_CLOUD_PERSIST +
                        "(" +
                        COLUMN_CLOUD_ID +
                        " INTEGER PRIMARY KEY NOT NULL," +
                        COLUMN_CLOUD_SERVICE +
                        " INTEGER," +
                        COLUMN_CLOUD_PERSIST +
                        " TEXT)"
                )
                database.execSQL(
                    "INSERT INTO " +
                        TEMP_TABLE_PREFIX +
                        TABLE_CLOUD_PERSIST +
                        " SELECT * FROM " +
                        TABLE_CLOUD_PERSIST
                )
                database.execSQL("DROP TABLE $TABLE_CLOUD_PERSIST")
                database.execSQL(
                    "ALTER TABLE " +
                        TEMP_TABLE_PREFIX +
                        TABLE_CLOUD_PERSIST +
                        " RENAME TO " +
                        TABLE_CLOUD_PERSIST
                )
            }
        }
        internal val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE " +
                        TABLE_CLOUD_PERSIST +
                        " SET " +
                        COLUMN_CLOUD_SERVICE +
                        " = " +
                        COLUMN_CLOUD_SERVICE +
                        "+1"
                )
            }
        }
        internal val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE " +
                        TABLE_CLOUD_PERSIST +
                        " SET " +
                        COLUMN_CLOUD_SERVICE +
                        " = " +
                        COLUMN_CLOUD_SERVICE +
                        "+1"
                )
            }
        }

        internal val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE " +
                        TABLE_CLOUD_PERSIST +
                        " SET " +
                        COLUMN_CLOUD_SERVICE +
                        " = " +
                        COLUMN_CLOUD_SERVICE +
                        "+1"
                )
            }
        }

        internal val MIGRATION_10_11: Migration = object : Migration(10, DATABASE_VERSION) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE " +
                        TABLE_CLOUD_PERSIST +
                        " SET " +
                        COLUMN_CLOUD_SERVICE +
                        " = " +
                        COLUMN_CLOUD_SERVICE +
                        "-2"
                )
            }
        }

        /**
         * Initialize the database. Optionally, may provide a custom way to create the database
         * with supplied [Context].
         */
        @JvmStatic
        fun initialize(context: Context): ExplorerDatabase {
            val builder = overrideDatabaseBuilder?.invoke(context) ?: Room.databaseBuilder(
                context,
                ExplorerDatabase::class.java,
                DATABASE_NAME
            )
            return builder
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
                .addMigrations(MIGRATION_9_10)
                .addMigrations(MIGRATION_10_11)
                .allowMainThreadQueries()
                .build()
        }
    }
}
