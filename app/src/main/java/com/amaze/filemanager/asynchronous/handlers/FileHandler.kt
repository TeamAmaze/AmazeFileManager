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

package com.amaze.filemanager.asynchronous.handlers

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.adapters.RecyclerAdapter
import com.amaze.filemanager.filesystem.CustomFileObserver
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.ui.fragments.MainFragment
import java.io.File
import java.lang.ref.WeakReference

class FileHandler(
    mainFragment: MainFragment,
    private val listView: RecyclerView,
    private val useThumbs: Boolean
) : Handler(
    Looper.getMainLooper()
) {
    private val mainFragment: WeakReference<MainFragment> = WeakReference(mainFragment)

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        val main = mainFragment.get() ?: return
        val mainFragmentViewModel = main.mainFragmentViewModel ?: return
        val elementsList = main.elementsList ?: return
        if (main.activity == null) {
            return
        }

        val path = msg.obj as String
        when (msg.what) {
            CustomFileObserver.GOBACK -> {
                main.goBack()
            }
            CustomFileObserver.NEW_ITEM -> {
                val fileCreated = HybridFile(
                    mainFragmentViewModel.openMode, "${main.currentPath}/$path"
                )
                val newElement = fileCreated.generateLayoutElement(main.requireContext(), useThumbs)
                main.elementsList?.add(newElement)
            }
            CustomFileObserver.DELETED_ITEM -> {
                val index = elementsList.withIndex().find {
                    File(it.value.desc).name == path
                }?.index

                if (index != null) {
                    main.elementsList?.removeAt(index)
                }
            }
            else -> {
                super.handleMessage(msg)
                return
            }
        }
        if (listView.visibility == View.VISIBLE) {
            if (elementsList.size == 0) {
                // no item left in list, recreate views
                main.reloadListElements(
                    true,
                    mainFragmentViewModel.results,
                    !mainFragmentViewModel.isList
                )
            } else {
                val itemList = main.elementsList ?: listOf()
                // we already have some elements in list view, invalidate the adapter
                (listView.adapter as RecyclerAdapter).setItems(listView, itemList)
            }
        } else {
            // there was no list view, means the directory was empty
            main.loadlist(main.currentPath, true, mainFragmentViewModel.openMode)
        }
        main.computeScroll()
    }
}
