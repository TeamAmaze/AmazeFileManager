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

package com.amaze.filemanager.database;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.models.explorer.EncryptedEntry;

import androidx.annotation.NonNull;

/** Created by vishal on 15/4/17. */
public class CryptHandler {

  private final ExplorerDatabase database;

  private CryptHandler(@NonNull ExplorerDatabase explorerDatabase) {
    database = explorerDatabase;
  }

  private static class CryptHandlerHolder {
    private static final CryptHandler INSTANCE =
        new CryptHandler(AppConfig.getInstance().getExplorerDatabase());
  }

  public static CryptHandler getInstance() {
    return CryptHandlerHolder.INSTANCE;
  }

  public void addEntry(EncryptedEntry encryptedEntry) {
    AppConfig.getInstance()
        .runInBackground(() -> database.encryptedEntryDao().insert(encryptedEntry));
  }

  public void clear(String path) {
    AppConfig.getInstance().runInBackground(() -> database.encryptedEntryDao().delete(path));
  }

  public void updateEntry(EncryptedEntry oldEncryptedEntry, EncryptedEntry newEncryptedEntry) {
    AppConfig.getInstance()
        .runInBackground(() -> database.encryptedEntryDao().update(newEncryptedEntry));
  }

  public EncryptedEntry findEntry(String path) {
    return database.encryptedEntryDao().select(path);
  }

  public EncryptedEntry[] getAllEntries() {
    return database.encryptedEntryDao().list();
  }
}
