package com.amaze.filemanager.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.amaze.filemanager.database.models.utilities.Grid;

import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PATH;
import static com.amaze.filemanager.database.UtilitiesDatabase.TABLE_GRID;

@Dao
public interface GridEntryDao {

    @Insert
    public void insert(Grid instance);

    @Update
    public void update(Grid instance);

    @Query("SELECT " + COLUMN_PATH + " FROM " + TABLE_GRID)
    public String[] listPaths();

    @Query("DELETE FROM " + TABLE_GRID + " WHERE " + COLUMN_PATH + " = :path")
    public void deleteByPath(String path);
}
