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

package com.amaze.filemanager.database.models.utilities;

import com.amaze.filemanager.database.UtilitiesDatabase;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Base class {@link Entity} representation of tables in utilities.db.
 *
 * <p>This class is the base classwith <code>id</code>, <code>path</code> columns common to all
 * tables.
 *
 * @see UtilitiesDatabase
 */
public abstract class OperationData {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = UtilitiesDatabase.COLUMN_ID)
  public int _id;

  @ColumnInfo(name = UtilitiesDatabase.COLUMN_PATH)
  public String path;

  public OperationData(@NonNull String path) {
    this.path = path;
  }

  @Override
  public String toString() {
    return new StringBuilder("OperationData type=[")
        .append(getClass().getSimpleName())
        .append("],path=[")
        .append(path)
        .append("]")
        .toString();
  }
}
