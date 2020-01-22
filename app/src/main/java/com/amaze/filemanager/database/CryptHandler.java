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
