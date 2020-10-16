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

import java.util.List;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.models.explorer.EncryptedEntry;

import android.util.Log;

import androidx.annotation.NonNull;

import io.reactivex.schedulers.Schedulers;

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
    database.encryptedEntryDao().insert(encryptedEntry).subscribeOn(Schedulers.io()).subscribe();
  }

  public void clear(String path) {
    database.encryptedEntryDao().delete(path).subscribeOn(Schedulers.io()).subscribe();
  }

  public void updateEntry(EncryptedEntry oldEncryptedEntry, EncryptedEntry newEncryptedEntry) {
    database.encryptedEntryDao().update(newEncryptedEntry).subscribeOn(Schedulers.io()).subscribe();
  }

  public EncryptedEntry findEntry(String path) {
    try {
      return database.encryptedEntryDao().select(path).subscribeOn(Schedulers.io()).blockingGet();
    } catch (Exception e) {
      // catch error to handle Single#onError for blockingGet
      Log.e(getClass().getSimpleName(), e.getMessage());
      return null;
    }
  }

  public EncryptedEntry[] getAllEntries() {
    List<EncryptedEntry> encryptedEntryList =
        database.encryptedEntryDao().list().subscribeOn(Schedulers.io()).blockingGet();
    EncryptedEntry[] encryptedEntries = new EncryptedEntry[encryptedEntryList.size()];
    return encryptedEntryList.toArray(encryptedEntries);
  }
}
