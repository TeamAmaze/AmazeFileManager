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

import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.daos.TabDao
import com.amaze.filemanager.database.models.explorer.Tab
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory

/** Created by Vishal on 9/17/2014.  */
object TabHandler {

    private val LOG = LoggerFactory.getLogger(TabHandler::class.java)

    private val tabDao: TabDao by lazy {
        AppConfig.getInstance().explorerDatabase.tabDao()
    }

    @JvmStatic
    fun addTab(tab: Tab) {
        tabDao.insertTab(tab).subscribeOn(Schedulers.io()).blockingAwait()
    }

    @JvmStatic
    fun update(tab: Tab) {
        tabDao.update(tab).subscribeOn(Schedulers.io()).subscribe()
    }

    @JvmStatic
    fun clear(): Completable {
        return tabDao.clear().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    @JvmStatic
    fun findTab(tabNo: Int): Tab? {
        return try {
            tabDao.find(tabNo).subscribeOn(Schedulers.io()).blockingGet()
        } catch (e: Exception) {
            // catch error to handle Single#onError for blockingGet
            LOG.error(e.message)
            null
        }
    }

    @JvmStatic
    val allTabs: Array<Tab>
        get() = tabDao.list().subscribeOn(Schedulers.io()).blockingGet().toTypedArray()

}