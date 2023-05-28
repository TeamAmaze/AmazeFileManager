/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>,
 * Oleksandr Narvatov <hipi96222@gmail.com> and Contributors.
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

package com.amaze.filemanager.database.daos

import androidx.room.*
import com.amaze.filemanager.database.models.utilities.SftpEntry
import io.reactivex.Completable
import com.amaze.filemanager.database.UtilitiesDatabase
import io.reactivex.Single

/**
 * [Dao] interface definition for [SftpEntry]. Concrete class is generated by Room
 * during build.
 *
 * @see Dao
 *
 * @see SftpEntry
 *
 * @see com.amaze.filemanager.database.UtilitiesDatabase
 */
@Dao
interface SftpEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(instance: SftpEntry): Completable

    @Update
    fun update(instance: SftpEntry): Completable

    @Query("SELECT * FROM " + UtilitiesDatabase.TABLE_SFTP)
    fun list(): Single<List<SftpEntry>>

    @Query(
        "SELECT * FROM "
                + UtilitiesDatabase.TABLE_SFTP
                + " WHERE "
                + UtilitiesDatabase.COLUMN_NAME
                + " = :name AND "
                + UtilitiesDatabase.COLUMN_PATH
                + " = :path"
    )
    fun findByNameAndPath(name: String, path: String): Single<SftpEntry>

    @Query("SELECT * FROM " + UtilitiesDatabase.TABLE_SFTP + " WHERE " + UtilitiesDatabase.COLUMN_NAME + " = :name")
    fun findByName(name: String): Single<SftpEntry>

    @Query("SELECT " + UtilitiesDatabase.COLUMN_HOST_PUBKEY + " FROM " + UtilitiesDatabase.TABLE_SFTP + " WHERE " + UtilitiesDatabase.COLUMN_PATH + " = :uri")
    fun getRemoteHostKey(uri: String): Single<String?>

    @Query(
        "SELECT "
                + UtilitiesDatabase.COLUMN_PRIVATE_KEY_NAME
                + " FROM "
                + UtilitiesDatabase.TABLE_SFTP
                + " WHERE "
                + UtilitiesDatabase.COLUMN_PATH
                + " = :uri"
    )
    fun getSshAuthPrivateKeyName(uri: String): Single<String?>

    @Query("SELECT " + UtilitiesDatabase.COLUMN_PRIVATE_KEY + " FROM " + UtilitiesDatabase.TABLE_SFTP + " WHERE " + UtilitiesDatabase.COLUMN_PATH + " = :uri")
    fun getSshAuthPrivateKey(uri: String): Single<String?>

    @Query("DELETE FROM " + UtilitiesDatabase.TABLE_SFTP + " WHERE " + UtilitiesDatabase.COLUMN_NAME + " = :name")
    fun deleteByName(name: String): Completable

    @Query(
        "DELETE FROM "
                + UtilitiesDatabase.TABLE_SFTP
                + " WHERE "
                + UtilitiesDatabase.COLUMN_NAME
                + " = :name AND "
                + UtilitiesDatabase.COLUMN_PATH
                + " = :path"
    )
    fun deleteByNameAndPath(name: String, path: String): Completable

}