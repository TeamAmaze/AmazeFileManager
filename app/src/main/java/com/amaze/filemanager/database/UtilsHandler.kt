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

package com.amaze.filemanager.database

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.R
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.database.models.utilities.Bookmark
import com.amaze.filemanager.database.models.utilities.Grid
import com.amaze.filemanager.database.models.utilities.Hidden
import com.amaze.filemanager.database.models.utilities.History
import com.amaze.filemanager.database.models.utilities.SftpEntry
import com.amaze.filemanager.database.models.utilities.SmbEntry
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue
import io.reactivex.schedulers.Schedulers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*

/**
 * Created by Vishal on 29-05-2017. Class handles database with tables having list of various
 * utilities like history, hidden files, list paths, grid paths, bookmarks, SMB entry
 *
 *
 * Try to use these functions from a background thread
 */
class UtilsHandler(
    private val context: Context,
    private val utilitiesDatabase: UtilitiesDatabase
) {

    private val log: Logger = LoggerFactory.getLogger(UtilsHandler::class.java)

    enum class Operation {
        HISTORY, HIDDEN, LIST, GRID, BOOKMARKS, SMB, SFTP
    }

    /**
     * Main save method.
     */
    @Suppress("ComplexMethod", "LongMethod")
    fun saveToDatabase(operationData: OperationData) {
        when (operationData.type) {
            Operation.HIDDEN ->
                utilitiesDatabase
                    .hiddenEntryDao()
                    .insert(Hidden(operationData.path))
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.HISTORY ->
                utilitiesDatabase.historyEntryDao().run {
                    deleteByPath(operationData.path)
                        .andThen(insert(History(operationData.path)))
                        .subscribeOn(Schedulers.io())
                        .subscribe()
                }
            Operation.LIST ->
                utilitiesDatabase
                    .listEntryDao()
                    .insert(
                        com.amaze.filemanager.database.models.utilities.List(operationData.path)
                    )
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.GRID ->
                utilitiesDatabase
                    .gridEntryDao()
                    .insert(Grid(operationData.path))
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.BOOKMARKS ->
                utilitiesDatabase
                    .bookmarkEntryDao()
                    .insert(Bookmark(operationData.name, operationData.path))
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.SMB ->
                utilitiesDatabase.smbEntryDao().run {
                    deleteByNameAndPath(operationData.name, operationData.path)
                        .andThen(insert(SmbEntry(operationData.name, operationData.path)))
                        .subscribeOn(Schedulers.io())
                        .subscribe()
                }
            Operation.SFTP ->
                utilitiesDatabase
                    .sftpEntryDao().run {
                        deleteByNameAndPath(operationData.name, operationData.path)
                            .andThen(
                                insert(
                                    SftpEntry(
                                        operationData.path,
                                        operationData.name,
                                        operationData.hostKey,
                                        operationData.sshKeyName,
                                        operationData.sshKey
                                    )
                                )
                            )
                            .subscribeOn(Schedulers.io())
                            .subscribe()
                    }

            else -> throw IllegalStateException("Unidentified operation!")
        }
    }

    /**
     * Main delete method.
     */
    fun removeFromDatabase(operationData: OperationData) {
        when (operationData.type) {
            Operation.HIDDEN ->
                utilitiesDatabase
                    .hiddenEntryDao()
                    .deleteByPath(operationData.path)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.HISTORY ->
                utilitiesDatabase
                    .historyEntryDao()
                    .deleteByPath(operationData.path)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.LIST ->
                utilitiesDatabase
                    .listEntryDao()
                    .deleteByPath(operationData.path)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.GRID ->
                utilitiesDatabase
                    .gridEntryDao()
                    .deleteByPath(operationData.path)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            Operation.BOOKMARKS -> removeBookmarksPath(operationData.name, operationData.path)
            Operation.SMB -> removeSmbPath(operationData.name, operationData.path)
            Operation.SFTP -> removeSftpPath(operationData.name, operationData.path)
            else -> throw IllegalStateException("Unidentified operation!")
        }
    }

    /**
     * Create common bookmarks to database.
     */
    fun addCommonBookmarks() {
        val sd = Environment.getExternalStorageDirectory()
        val dirs = arrayOf(
            File(sd, Environment.DIRECTORY_DCIM).absolutePath,
            File(sd, Environment.DIRECTORY_DOWNLOADS).absolutePath,
            File(sd, Environment.DIRECTORY_MOVIES).absolutePath,
            File(sd, Environment.DIRECTORY_MUSIC).absolutePath,
            File(sd, Environment.DIRECTORY_PICTURES).absolutePath
        )
        for (dir in dirs) {
            saveToDatabase(OperationData(Operation.BOOKMARKS, File(dir).name, dir))
        }
    }

    /**
     * Update SSH connection entry.
     */
    @Suppress("ComplexMethod", "LongParameterList")
    fun updateSsh(
        connectionName: String,
        oldConnectionName: String,
        path: String,
        hostKey: String?,
        sshKeyName: String?,
        sshKey: String?
    ) {
        utilitiesDatabase
            .sftpEntryDao()
            .findByName(oldConnectionName)
            .subscribeOn(Schedulers.io())
            .subscribe { entry: SftpEntry ->
                entry.name = connectionName
                entry.path = path
                entry.hostKey = hostKey
                if (sshKeyName != null && sshKey != null) {
                    entry.sshKeyName = sshKeyName
                    entry.sshKey = sshKey
                }
                utilitiesDatabase
                    .sftpEntryDao()
                    .update(entry)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }
    }

    /**
     * Get browse history in [LinkedList].
     */
    val historyLinkedList: LinkedList<String>
        get() {
            val paths = LinkedList<String>()
            for (
                history in utilitiesDatabase.historyEntryDao().list().subscribeOn(Schedulers.io())
                    .blockingGet()
            ) {
                paths.add(history.path)
            }
            return paths
        }

    /**
     * Return list of Hidden values
     */
    val hiddenFilesConcurrentRadixTree: ConcurrentRadixTree<VoidValue>
        get() {
            val paths = ConcurrentRadixTree<VoidValue>(DefaultCharArrayNodeFactory())
            for (
                path in utilitiesDatabase.hiddenEntryDao().listPaths().subscribeOn(Schedulers.io())
                    .blockingGet()
            ) {
                paths.put(path, VoidValue.SINGLETON)
            }
            return paths
        }

    /**
     * Return list of paths using list view.
     */
    val listViewList: ArrayList<String>
        get() = ArrayList(
            utilitiesDatabase.listEntryDao().listPaths().subscribeOn(Schedulers.io()).blockingGet()
        )

    /**
     * Return list of paths using grid view.
     */
    val gridViewList: ArrayList<String>
        get() = ArrayList(
            utilitiesDatabase.gridEntryDao().listPaths().subscribeOn(Schedulers.io()).blockingGet()
        )

    /**
     * Return list of bookmarks.
     */
    val bookmarksList: ArrayList<Array<String>>
        get() {
            val row = ArrayList<Array<String>>()
            for (
                bookmark in utilitiesDatabase.bookmarkEntryDao().list()
                    .subscribeOn(Schedulers.io()).blockingGet()
            ) {
                row.add(arrayOf(bookmark.name, bookmark.path))
            }
            return row
        }

    /**
     * Return list of SMB connections in name/URI pairs.
     */
    val smbList: ArrayList<Array<String>>
        get() {
            val retval = ArrayList<Array<String>>()
            for (
                entry in utilitiesDatabase.smbEntryDao().list().subscribeOn(Schedulers.io())
                    .blockingGet()
            ) {
                try {
                    retval.add(arrayOf(entry.name, entry.path))
                } catch (e: GeneralSecurityException) {
                    log.warn("failed to decrypt smb list path", e)

                    // failing to decrypt the path, removing entry from database
                    Toast.makeText(
                        context,
                        context.getString(R.string.failed_smb_decrypt_path),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    removeSmbPath(entry.name, "")
                    continue
                } catch (e: IOException) {
                    log.warn("failed to decrypt smb list path", e)
                    Toast.makeText(
                        context,
                        context.getString(R.string.failed_smb_decrypt_path),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    removeSmbPath(entry.name, "")
                    continue
                }
            }
            return retval
        }

    /**
     * Return SSH connections in name/URI pairs.
     */
    val sftpList: List<Array<String>>
        get() {
            val retval = ArrayList<Array<String>>()
            for (
                entry in utilitiesDatabase.sftpEntryDao().list().subscribeOn(Schedulers.io())
                    .blockingGet()
            ) {
                val path = entry.path
                if (path == null) {
                    log.error("Error decrypting path: " + entry.path)
                    // failing to decrypt the path, removing entry from database
                    Toast.makeText(
                        context,
                        context.getString(R.string.failed_smb_decrypt_path),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    retval.add(arrayOf(entry.name, path))
                }
            }
            return retval
        }

    /**
     * Returns SSH host key of specified URI.
     */
    fun getRemoteHostKey(uri: String): String? =
        runCatching {
            utilitiesDatabase
                .sftpEntryDao()
                .getRemoteHostKey(uri)
                .subscribeOn(Schedulers.io())
                .blockingGet()
        }.onFailure {
            if (BuildConfig.DEBUG) {
                log.warn("Error getting public key for URI [$uri]", it)
            }
        }.getOrNull()

    /**
     * Returns name of SSH private key of specified URI.
     */
    fun getSshAuthPrivateKeyName(uri: String): String? =
        runCatching {
            utilitiesDatabase
                .sftpEntryDao()
                .getSshAuthPrivateKeyName(uri)
                .subscribeOn(Schedulers.io())
                .blockingGet()
        }.onFailure {
            // catch error to handle Single#onError for blockingGet
            log.error("Error getting SSH private key name", it)
        }.getOrNull()

    /**
     * Returns private key of specified SSH URI.
     */
    fun getSshAuthPrivateKey(uri: String): String? =
        runCatching {
            utilitiesDatabase
                .sftpEntryDao()
                .getSshAuthPrivateKey(uri)
                .subscribeOn(Schedulers.io())
                .blockingGet()
        }.onFailure {
            // catch error to handle Single#onError for blockingGet
            if (BuildConfig.DEBUG) {
                log.error("Error getting auth private key for URI [$uri]", it)
            }
        }.getOrNull()

    private fun removeBookmarksPath(name: String, path: String) {
        utilitiesDatabase
            .bookmarkEntryDao()
            .deleteByNameAndPath(name, path)
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Remove SMB entry
     *
     * @param path the path we get from saved runtime variables is a decrypted, to remove entry, we
     * must encrypt it's password fiend first first
     */
    private fun removeSmbPath(name: String, path: String) {
        if ("" == path) {
            utilitiesDatabase.smbEntryDao().deleteByName(name)
                .subscribeOn(Schedulers.io()).subscribe()
        } else {
            utilitiesDatabase
                .smbEntryDao()
                .deleteByNameAndPath(name, path)
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    private fun removeSftpPath(name: String, path: String) {
        if ("" == path) {
            utilitiesDatabase.sftpEntryDao().deleteByName(name)
                .subscribeOn(Schedulers.io()).subscribe()
        } else {
            utilitiesDatabase
                .sftpEntryDao()
                .deleteByNameAndPath(name, path)
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    /**
     * Update [Bookmark].
     */
    fun renameBookmark(oldName: String, oldPath: String, newName: String, newPath: String) {
        val bookmark: Bookmark = kotlin.runCatching {
            utilitiesDatabase
                .bookmarkEntryDao()
                .findByNameAndPath(oldName, oldPath)
                .subscribeOn(Schedulers.io())
                .blockingGet()
        }.onFailure {
            // catch error to handle Single#onError for blockingGet
            log.error(it.message!!)
            return
        }.getOrThrow()

        bookmark.name = newName
        bookmark.path = newPath
        utilitiesDatabase.bookmarkEntryDao().update(bookmark).subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Update [SmbEntry].
     */
    fun renameSMB(oldName: String, oldPath: String, newName: String, newPath: String) {
        utilitiesDatabase
            .smbEntryDao()
            .findByNameAndPath(oldName, oldPath)
            .subscribeOn(Schedulers.io())
            .subscribe { smbEntry: SmbEntry ->
                smbEntry.name = newName
                smbEntry.path = newPath
                utilitiesDatabase
                    .smbEntryDao()
                    .update(smbEntry)
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }
    }

    /**
     * Clear specified table. Only supports [History] for now.
     */
    fun clearTable(table: Operation) {
        when (table) {
            Operation.HISTORY -> utilitiesDatabase.historyEntryDao().clear()
                .subscribeOn(Schedulers.io()).subscribe()
            else -> {}
        }
    }
}
