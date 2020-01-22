package com.amaze.filemanager.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.amaze.filemanager.database.models.utilities.SmbEntry;

import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_NAME;
import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PATH;
import static com.amaze.filemanager.database.UtilitiesDatabase.TABLE_SMB;

@Dao
public interface SmbEntryDao {

    @Insert
    public void insert(SmbEntry instance);

    @Update
    public void update(SmbEntry instance);

    @Query("SELECT * FROM " + TABLE_SMB)
    public SmbEntry[] list();

    @Query("SELECT * FROM " + TABLE_SMB + " WHERE " + COLUMN_NAME + " = :name AND " + COLUMN_PATH + " = :path")
    public SmbEntry findByNameAndPath(String name, String path);

    @Query("DELETE FROM " + TABLE_SMB + " WHERE " + COLUMN_NAME + " = :name")
    public void deleteByName(String name);

    @Query("DELETE FROM " + TABLE_SMB + " WHERE " + COLUMN_NAME + " = :name AND " + COLUMN_PATH + " = :path")
    public void deleteByNameAndPath(String name, String path);
}
