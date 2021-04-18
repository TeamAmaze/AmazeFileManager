/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.drag

import android.os.AsyncTask
import android.util.Log
import android.view.DragEvent
import android.view.View
import com.amaze.filemanager.adapters.RecyclerAdapter
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.adapters.holders.ItemViewHolder
import com.amaze.filemanager.asynchronous.asynctasks.PrepareCopyTask
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import java.util.*

class RecyclerAdapterDragListener(
    private val adapter: RecyclerAdapter,
    private val holder: ItemViewHolder,
    private val dragAndDropPref: Int,
    private val mainFragment: MainFragment
) : View.OnDragListener {

    private val TAG = javaClass.simpleName

    override fun onDrag(p0: View?, p1: DragEvent?): Boolean {
        val checkedItems: ArrayList<LayoutElementParcelable> = adapter.checkedItems
        val listItem = (adapter.itemsDigested[holder.adapterPosition])
        val currentElement = adapter.itemsDigested[holder.adapterPosition].elem
        return when (p1?.action) {
            DragEvent.ACTION_DRAG_ENDED -> {
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                if (dragAndDropPref == PreferencesConstants.PREFERENCE_DRAG_TO_SELECT) {
                    if (listItem.specialType != RecyclerAdapter.TYPE_BACK &&
                        listItem.shouldToggleDragChecked
                    ) {
                        listItem.toggleShouldToggleDragChecked()
                        adapter.toggleChecked(
                            holder.adapterPosition,
                            if (mainFragment.IS_LIST) holder.checkImageView
                            else holder.checkImageViewGrid
                        )
                    }
                }
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                if (dragAndDropPref == PreferencesConstants.PREFERENCE_DRAG_TO_SELECT) {
                    listItem.toggleShouldToggleDragChecked()
                }
                true
            }
            DragEvent.ACTION_DRAG_STARTED -> {
                return true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                true
            }
            DragEvent.ACTION_DROP -> {
                if (dragAndDropPref != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT) {
                    if (!currentElement.isDirectory) {
                        Log.d(
                            TAG,
                            (
                                "Trying to drop into a non-directory, not allowed " +
                                    "%s"
                                ).format(currentElement.desc)
                        )
                        return false
                    }
                    val arrayList = ArrayList<HybridFileParcelable>(checkedItems.size)
                    checkedItems.forEach {
                        if (it.desc.equals(currentElement.desc)) {
                            Log.d(
                                TAG,
                                (
                                    "Trying to drop into one of checked items, not allowed " +
                                        "%s"
                                    ).format(it.desc)
                            )
                            return false
                        }
                        arrayList.add(it.generateBaseFile())
                    }
                    PrepareCopyTask(
                        mainFragment,
                        currentElement.desc,
                        dragAndDropPref == PreferencesConstants.PREFERENCE_DRAG_TO_MOVE,
                        mainFragment.mainActivity,
                        mainFragment.mainActivity.isRootExplorer
                    )
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, arrayList)
                    adapter.toggleChecked(false)
                }
                true
            }
            else -> false
        }
    }
}
