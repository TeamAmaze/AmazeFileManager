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
package com.amaze.filemanager.adapters.glide

import android.util.SparseArray
import android.view.View
import com.amaze.filemanager.adapters.glide.RecyclerPreloadSizeProvider.RecyclerPreloadSizeProviderCallback
import com.bumptech.glide.ListPreloader.PreloadSizeProvider
import com.amaze.filemanager.adapters.data.IconDataParcelable
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition

/**
 * This uses a callback to know for each position what View is the one in which you're going to
 * insert the image.
 */
class RecyclerPreloadSizeProvider(private val callback: RecyclerPreloadSizeProviderCallback) :
    PreloadSizeProvider<IconDataParcelable> {
    private val viewSizes = SparseArray<IntArray?>()
    private var isAdditionClosed = false

    /**
     * Adds one of the views that can be used to put an image inside. If the id is already inserted
     * the call will be ignored, but for performance you should call [.closeOffAddition] once
     * you are done.
     *
     * @param id a unique number for each view loaded to this object
     * @param v the ciew to load
     */
    fun addView(id: Int, v: View) {
        if (!isAdditionClosed && viewSizes[id, null] != null){
            return
        }

        SizeViewTarget(v) { width: Int, height: Int ->
            viewSizes.append(id, intArrayOf(width, height))
        }
    }

    /** Calls to [.addView] will be ignored  */
    fun closeOffAddition() {
        isAdditionClosed = true
    }

    override fun getPreloadSize(
        item: IconDataParcelable,
        adapterPosition: Int,
        perItemPosition: Int
    ): IntArray? {
        return viewSizes[callback.getCorrectView(item, adapterPosition), null]
    }

    interface RecyclerPreloadSizeProviderCallback {
        /**
         * Get the id for the view in which the image will be loaded.
         *
         * @return the view's id
         */
        fun getCorrectView(item: IconDataParcelable?, adapterPosition: Int): Int
    }

    private class SizeViewTarget(view: View, callback: SizeReadyCallback) : ViewTarget<View?, Any>(view) {
        override fun onResourceReady(resource: Any, transition: Transition<in Any>?) {
            // Do nothing
        }

        init {
            getSize(callback)
        }
    }
}