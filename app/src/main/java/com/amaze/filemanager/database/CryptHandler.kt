/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.database

import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.daos.EncryptedEntryDao
import com.amaze.filemanager.database.models.explorer.EncryptedEntry
import io.reactivex.schedulers.Schedulers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Created by vishal on 15/4/17.  */
object CryptHandler {

    private val log: Logger = LoggerFactory.getLogger(CryptHandler::class.java)

    private val encryptedEntryDao: EncryptedEntryDao by lazy {
        AppConfig.getInstance().explorerDatabase.encryptedEntryDao()
    }

    /**
     * Add [EncryptedEntry] to database.
     */
    @JvmStatic
    fun addEntry(encryptedEntry: EncryptedEntry) {
        encryptedEntryDao.insert(encryptedEntry).subscribeOn(Schedulers.io()).subscribe()
    }

    /**
     * Remove [EncryptedEntry] of specified path.
     */
    @JvmStatic
    fun clear(path: String) {
        encryptedEntryDao.delete(path).subscribeOn(Schedulers.io()).subscribe()
    }

    /**
     * Update specified new [EncryptedEntry] in database.
     */
    @JvmStatic
    fun updateEntry(newEncryptedEntry: EncryptedEntry) {
        encryptedEntryDao.update(newEncryptedEntry).subscribeOn(Schedulers.io()).subscribe()
    }

    /**
     * Find [EncryptedEntry] of specified path. Returns null if not exist.
     */
    @JvmStatic
    fun findEntry(path: String): EncryptedEntry? {
        return runCatching {
            encryptedEntryDao.select(path).subscribeOn(Schedulers.io()).blockingGet()
        }.onFailure {
            log.error(it.message!!)
        }.getOrNull()
    }

    @JvmStatic
    val allEntries: Array<EncryptedEntry>
        get() = encryptedEntryDao.list().subscribeOn(Schedulers.io()).blockingGet().toTypedArray()
}
