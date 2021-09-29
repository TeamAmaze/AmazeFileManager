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

import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.amaze.filemanager.adapters.RecyclerAdapter
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.adapters.holders.ItemViewHolder
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.dialogs.DragAndDropDialog
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import com.amaze.filemanager.utils.DataUtils
import kotlin.collections.ArrayList

class RecyclerAdapterDragListener(
    private val adapter: RecyclerAdapter,
    private val holder: ItemViewHolder?,
    private val dragAndDropPref: Int,
    private val mainFragment: MainFragment
) : View.OnDragListener {

    private val TAG = javaClass.simpleName

    override fun onDrag(p0: View?, p1: DragEvent?): Boolean {
        return when (p1?.action) {
            DragEvent.ACTION_DRAG_ENDED -> {
                Log.d(TAG, "ENDING DRAG, DISABLE CORNERS")
                mainFragment.requireMainActivity().initCornersDragListener(
                    true,
                    dragAndDropPref
                        != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT
                )
                if (dragAndDropPref
                    != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT
                ) {
                    val dataUtils = DataUtils.getInstance()
                    dataUtils.checkedItemsList = null
                    mainFragment.requireMainActivity()
                        .tabFragment.dragPlaceholder?.visibility = View.INVISIBLE
                }
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                holder?.run {
                    if (adapter.itemsDigested.size != 0 &&
                        holder.adapterPosition < adapter.itemsDigested.size
                    ) {
                        val listItem = (adapter.itemsDigested[holder.adapterPosition])
                        if (dragAndDropPref == PreferencesConstants.PREFERENCE_DRAG_TO_SELECT) {
                            if (listItem.specialType != RecyclerAdapter.TYPE_BACK &&
                                listItem.shouldToggleDragChecked
                            ) {
                                listItem.toggleShouldToggleDragChecked()
                                adapter.toggleChecked(
                                    holder.adapterPosition,
                                    if (mainFragment.mainFragmentViewModel?.isList == true) {
                                        holder.checkImageView
                                    } else {
                                        holder.checkImageViewGrid
                                    }
                                )
                            }
                        } else {
                            val currentElement = listItem.elem
                            if (currentElement.isDirectory && listItem.specialType !=
                                RecyclerAdapter.TYPE_BACK
                            ) {
                                holder.rl.isSelected = true
                            }
                        }
                    }
                }
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                holder?.run {
                    if (adapter.itemsDigested.size != 0 &&
                        holder.adapterPosition < adapter.itemsDigested.size
                    ) {
                        val listItem = (adapter.itemsDigested[holder.adapterPosition])
                        if (dragAndDropPref != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT) {
                            val checkedItems:
                                ArrayList<LayoutElementParcelable> = adapter.checkedItems
                            val currentElement = listItem.elem
                            if (currentElement.isDirectory && listItem.specialType !=
                                RecyclerAdapter.TYPE_BACK &&
                                !checkedItems.contains(currentElement)
                            ) {
                                holder.rl.run {
                                    isSelected = false
                                    isFocusable = false
                                    isFocusableInTouchMode = false
                                    clearFocus()
                                }
                            }
                        }
                    }
                }
                true
            }
            DragEvent.ACTION_DRAG_STARTED -> {
                return true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                holder?.run {
                    if (dragAndDropPref != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT) {
                        holder.rl.run {
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()
                        }
                    }
                }
                true
            }
            DragEvent.ACTION_DROP -> {
                if (dragAndDropPref != PreferencesConstants.PREFERENCE_DRAG_TO_SELECT) {
                    var checkedItems: ArrayList<LayoutElementParcelable> = adapter.checkedItems
                    var currentFileParcelable: HybridFileParcelable? = null
                    var isCurrentElementDirectory: Boolean? = null
                    var isEmptyArea: Boolean? = null
                    var pasteLocation: String? = if (adapter.itemsDigested.size == 0) {
                        mainFragment.currentPath
                    } else {
                        if (holder == null || holder.adapterPosition == RecyclerView.NO_POSITION) {
                            Log.d(TAG, "Trying to drop into empty area")
                            isEmptyArea = true
                            mainFragment.currentPath
                        } else {
                            if (adapter.itemsDigested[holder.adapterPosition].specialType
                                == RecyclerAdapter.TYPE_BACK
                            ) {
                                // dropping in goback button
                                // hack to get the parent path
                                val hybridFileParcelable = mainFragment
                                    .elementsList!!.get(1).generateBaseFile()
                                val hybridFile = HybridFile(
                                    hybridFileParcelable.mode,
                                    hybridFileParcelable.getParent(mainFragment.context)
                                )
                                hybridFile.getParent(mainFragment.context)
                            } else {
                                val currentElement = adapter
                                    .itemsDigested[holder.adapterPosition].elem
                                currentFileParcelable = currentElement.generateBaseFile()
                                isCurrentElementDirectory = currentElement.isDirectory
                                currentElement.desc
                            }
                        }
                    }
                    if (checkedItems.size == 0) {
                        // probably because we switched tabs and
                        // this adapter doesn't have any checked items, get from data utils
                        val dataUtils = DataUtils.getInstance()
                        Log.d(
                            TAG,
                            "Didn't find checked items in adapter, " +
                                "checking dataUtils size ${
                                dataUtils.checkedItemsList?.size ?: "null"}"
                        )
                        checkedItems = dataUtils.checkedItemsList
                    }
                    val arrayList = ArrayList<HybridFileParcelable>(checkedItems.size)
                    checkedItems.forEach {
                        val file = it.generateBaseFile()
                        if (it.desc.equals(pasteLocation) ||
                            (
                                (
                                    isCurrentElementDirectory == false &&
                                        currentFileParcelable?.getParent(mainFragment.context)
                                            .equals(file.getParent(mainFragment.context))
                                    ) ||
                                    (
                                        isEmptyArea == true && mainFragment.currentPath
                                            .equals(file.getParent(mainFragment.context))
                                        )
                                )
                        ) {
                            Log.d(
                                TAG,
                                (
                                    "Trying to drop into one of checked items or current " +
                                        "location, not allowed ${it.desc}"
                                    )
                            )
                            holder?.rl?.run {
                                isFocusable = false
                                isFocusableInTouchMode = false
                                clearFocus()
                            }
                            return false
                        }
                        arrayList.add(it.generateBaseFile())
                    }
                    if (isCurrentElementDirectory == false || isEmptyArea == true) {
                        pasteLocation = mainFragment.currentPath
                    }
                    Log.d(
                        TAG,
                        (
                            "Trying to drop into one of checked items " +
                                "%s"
                            ).format(pasteLocation)
                    )
                    DragAndDropDialog.showDialogOrPerformOperation(
                        pasteLocation!!,
                        arrayList, mainFragment.requireMainActivity()
                    )
                    adapter.toggleChecked(false)
                    holder?.rl?.run {
                        isSelected = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        clearFocus()
                    }
                }
                true
            }
            else -> false
        }
    }
}
