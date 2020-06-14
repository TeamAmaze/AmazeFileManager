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
import com.amaze.filemanager.database.models.StringWrapper;
import com.amaze.filemanager.database.typeconverters.EncryptedStringTypeConverter;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/** Created by vishal on 8/4/17. */
@Entity(tableName = ExplorerDatabase.TABLE_ENCRYPTED)
public class EncryptedEntry {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = ExplorerDatabase.COLUMN_ENCRYPTED_ID)
  private int _id;

  @ColumnInfo(name = ExplorerDatabase.COLUMN_ENCRYPTED_PATH)
  private String path;

  @ColumnInfo(name = ExplorerDatabase.COLUMN_ENCRYPTED_PASSWORD)
  @TypeConverters(EncryptedStringTypeConverter.class)
  private StringWrapper password;

  public EncryptedEntry() {}

  public EncryptedEntry(String path, String unencryptedPassword) {
    this.path = path;
    this.password = new StringWrapper(unencryptedPassword);
  }

  public void setId(int _id) {
    this._id = _id;
  }

  public int getId() {
    return this._id;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return this.path;
  }

  public void setPassword(StringWrapper password) {
    this.password = password;
  }

  public StringWrapper getPassword() {
    return this.password;
  }
}
