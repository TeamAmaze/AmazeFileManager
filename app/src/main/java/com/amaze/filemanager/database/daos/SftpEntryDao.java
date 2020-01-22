package com.amaze.filemanager.database.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.amaze.filemanager.database.models.utilities.SftpEntry;

import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_HOST_PUBKEY;
import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_NAME;
import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PATH;
import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PRIVATE_KEY;
import static com.amaze.filemanager.database.UtilitiesDatabase.COLUMN_PRIVATE_KEY_NAME;
import static com.amaze.filemanager.database.UtilitiesDatabase.TABLE_SFTP;

@Dao
public interface SftpEntryDao {

    @Insert
    public void insert(SftpEntry instance);

    @Update
    public void update(SftpEntry instance);

    @Query("SELECT * FROM " + TABLE_SFTP)
    public SftpEntry[] list();

    @Query("SELECT * FROM " + TABLE_SFTP + " WHERE " + COLUMN_NAME + " = :name AND " + COLUMN_PATH + " = :path")
    public SftpEntry findByNameAndPath(String name, String path);

    @Query("SELECT * FROM " + TABLE_SFTP + " WHERE " + COLUMN_NAME + " = :name")
    public SftpEntry findByName(String name);

    @Query("SELECT " + COLUMN_HOST_PUBKEY + " FROM " + TABLE_SFTP + " WHERE PATH = :uri")
    public String getSshHostKey(String uri);

    @Query("SELECT " + COLUMN_PRIVATE_KEY_NAME + " FROM " + TABLE_SFTP + " WHERE PATH = :uri")
    public String getSshAuthPrivateKeyName(String uri);

    @Query("SELECT " + COLUMN_PRIVATE_KEY + " FROM " + TABLE_SFTP + " WHERE PATH = :uri")
    public String getSshAuthPrivateKey(String uri);

    @Query("DELETE FROM " + TABLE_SFTP + " WHERE " + COLUMN_NAME + " = :name")
    public void deleteByName(String name);

    @Query("DELETE FROM " + TABLE_SFTP + " WHERE " + COLUMN_NAME + " = :name AND " + COLUMN_PATH + " = :path")
    public void deleteByNameAndPath(String name, String path);
}
