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

package com.amaze.filemanager.database;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static com.amaze.filemanager.database.ExplorerDatabase.MIGRATION_1_2;
import static com.amaze.filemanager.database.ExplorerDatabase.MIGRATION_2_3;
import static com.amaze.filemanager.database.ExplorerDatabase.MIGRATION_3_4;
import static com.amaze.filemanager.database.ExplorerDatabase.MIGRATION_4_5;
import static com.amaze.filemanager.database.ExplorerDatabase.MIGRATION_5_6;
import static com.amaze.filemanager.database.ExplorerDatabase.MIGRATION_6_7;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.shadows.ShadowMultiDex;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class ExplorerDatabaseMigrationTest {

  private static final String TEST_DB = "explorer-test";

  @Rule
  public final MigrationTestHelper helper =
      new MigrationTestHelper(
          InstrumentationRegistry.getInstrumentation(),
          ExplorerDatabase.class.getCanonicalName(),
          new FrameworkSQLiteOpenHelperFactory());

  @Test
  public void migrateAll() throws IOException {
    SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
    db.close();

    ExplorerDatabase explorerDatabase =
        Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                ExplorerDatabase.class,
                TEST_DB)
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7)
            .build();
    explorerDatabase.getOpenHelper().getWritableDatabase();
    explorerDatabase.close();
  }

  @Test
  public void migrateFromV5() throws IOException {
    SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 5);
    db.close();

    ExplorerDatabase explorerDatabase =
        Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                ExplorerDatabase.class,
                TEST_DB)
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .build();
    explorerDatabase.getOpenHelper().getWritableDatabase();
    explorerDatabase.close();
  }

  @Test
  public void migrateFromV6() throws IOException {
    SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 6);
    db.close();

    ExplorerDatabase explorerDatabase =
        Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                ExplorerDatabase.class,
                TEST_DB)
            .addMigrations(MIGRATION_6_7)
            .build();
    explorerDatabase.getOpenHelper().getWritableDatabase();
    explorerDatabase.close();
  }
}
