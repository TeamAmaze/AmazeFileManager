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

import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_NAME;
import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PATH;
import static com.amaze.filemanager.database.UtilitiesDatabase.TABLE_BOOKMARKS;

import com.amaze.filemanager.database.UtilitiesDatabase;

import androidx.room.Entity;
import androidx.room.Index;

/**
 * {@link Entity} representation of <code>bookmark</code> table in utilities.db.
 *
 * @see UtilitiesDatabase
 */
@Entity(
    tableName = TABLE_BOOKMARKS,
    indices = {
      @Index(
          name = TABLE_BOOKMARKS + "_idx",
          value = {COLUMN_NAME, COLUMN_PATH},
          unique = true)
    })
public class Bookmark extends OperationDataWithName {
  public Bookmark(String name, String path) {
    super(name, path);
  }
}
