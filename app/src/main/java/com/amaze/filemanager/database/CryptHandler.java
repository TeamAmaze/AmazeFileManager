/*
 * CryptHandler.java
 *
 * Copyright (C) 2017-2020 Vishal Nehra <vishalmeham2@gmail.com>,
 * John Carlson <jawnnypoo@gmail.com>, Emmanuel Messulam<emmanuelbendavid@gmail.com>,
 * Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

import com.amaze.filemanager.database.models.explorer.EncryptedEntry;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vishal on 15/4/17.
 */

public class CryptHandler {

    private final ExplorerDatabase database;

    public CryptHandler() {
        database = ExplorerDatabase.getInstance();
    }

    public void addEntry(EncryptedEntry encryptedEntry) {
        database.encryptedEntryDao().insert(encryptedEntry);
    }

    public void clear(String path) {
        database.encryptedEntryDao().delete(database.encryptedEntryDao().select(path));
    }

    public void updateEntry(EncryptedEntry oldEncryptedEntry, EncryptedEntry newEncryptedEntry) {
        database.encryptedEntryDao().update(newEncryptedEntry);
    }

    public EncryptedEntry findEntry(String path) {
        return database.encryptedEntryDao().select(path);
    }

    public List<EncryptedEntry> getAllEntries()  {
        return Arrays.asList(database.encryptedEntryDao().list());
    }
}
