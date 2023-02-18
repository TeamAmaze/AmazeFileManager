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

package com.amaze.filemanager.asynchronous.asynctasks.searchfilesystem

import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.asynchronous.asynctasks.Task
import com.amaze.filemanager.filesystem.files.FileListSorter
import com.amaze.filemanager.ui.fragments.MainFragment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SortSearchResultTask(
    val elements: MutableList<LayoutElementParcelable>,
    val sorter: FileListSorter,
    val mainFragment: MainFragment,
    val query: String
) : Task<Unit, SortSearchResultCallable> {

    private val log: Logger = LoggerFactory.getLogger(SortSearchResultTask::class.java)

    private val task = SortSearchResultCallable(elements, sorter)

    override fun getTask(): SortSearchResultCallable = task

    override fun onError(error: Throwable) {
        log.error("Could not sort search results because of exception", error)
    }

    override fun onFinish(value: Unit) {
        val mainFragmentViewModel = mainFragment.mainFragmentViewModel

        if (mainFragmentViewModel == null) {
            log.error(
                "Could not show sorted search results because main fragment view model is null"
            )
            return
        }

        val mainActivity = mainFragment.mainActivity

        if (mainActivity == null) {
            log.error("Could not show sorted search results because main activity is null")
            return
        }

        mainFragment.reloadListElements(
            true,
            true,
            !mainFragmentViewModel.isList
        ) // TODO: 7/7/2017 this is really inneffient, use RecycleAdapter's

        // createHeaders()
        mainActivity.appbar.bottomBar.setPathText("")
        mainActivity
            .appbar
            .bottomBar.fullPathText = mainActivity.getString(R.string.search_results, query)
        mainFragmentViewModel.results = false
    }
}
