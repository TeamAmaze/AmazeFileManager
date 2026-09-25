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

package com.amaze.filemanager.adapters.glide.apkimage

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.amaze.filemanager.R
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 10/12/2017, at 16:12.
 */
class ApkImageDataFetcher(private val context: Context, private val model: String) :
    DataFetcher<Drawable> {
    override fun loadData(
        priority: Priority,
        callback: DataFetcher.DataCallback<in Drawable>,
    ) {
        val pi =
            context.packageManager.getPackageArchiveInfo(
                model,
                0,
            )
        val apkIcon: Drawable?
        if (pi != null) {
            pi.applicationInfo.sourceDir = model
            pi.applicationInfo.publicSourceDir = model
            apkIcon = pi.applicationInfo.loadIcon(context.packageManager)
        } else {
            apkIcon = ContextCompat.getDrawable(context, R.drawable.ic_android_white_24dp)
        }
        callback.onDataReady(apkIcon)
    }

    override fun cleanup() {
        // Intentionally empty only because we're not opening an InputStream or another I/O resource!
    }

    override fun cancel() {
        // No cancelation procedure
    }

    override fun getDataClass(): Class<Drawable> = Drawable::class.java

    override fun getDataSource(): DataSource = DataSource.LOCAL
}
