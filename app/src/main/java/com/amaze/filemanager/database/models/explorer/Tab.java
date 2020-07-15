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

package com.amaze.filemanager.database.models.explorer;

import com.amaze.filemanager.database.ExplorerDatabase;
import com.amaze.filemanager.filesystem.files.FileUtils;

import android.content.SharedPreferences;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Created by Vishal on 9/17/2014 */
@Entity(tableName = ExplorerDatabase.TABLE_TAB)
public class Tab {

  @PrimaryKey
  @ColumnInfo(name = ExplorerDatabase.COLUMN_TAB_NO)
  public final int tabNumber;

  @ColumnInfo(name = ExplorerDatabase.COLUMN_PATH)
  public final String path;

  @ColumnInfo(name = ExplorerDatabase.COLUMN_HOME)
  public final String home;

  public Tab(int tabNumber, String path, String home) {
    this.tabNumber = tabNumber;
    this.path = path;
    this.home = home;
  }

  public String getOriginalPath(boolean savePaths, SharedPreferences sharedPreferences) {
    if (savePaths && FileUtils.isPathAccessible(path, sharedPreferences)) {
      return path;
    } else {
      return home;
    }
  }
}
