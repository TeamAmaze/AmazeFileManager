package com.amaze.filemanager.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.amaze.filemanager.database.models.utilities.Hidden;

import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PATH;
import static com.amaze.filemanager.database.UtilitiesDatabase.TABLE_HIDDEN;

@Dao
public interface HiddenEntryDao {

    @Insert
    public void insert(Hidden instance);

    @Update
    public void update(Hidden instance);

    @Query("SELECT " + COLUMN_PATH + " FROM " + TABLE_HIDDEN)
    public String[] listPaths();

    @Query("DELETE FROM " + TABLE_HIDDEN + " WHERE " + COLUMN_PATH + " = :path")
    public void deleteByPath(String path);
}
