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

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.amaze.filemanager.GlideRequest
import com.bumptech.glide.RequestBuilder
import com.amaze.filemanager.GlideApp
import com.amaze.filemanager.R

class AppsAdapterPreloadModel(
    f: Fragment,
    private val isBottomSheet: Boolean
) : PreloadModelProvider<String> {
    private val context: Context = f.requireContext()
    private val request: GlideRequest<Drawable> = GlideApp.with(f).asDrawable().fitCenter()
    private var items: List<String>? = null

    fun setItemList(items: List<String>?) {
        this.items = items
    }

    override fun getPreloadItems(position: Int): List<String> {
        val items = items ?: return emptyList()

        return listOf(items[position])
    }

    override fun getPreloadRequestBuilder(item: String): RequestBuilder<*> {
        return if (isBottomSheet) {
            request.clone().load(getApplicationIconFromPackageName(item))
        } else {
            request.clone().load(item)
        }
    }

    fun loadApkImage(item: String, v: ImageView) {
        if (isBottomSheet) {
            request.load(getApplicationIconFromPackageName(item)).into(v)
        } else {
            request.load(item).into(v)
        }
    }

    private fun getApplicationIconFromPackageName(packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(javaClass.simpleName, e)
            ContextCompat.getDrawable(context, R.drawable.ic_broken_image_white_24dp)
        }
    }

}