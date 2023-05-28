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

package com.amaze.filemanager.database

import android.annotation.SuppressLint
import android.content.Context
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.daos.CloudEntryDao
import com.amaze.filemanager.database.models.explorer.CloudEntry
import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.ui.fragments.CloudSheetFragment
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory

/** Created by vishal on 18/4/17.  */
@SuppressLint("CheckResult")
object CloudHandler {

    private val LOG = LoggerFactory.getLogger(CloudHandler::class.java)

    private val cloudEntryDao: CloudEntryDao by lazy {
        AppConfig.getInstance().explorerDatabase.cloudEntryDao()
    }

    @JvmStatic
    @Throws(CloudPluginException::class)
    fun addEntry(context: Context, cloudEntry: CloudEntry?) {
        if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw CloudPluginException()

        cloudEntryDao.insert(cloudEntry).subscribeOn(Schedulers.io()).subscribe()
    }

    @JvmStatic
    fun clear(serviceType: OpenMode) {
        cloudEntryDao
            .findByServiceType(serviceType.ordinal)
            .subscribeOn(Schedulers.io())
            .subscribe(
                ::deleteCloudEntry
            ) { throwable ->
                LOG.warn("failed to delete cloud connection", throwable)
            }
    }

    @JvmStatic
    @Throws(CloudPluginException::class)
    fun updateEntry(context: Context, serviceType: OpenMode?, newCloudEntry: CloudEntry) {
        if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw CloudPluginException()

        cloudEntryDao.update(newCloudEntry).subscribeOn(Schedulers.io()).subscribe()
    }

    @JvmStatic
    private fun deleteCloudEntry(cloudEntry: CloudEntry) {
        cloudEntryDao
            .delete(cloudEntry)
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    @JvmStatic
    fun clearAllCloudConnections() {
        cloudEntryDao.clear().subscribeOn(Schedulers.io()).blockingGet()
    }

    @JvmStatic
    @Throws(CloudPluginException::class)
    fun findEntry(context: Context, serviceType: OpenMode): CloudEntry? {
        if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw CloudPluginException()

        return try {
            cloudEntryDao
                .findByServiceType(serviceType.ordinal)
                .subscribeOn(Schedulers.io())
                .blockingGet()
        } catch (e: Exception) {
            // catch error to handle Single#onError for blockingGet
            LOG.error(javaClass.simpleName, e)
            null
        }
    }

    @JvmStatic
    @Throws(CloudPluginException::class)
    fun getAllEntries(context: Context): List<CloudEntry> {
        if (!CloudSheetFragment.isCloudProviderAvailable(context)) throw CloudPluginException()

        return cloudEntryDao.list().subscribeOn(Schedulers.io()).blockingGet()
    }


    const val CLOUD_PREFIX_BOX = "box:/"
    const val CLOUD_PREFIX_DROPBOX = "dropbox:/"
    const val CLOUD_PREFIX_GOOGLE_DRIVE = "gdrive:/"
    const val CLOUD_PREFIX_ONE_DRIVE = "onedrive:/"

    const val CLOUD_NAME_GOOGLE_DRIVE = "Google Drive™"
    const val CLOUD_NAME_DROPBOX = "Dropbox"
    const val CLOUD_NAME_ONE_DRIVE = "One Drive"
    const val CLOUD_NAME_BOX = "Box"

}