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

import android.graphics.drawable.Drawable
import androidx.fragment.app.Fragment
import com.amaze.filemanager.GlideApp
import com.amaze.filemanager.GlideRequest
import com.amaze.filemanager.adapters.data.IconDataParcelable
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy

class RecyclerPreloadModelProvider(
    fragment: Fragment,
    private val urisToLoad: List<IconDataParcelable>,
    isCircled: Boolean
) : PreloadModelProvider<IconDataParcelable> {
    private val request: GlideRequest<Drawable>
    init {
        val incompleteRequest = GlideApp.with(fragment).asDrawable()
        request = if (isCircled) {
            incompleteRequest.circleCrop()
        } else {
            incompleteRequest.centerCrop()
        }
    }

    override fun getPreloadItems(position: Int): List<IconDataParcelable> {
        val iconData = if (position < urisToLoad.size) urisToLoad[position] else null
        iconData ?: return emptyList()
        return listOf(iconData)
    }

    override fun getPreloadRequestBuilder(iconData: IconDataParcelable): RequestBuilder<Drawable> {
        val requestBuilder =
            when (iconData.type) {
                IconDataParcelable.IMAGE_FROMFILE -> {
                    request.load(iconData.path)
                }
                IconDataParcelable.IMAGE_FROMCLOUD -> {
                    request.load(iconData.path).diskCacheStrategy(DiskCacheStrategy.NONE)
                }
                else -> {
                    request.load(iconData.image)
                }
            }
        return requestBuilder
    }
}
