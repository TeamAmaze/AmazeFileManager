/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import android.util.Base64
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amaze.filemanager.database.UtilitiesDatabase.Companion.COLUMN_HOST_PUBKEY
import com.amaze.filemanager.database.UtilitiesDatabase.Companion.COLUMN_NAME
import com.amaze.filemanager.database.UtilitiesDatabase.Companion.COLUMN_PATH
import com.amaze.filemanager.database.UtilitiesDatabase.Companion.COLUMN_PRIVATE_KEY
import com.amaze.filemanager.database.UtilitiesDatabase.Companion.COLUMN_PRIVATE_KEY_NAME
import com.amaze.filemanager.database.UtilitiesDatabase.Companion.TABLE_SFTP
import com.amaze.filemanager.database.UtilitiesDatabase.Companion.TABLE_SMB
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.PasswordUtil
import com.amaze.filemanager.utils.smb.SmbUtil
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Test [UtilitiesDatabase] migration.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
class UtilitiesDatabaseMigrationTest {

    companion object {
        private const val TEST_DB = "utilities-test"
    }

    @Rule
    @JvmField
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        UtilitiesDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Sanity check for all migrations.
     */
    @Test
    fun migrateAll() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.close()

        val utilitiesDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UtilitiesDatabase::class.java,
            TEST_DB
        )
            .allowMainThreadQueries()
            .addMigrations(
                UtilitiesDatabase.MIGRATION_1_2,
                UtilitiesDatabase.MIGRATION_2_3,
                UtilitiesDatabase.MIGRATION_3_4,
                UtilitiesDatabase.MIGRATION_4_5,
                UtilitiesDatabase.MIGRATION_5_6
            )
            .build()
        utilitiesDatabase.openHelper.writableDatabase
        utilitiesDatabase.close()
    }

    /**
     * Version 5 migrate to 6 test - test password are migrated without problem
     */
    @Test
    @Suppress("ComplexMethod", "LongMethod", "StringLiteralDuplication")
    fun testMigrationFrom5To6() {
        val db: SupportSQLiteDatabase =
            helper.createDatabase(TEST_DB, 5)
        val password1 = PasswordUtil.encryptPassword(
            InstrumentationRegistry.getInstrumentation().targetContext,
            "passw0rd",
            Base64.DEFAULT
        )
        val password2 = PasswordUtil.encryptPassword(
            InstrumentationRegistry.getInstrumentation().targetContext,
            "\\password/%&*()",
            Base64.DEFAULT
        )
        db.execSQL(
            "INSERT INTO $TABLE_SMB ($COLUMN_NAME, $COLUMN_PATH) " +
                "VALUES ('test', 'smb://user:$password1@127.0.0.1/user')"
        )
        db.execSQL(
            "INSERT INTO $TABLE_SMB ($COLUMN_NAME, $COLUMN_PATH) " +
                "VALUES ('test anonymous', 'smb://127.0.0.1/Public')"
        )
        db.execSQL(
            "INSERT INTO $TABLE_SFTP ($COLUMN_NAME, $COLUMN_PATH, $COLUMN_HOST_PUBKEY) " +
                "VALUES ('test password', 'ssh://user:$password2@10.0.0.1', '12345678')"
        )
        db.execSQL(
            "INSERT INTO $TABLE_SFTP ($COLUMN_NAME, $COLUMN_PATH, $COLUMN_HOST_PUBKEY, " +
                "$COLUMN_PRIVATE_KEY_NAME, $COLUMN_PRIVATE_KEY) " +
                "VALUES ('test no password', 'ssh://user@10.0.0.2', '1234'," +
                " 'test private key', 'abcd')"
        )
        db.close()

        val utilitiesDatabase = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UtilitiesDatabase::class.java,
            TEST_DB
        )
            .addMigrations(UtilitiesDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        utilitiesDatabase.openHelper.writableDatabase
        val smbEntries = utilitiesDatabase.smbEntryDao().list().blockingGet()
        smbEntries.find { it.name == "test" }?.run {
            assertEquals(
                "smb://user:passw0rd@127.0.0.1/user",
                SmbUtil.getSmbDecryptedPath(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    this.path
                )
            )
        }
//        smbEntries.find { it.name == "test anonymous" }?.run {
//            assertEquals(
//                "smb://127.0.0.1/Public",
//                SmbUtil.getSmbDecryptedPath(
//                    InstrumentationRegistry.getInstrumentation().targetContext,
//                    this.path
//                )
//            )
//        }
//        val sftpEntries = utilitiesDatabase.sftpEntryDao().list().blockingGet()
//        sftpEntries.find { it.name == "test password" }?.run {
//            assertEquals(
//                "ssh://user:\\password/%&*()@10.0.0.1",
//                NetCopyClientUtils.decryptFtpPathAsNecessary(this.path)
//            )
//        } ?: fail("test password entry not found")
//        sftpEntries.find { it.name == "test no password" }?.run {
//            assertEquals(
//                "ssh://user@10.0.0.2",
//                NetCopyClientUtils.decryptFtpPathAsNecessary(this.path)
//            )
//        } ?: fail("test no password entry not found")
        utilitiesDatabase.close()
    }
}
