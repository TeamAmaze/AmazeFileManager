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

package com.amaze.filemanager.adapters.glide.cloudicon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.amaze.filemanager.filesystem.cloud.CloudUtil
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import java.io.IOException
import java.io.InputStream

class CloudIconDataFetcher(
    private val context: Context,
    private val path: String,
    private val width: Int,
    private val height: Int
) : DataFetcher<Bitmap> {

    companion object {
        private val TAG = CloudIconDataFetcher::class.java.simpleName
    }

    private var inputStream: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap?>) {
        inputStream = CloudUtil.getThumbnailInputStreamForCloud(context, path)
        val options = BitmapFactory.Options().also {
            it.outWidth = width
            it.outHeight = height
        }
        val drawable = BitmapFactory.decodeStream(inputStream, null, options)
        callback.onDataReady(drawable)
    }

    override fun cleanup() {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error cleaning up cloud icon fetch", e)
        }
    }

    override fun cancel() = Unit

    override fun getDataClass(): Class<Bitmap> = Bitmap::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE
}
