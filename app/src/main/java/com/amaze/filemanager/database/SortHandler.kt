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

import android.content.Context
import androidx.preference.PreferenceManager
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.daos.SortDao
import com.amaze.filemanager.database.models.explorer.Sort
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory

/** Created by Ning on 5/28/2018.  */
object SortHandler {

    private val LOG = LoggerFactory.getLogger(SortHandler::class.java)

    private val sortDao: SortDao by lazy {
        AppConfig.getInstance().explorerDatabase.sortDao()
    }

    @JvmStatic
    fun getSortType(context: Context, path: String?): Int {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val onlyThisFolders = sharedPref.getStringSet(PREFERENCE_SORTBY_ONLY_THIS, HashSet())
        val onlyThis = onlyThisFolders?.contains(path)
        val globalSortBy = sharedPref.getString("sortby", "0")?.toInt() ?: 0
        if (onlyThis == false) {
            return globalSortBy
        }
        val sort = findEntry(path)
        return sort?.type ?: globalSortBy
    }

    @JvmStatic
    fun addEntry(sort: Sort?) {
        sortDao.insert(sort).subscribeOn(Schedulers.io()).subscribe()
    }

    @JvmStatic
    fun clear(path: String?) {
        sortDao.clear(path).subscribeOn(Schedulers.io()).subscribe()
    }

    @JvmStatic
    fun updateEntry(newSort: Sort?) {
        sortDao.update(newSort).subscribeOn(Schedulers.io()).subscribe()
    }

    @JvmStatic
    fun findEntry(path: String?): Sort? {
        return try {
            sortDao.find(path).subscribeOn(Schedulers.io()).blockingGet()
        } catch (e: Exception) {
            // catch error to handle Single#onError for blockingGet
            LOG.error(javaClass.simpleName, e)
            null
        }
    }

}