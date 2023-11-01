/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import io.reactivex.schedulers.Schedulers
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Tests for [ExplorerDatabase] migrations.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
@Suppress("StringLiteralDuplication", "ComplexMethod", "LongMethod")
class ExplorerDatabaseMigrationTest {

    @Rule
    @JvmField
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ExplorerDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Sanity check for all migrations.
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.close()
        val explorerDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ExplorerDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                ExplorerDatabase.MIGRATION_1_2,
                ExplorerDatabase.MIGRATION_2_3,
                ExplorerDatabase.MIGRATION_3_4,
                ExplorerDatabase.MIGRATION_4_5,
                ExplorerDatabase.MIGRATION_5_6,
                ExplorerDatabase.MIGRATION_6_7,
                ExplorerDatabase.MIGRATION_7_8,
                ExplorerDatabase.MIGRATION_8_9,
                ExplorerDatabase.MIGRATION_9_10,
                ExplorerDatabase.MIGRATION_10_11
            )
            .build()
        explorerDatabase.openHelper.writableDatabase
        explorerDatabase.close()
    }

    /**
     * Test migrate from v5 to v6 - add sort table.
     */
    @Test
    @Throws(IOException::class)
    fun migrateFromV5() {
        val db = helper.createDatabase(TEST_DB, 5)
        db.close()
        val explorerDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ExplorerDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                ExplorerDatabase.MIGRATION_5_6,
                ExplorerDatabase.MIGRATION_6_7,
                ExplorerDatabase.MIGRATION_7_8,
                ExplorerDatabase.MIGRATION_8_9,
                ExplorerDatabase.MIGRATION_9_10,
                ExplorerDatabase.MIGRATION_10_11
            )
            .build()
        explorerDatabase.openHelper.writableDatabase
        explorerDatabase.close()
    }

    /**
     * Test migrate from v6 to v7 - fix primary keys
     */
    @Test
    @Throws(IOException::class)
    fun migrateFromV6() {
        val db = helper.createDatabase(TEST_DB, 6)
        db.close()
        val explorerDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ExplorerDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                ExplorerDatabase.MIGRATION_6_7,
                ExplorerDatabase.MIGRATION_7_8,
                ExplorerDatabase.MIGRATION_8_9,
                ExplorerDatabase.MIGRATION_9_10,
                ExplorerDatabase.MIGRATION_10_11
            )
            .build()
        explorerDatabase.openHelper.writableDatabase
        explorerDatabase.close()
    }

    /**
     * Test migrate from v7 to v8, after shifting OpenMode by 1.
     */
    @Test
    @Throws(IOException::class)
    fun migrateFromV7() {
        val db = helper.createDatabase(TEST_DB, 7)
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (1," +
                (OpenMode.GDRIVE.ordinal - 1) +
                ",'abcd')"
        )
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (2," +
                (OpenMode.DROPBOX.ordinal - 1) +
                ",'efgh')"
        )
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (3," +
                (OpenMode.BOX.ordinal - 1) +
                ",'ijkl')"
        )
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (4," +
                (OpenMode.ONEDRIVE.ordinal - 1) +
                ",'mnop')"
        )
        db.close()
        val explorerDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ExplorerDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                ExplorerDatabase.MIGRATION_7_8,
                ExplorerDatabase.MIGRATION_8_9,
                ExplorerDatabase.MIGRATION_9_10,
                ExplorerDatabase.MIGRATION_10_11
            ).allowMainThreadQueries()
            .build()
        explorerDatabase.openHelper.writableDatabase
        var verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.GDRIVE.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(1, verify.id.toLong())
        Assert.assertEquals("abcd", verify.persistData.toString())
        verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.BOX.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(3, verify.id.toLong())
        Assert.assertEquals("ijkl", verify.persistData.toString())
        verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.DROPBOX.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(2, verify.id.toLong())
        Assert.assertEquals("efgh", verify.persistData.toString())
        verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.ONEDRIVE.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(4, verify.id.toLong())
        Assert.assertEquals("mnop", verify.persistData.toString())
        explorerDatabase.close()
    }

    /**
     * Test migrate from v8 to v9, after shifting OpenMode by 1 again.
     */
    @Test
    @Throws(IOException::class)
    fun migrateFromV8() {
        val db = helper.createDatabase(TEST_DB, 8)
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (1," +
                (OpenMode.GDRIVE.ordinal) +
                ",'abcd')"
        )
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (2," +
                (OpenMode.DROPBOX.ordinal) +
                ",'efgh')"
        )
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (3," +
                (OpenMode.BOX.ordinal) +
                ",'ijkl')"
        )
        db.execSQL(
            "INSERT INTO " +
                ExplorerDatabase.TABLE_CLOUD_PERSIST +
                "(" +
                ExplorerDatabase.COLUMN_CLOUD_ID +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_SERVICE +
                "," +
                ExplorerDatabase.COLUMN_CLOUD_PERSIST +
                ") VALUES (4," +
                (OpenMode.ONEDRIVE.ordinal) +
                ",'mnop')"
        )
        db.close()
        val explorerDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ExplorerDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                ExplorerDatabase.MIGRATION_8_9,
                ExplorerDatabase.MIGRATION_9_10,
                ExplorerDatabase.MIGRATION_10_11
            )
            .allowMainThreadQueries()
            .build()
        explorerDatabase.openHelper.writableDatabase
        var verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.GDRIVE.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(1, verify.id.toLong())
        Assert.assertEquals("abcd", verify.persistData.toString())
        verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.BOX.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(3, verify.id.toLong())
        Assert.assertEquals("ijkl", verify.persistData.toString())
        verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.DROPBOX.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(2, verify.id.toLong())
        Assert.assertEquals("efgh", verify.persistData.toString())
        verify = explorerDatabase
            .cloudEntryDao()
            .findByServiceType(OpenMode.ONEDRIVE.ordinal)
            .subscribeOn(Schedulers.trampoline())
            .blockingGet()
        Assert.assertEquals(4, verify.id.toLong())
        Assert.assertEquals("mnop", verify.persistData.toString())
        explorerDatabase.close()
    }

    companion object {
        private const val TEST_DB = "explorer-test"
    }
}
