/*
 * EncryptedEntryDao.java
 *
 * Copyright (C) 2020 Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.amaze.filemanager.database.models.explorer.EncryptedEntry;

@Dao
public interface EncryptedEntryDao {

    @Insert
    public void insert(EncryptedEntry entry);

    @Query("SELECT * FROM encrypted WHERE path = :path")
    public EncryptedEntry select(String path);

    @Update
    public void update(EncryptedEntry entry);

    @Delete
    public void delete(EncryptedEntry entry);

    @Query("SELECT * FROM encrypted")
    public EncryptedEntry[] list();
}
